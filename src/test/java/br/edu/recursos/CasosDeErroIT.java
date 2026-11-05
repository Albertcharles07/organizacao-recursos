package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/**
 * Caminhos de erro e transições que ainda não tinham teste: login inválido, logout, contas e
 * usuários do administrador, rejeição e solicitação expirada.
 */
@SpringBootTest(properties = "app.admin-password=" + CasosDeErroIT.SENHA_ADMIN)
@AutoConfigureMockMvc
@Testcontainers
class CasosDeErroIT {
  static final String SENHA_ADMIN = "erros-admin-senha-123";
  static final String SENHA = "senha-erros-123";

  @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired BookingService service;
  @Autowired PasswordEncoder encoder;
  long sala, material;
  Instant inicio, fim;

  @BeforeEach
  void preparar() {
    db.execute(
        "TRUNCATE material_movement,notification,allocation,booking,avoided_conflict,resource"
            + " RESTART IDENTITY CASCADE");
    for (String nome : List.of("ana", "bia", "gestor"))
      db.update(
          "insert into app_user(username,password,role) values (?,?,?) on conflict(username) do"
              + " nothing",
          nome,
          encoder.encode(SENHA),
          nome.equals("gestor") ? "RESPONSAVEL" : "SOLICITANTE");
    db.update("update app_user set enabled=true");
    sala =
        db.queryForObject(
            "insert into resource(name,type,capacity,location) values ('Sala E','SALA',30,'Bloco"
                + " E') returning id",
            Long.class);
    material =
        db.queryForObject(
            "insert into resource(name,type,capacity,location,restricted) values"
                + " ('Projetor E','MATERIAL',1,'Bloco E',true) returning id",
            Long.class);
    inicio = Instant.now().plusSeconds(86400);
    fim = inicio.plusSeconds(3600);
  }

  long usuario(String nome) {
    return db.queryForObject("select id from app_user where username=?", Long.class, nome);
  }

  // ---------------------------------------------------------------- autenticação

  @Test
  void loginComSenhaErradaRetorna401() throws Exception {
    mvc.perform(formLogin("/api/login").user("ana").password("senha-errada"))
        .andExpect(status().isUnauthorized());
    mvc.perform(formLogin("/api/login").user("nao-existe").password(SENHA))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void contaDesativadaNaoConsegueEntrar() throws Exception {
    db.update("update app_user set enabled=false where username='bia'");
    mvc.perform(formLogin("/api/login").user("bia").password(SENHA))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutEncerraASessao() throws Exception {
    var sessao =
        (MockHttpSession)
            mvc.perform(formLogin("/api/login").user("ana").password(SENHA))
                .andExpect(status().isNoContent())
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(get("/api/me").session(sessao)).andExpect(status().isOk());
    mvc.perform(post("/api/logout").session(sessao).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/me").session(sessao)).andExpect(status().isUnauthorized());
  }

  // ---------------------------------------------------------------- administração de usuários

  @Test
  void administradorNaoDesativaAPropriaConta() throws Exception {
    mvc.perform(
            put("/api/users/" + usuario("admin") + "/enabled")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"enabled\":false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Não desative sua própria conta."));
    assertTrue(
        db.queryForObject("select enabled from app_user where username='admin'", Boolean.class));
  }

  @Test
  void ativarUsuarioInexistenteOuSemCampoRetorna400() throws Exception {
    var admin = user("admin").roles("ADMIN");
    mvc.perform(
            put("/api/users/99999/enabled")
                .with(admin)
                .with(csrf())
                .contentType("application/json")
                .content("{\"enabled\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Usuário não encontrado."));
    mvc.perform(
            put("/api/users/" + usuario("bia") + "/enabled")
                .with(admin)
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Informe enabled."));
  }

  @Test
  void usuarioDuplicadoRetorna409() throws Exception {
    mvc.perform(
            post("/api/users")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"username\":\"ana\",\"password\":\"outra-senha-123\","
                        + "\"role\":\"SOLICITANTE\"}"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("Registro duplicado ou operação incompatível com os dados existentes."));
    assertEquals(
        1,
        db.queryForObject("select count(*) from app_user where username='ana'", Integer.class));
  }

  @Test
  void senhaCurtaNoCadastroRetorna400() throws Exception {
    mvc.perform(
            post("/api/users")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"curta\",\"password\":\"123\",\"role\":\"SOLICITANTE\"}"))
        .andExpect(status().isBadRequest());
    assertEquals(
        0,
        db.queryForObject("select count(*) from app_user where username='curta'", Integer.class));
  }

  // ---------------------------------------------------------------- transições

  @Test
  void rejeicaoLiberaORecursoParaOutraReserva() {
    long pedido = service.save(null, "ana", "Projetor", inicio, fim, List.of(material));
    service.transition(pedido, "REJEITADA", "gestor", true);
    assertEquals("REJEITADA", service.get(pedido).get("state"));
    assertDoesNotThrow(() -> service.save(null, "bia", "Projetor", inicio, fim, List.of(material)));
  }

  @Test
  void solicitacaoExpiradaNaoPodeSerAprovada() {
    long pedido = service.save(null, "ana", "Projetor", inicio, fim, List.of(material));
    db.update(
        "update booking set start_at=now()-interval '1 hour',end_at=now()+interval '1 hour'"
            + " where id=?",
        pedido);
    var erro =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.transition(pedido, "APROVADA", "gestor", true));
    assertEquals("A solicitação expirou.", erro.getMessage());
    assertEquals("SOLICITADA", service.get(pedido).get("state"));
  }

  @Test
  void naoComparecimentoLiberaOHorario() {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(sala));
    db.update(
        "update booking set start_at=now()-interval '10 minutes',end_at=now()+interval '1 hour'"
            + " where id=?",
        id);
    service.transition(id, "NAO_COMPARECEU", "gestor", true);
    assertEquals(
        0,
        db.queryForObject(
            "select count(*) from allocation where booking_id=? and active", Integer.class, id));
  }

  @Test
  void estadoDesconhecidoRetorna400() throws Exception {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(sala));
    mvc.perform(
            post("/api/bookings/" + id + "/state")
                .with(user("gestor").roles("RESPONSAVEL"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"state\":\"QUALQUER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Transição de estado inválida."));
  }
}
