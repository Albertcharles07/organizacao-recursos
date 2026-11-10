package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/**
 * Histórico auditável de mudanças de estado (RF10, RC03, Aula 11), com PostgreSQL real.
 *
 * <p>Confirma que o registro é gravado na mesma transação da mudança de estado (rollback não
 * deixa registro órfão), que o histórico não pode ser alterado e que só Responsável e
 * Administrador consultam o histórico de uma reserva.
 */
@SpringBootTest(properties = "app.admin-password=historico-admin-senha-123")
@AutoConfigureMockMvc
@Testcontainers
class HistoricoEstadosIT {
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
  @Autowired ObjectMapper json;
  long sala, outraSala, material;
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
          encoder.encode("senha-historico-123"),
          nome.equals("gestor") ? "RESPONSAVEL" : "SOLICITANTE");
    sala = recurso("SALA", false);
    outraSala = recurso("SALA", false);
    material = recurso("MATERIAL", true);
    inicio = Instant.now().plusSeconds(86400);
    fim = inicio.plusSeconds(3600);
  }

  long recurso(String tipo, boolean restrito) {
    return db.queryForObject(
        "insert into resource(name,type,capacity,location,restricted) values (?,?,30,'Bloco H',?)"
            + " returning id",
        Long.class,
        tipo,
        tipo,
        restrito);
  }

  List<Map<String, Object>> historico(long reserva) {
    return db.queryForList(
        "select actor,at,previous_state,new_state,reason from booking_history where booking_id=?"
            + " order by id",
        reserva);
  }

  @Test
  @DisplayName("Reserva cancelada pela API tem histórico com usuário, instante, estados e motivo")
  void reservaCanceladaTemHistoricoCompleto() throws Exception {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(sala));
    Instant antes = Instant.now();
    mvc.perform(
            post("/api/bookings/" + id + "/state")
                .with(user("ana").roles("SOLICITANTE"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"state\":\"CANCELADA\",\"reason\":\"Aula remarcada\"}"))
        .andExpect(status().isOk());
    Instant depois = Instant.now();

    var linhas = historico(id);
    assertEquals(2, linhas.size());
    assertNull(linhas.get(0).get("previous_state"));
    assertEquals("APROVADA", linhas.get(0).get("new_state"));
    assertEquals("Reserva criada", linhas.get(0).get("reason"));

    var cancelamento = linhas.get(1);
    assertEquals("ana", cancelamento.get("actor"));
    assertEquals("APROVADA", cancelamento.get("previous_state"));
    assertEquals("CANCELADA", cancelamento.get("new_state"));
    assertEquals("Aula remarcada", cancelamento.get("reason"));
    Instant quando = ((Timestamp) cancelamento.get("at")).toInstant();
    assertFalse(quando.isBefore(antes.minusSeconds(1)) || quando.isAfter(depois.plusSeconds(1)));

    mvc.perform(get("/api/bookings/" + id + "/history").with(user("gestor").roles("RESPONSAVEL")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[1].previous_state").value("APROVADA"))
        .andExpect(jsonPath("$[1].new_state").value("CANCELADA"))
        .andExpect(jsonPath("$[1].reason").value("Aula remarcada"))
        .andExpect(jsonPath("$[1].actor").value("ana"));
  }

  @Test
  void cicloCompletoGeraUmRegistroPorMudanca() {
    long id = service.save(null, "ana", "Projetor", inicio, fim, List.of(material));
    service.transition(id, "APROVADA", "gestor", true, "Uso em aula prática");
    db.update(
        "update booking set start_at=now()-interval '1 minute',end_at=now()+interval '1 hour'"
            + " where id=?",
        id);
    service.transition(id, "EM_USO", "gestor", true, null);
    service.transition(id, "CONCLUIDA", "gestor", true, null);

    var transicoes =
        historico(id).stream()
            .map(l -> l.get("previous_state") + " → " + l.get("new_state"))
            .toList();
    assertEquals(
        List.of(
            "null → SOLICITADA",
            "SOLICITADA → APROVADA",
            "APROVADA → EM_USO",
            "EM_USO → CONCLUIDA"),
        transicoes);
  }

  @Test
  @DisplayName("Rollback: falha depois de gravar o histórico não deixa registro órfão")
  void falhaNaTransacaoNaoDeixaHistoricoOrfao() {
    long id = service.save(null, "ana", "Projetor", inicio, fim, List.of(material));
    // A tabela audit não é zerada entre os testes (só aceita inserção), então compara antes e depois.
    int auditoriaAntes = auditoriaDeEstado(id);
    // Simula uma falha no último passo da transição (a notificação), depois do histórico gravado.
    db.execute(
        "create or replace function teste_falha_notificacao() returns trigger language plpgsql as"
            + " $$ begin raise exception 'falha simulada'; end $$");
    db.execute(
        "create trigger teste_falha_notificacao before insert on notification for each row"
            + " execute function teste_falha_notificacao()");
    try {
      assertThrows(
          DataAccessException.class,
          () -> service.transition(id, "APROVADA", "gestor", true, "Aprovada"));
    } finally {
      db.execute("drop trigger teste_falha_notificacao on notification");
    }

    assertEquals("SOLICITADA", service.get(id).get("state"));
    assertEquals(1, historico(id).size(), "só o registro da criação pode existir");
    assertEquals(auditoriaAntes, auditoriaDeEstado(id), "auditoria da transição desfeita junto");
  }

  int auditoriaDeEstado(long reserva) {
    return db.queryForObject(
        "select count(*) from audit where entity_id=? and action='ESTADO'", Integer.class, reserva);
  }

  @Test
  void alteracaoRecusadaNaoGravaHistorico() {
    long primeira = service.save(null, "ana", "Primeira", inicio, fim, List.of(sala));
    service.save(null, "bia", "Segunda", fim, fim.plusSeconds(3600), List.of(sala));
    assertThrows(
        DataIntegrityViolationException.class,
        () -> service.save(primeira, "ana", "Conflito", fim, fim.plusSeconds(3600), List.of(sala)));
    assertEquals(1, historico(primeira).size());
  }

  @Test
  void historicoNaoPodeSerAlteradoNemApagado() throws Exception {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(sala));
    assertThrows(
        DataAccessException.class, () -> db.update("update booking_history set reason='editado'"));
    assertThrows(DataAccessException.class, () -> db.update("delete from booking_history"));

    // A API não oferece rota para alterar ou apagar o histórico, nem para excluir reservas.
    var admin = user("admin").roles("ADMIN");
    mvc.perform(put("/api/bookings/" + id + "/history").with(admin).with(csrf()))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(delete("/api/bookings/" + id + "/history").with(admin).with(csrf()))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(delete("/api/bookings/" + id).with(admin).with(csrf()))
        .andExpect(status().isMethodNotAllowed());
    assertEquals(1, historico(id).size());
  }

  @Test
  void reservaIniciadaNaoPodeSerCanceladaNemExcluida() throws Exception {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(outraSala));
    db.update(
        "update booking set start_at=now()-interval '1 minute',end_at=now()+interval '1 hour'"
            + " where id=?",
        id);
    mvc.perform(
            post("/api/bookings/" + id + "/state")
                .with(user("ana").roles("SOLICITANTE"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"state\":\"CANCELADA\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/bookings/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isMethodNotAllowed());
    assertEquals("APROVADA", service.get(id).get("state"));
    assertEquals(1, historico(id).size());
  }

  @Test
  void consultaDoHistoricoSomenteResponsavelEAdministrador() throws Exception {
    long id = service.save(null, "ana", "Aula", inicio, fim, List.of(sala));
    String url = "/api/bookings/" + id + "/history";
    mvc.perform(get(url)).andExpect(status().isUnauthorized());
    mvc.perform(get(url).with(user("ana").roles("SOLICITANTE"))).andExpect(status().isForbidden());
    mvc.perform(get(url).with(user("gestor").roles("RESPONSAVEL"))).andExpect(status().isOk());
    mvc.perform(get(url).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    mvc.perform(get("/api/bookings/99999/history").with(user("gestor").roles("RESPONSAVEL")))
        .andExpect(status().isBadRequest());
  }
}
