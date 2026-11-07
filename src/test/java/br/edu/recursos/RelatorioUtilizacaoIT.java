package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/**
 * Relatório de utilização (RF12, Aula 11): {@code GET /api/relatorios/utilizacao?inicio=&fim=}.
 *
 * <p>Testes de API em caixa-preta com dados pequenos semeados no próprio teste. Os valores
 * esperados foram calculados à mão e a conta está escrita em comentário junto de cada asserção:
 * eles <b>não</b> foram copiados da saída da aplicação.
 *
 * <p>Regras do relatório:
 *
 * <ul>
 *   <li>Contam as reservas APROVADA, EM_USO e CONCLUIDA. Não contam SOLICITADA (ainda não
 *       aprovada), CANCELADA, REJEITADA e NAO_COMPARECEU.
 *   <li>Só conta a parte da reserva que fica dentro do período [inicio, fim).
 *   <li>"recursos" traz salas e materiais; "professores" traz a carga horária de cada docente.
 *   <li>"conflitosEvitados" conta as tentativas de reserva recusadas por sobreposição cujo
 *       instante da tentativa está dentro do período.
 * </ul>
 */
@SpringBootTest(properties = "app.admin-password=" + RelatorioUtilizacaoIT.SENHA_ADMIN)
@AutoConfigureMockMvc
@Testcontainers
class RelatorioUtilizacaoIT {
  static final String SENHA_ADMIN = "relatorio-admin-senha-123";
  static final String SENHA = "senha-relatorio-123";
  static final String URL = "/api/relatorios/utilizacao";

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

  /** Meia-noite (UTC) de um dia daqui a 10 dias: o "dia D" dos dados. */
  Instant diaD;

  long sala101, sala102, profAna, projetor;

  Instant h(int hora, int minuto) {
    return diaD.plus(hora, ChronoUnit.HOURS).plus(minuto, ChronoUnit.MINUTES);
  }

  @BeforeEach
  void semear() {
    db.execute(
        "TRUNCATE material_movement,notification,allocation,booking,avoided_conflict,resource"
            + " RESTART IDENTITY CASCADE");
    for (String nome : List.of("ana", "gestor"))
      db.update(
          "insert into app_user(username,password,role) values (?,?,?) on conflict(username) do"
              + " nothing",
          nome,
          encoder.encode(SENHA),
          nome.equals("gestor") ? "RESPONSAVEL" : "SOLICITANTE");
    diaD = LocalDate.now(ZoneOffset.UTC).plusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant();

    sala101 = recurso("Sala 101", "SALA", false);
    sala102 = recurso("Sala 102", "SALA", false);
    profAna = recurso("Prof. Ana", "PROFESSOR", false);
    projetor = recurso("Projetor", "MATERIAL", true);
  }

  /**
   * Agenda do dia D usada pelo teste com oráculo. Período consultado: 08:00 até 18:00.
   *
   * <pre>
   *   A  09:00–11:00  Sala 101 + Prof. Ana  APROVADA
   *   B  14:00–15:30  Sala 101              CONCLUIDA
   *   C  16:00–17:00  Sala 101              CANCELADA      (não conta)
   *   D  17:00–20:00  Sala 101              APROVADA       (atravessa o fim do período)
   *   E  07:00–08:30  Sala 101              APROVADA       (atravessa o início do período)
   *   F  12:00–13:00  Sala 102 + Prof. Ana  EM_USO
   *   G  10:00–12:00  Projetor (restrito)   SOLICITADA     (não conta)
   *   H  dia D+1, 09:00–10:00  Prof. Ana    APROVADA       (fora do período)
   *
   *   Conflitos evitados registrados: 10:00 e 12:00 do dia D (dentro),
   *   10:00 do dia D-1 (fora) e 18:00 do dia D (fora: o fim é exclusivo).
   * </pre>
   */
  void semearAgendaDoDiaD() {
    reservar("A", h(9, 0), h(11, 0), sala101, profAna);
    long b = reservar("B", h(14, 0), h(15, 30), sala101);
    db.update("update booking set state='CONCLUIDA' where id=?", b);
    long c = reservar("C", h(16, 0), h(17, 0), sala101);
    service.transition(c, "CANCELADA", "ana", false);
    reservar("D", h(17, 0), h(20, 0), sala101);
    reservar("E", h(7, 0), h(8, 30), sala101);
    long f = reservar("F", h(12, 0), h(13, 0), sala102, profAna);
    db.update("update booking set state='EM_USO' where id=?", f);
    long g = reservar("G", h(10, 0), h(12, 0), projetor);
    assertEquals("SOLICITADA", service.get(g).get("state"));
    reservar("H", h(24 + 9, 0), h(24 + 10, 0), profAna);

    conflitoEm(h(10, 0));
    conflitoEm(h(12, 0));
    conflitoEm(h(-24 + 10, 0));
    conflitoEm(h(18, 0));
  }

  // ---------------------------------------------------------------- testes

  @Test
  @DisplayName("Oráculo calculado à mão: horas por recurso, por professor e conflitos no período")
  void relatorioDoDiaComOraculo() throws Exception {
    semearAgendaDoDiaD();
    JsonNode r = relatorio(login("gestor", SENHA), h(8, 0), h(18, 0));

    // Sala 101 = A (09:00–11:00) 2,0 h
    //          + B (14:00–15:30) 1,5 h
    //          + C cancelada     0,0 h
    //          + D só 17:00–18:00 dentro do período: 1,0 h
    //          + E só 08:00–08:30 dentro do período: 0,5 h
    //          = 2,0 + 1,5 + 0 + 1,0 + 0,5 = 5,0 h
    assertEquals(5.0, horas(r, "recursos", "Sala 101"), 0.001);
    // Sala 102 = F (12:00–13:00, em uso) = 1,0 h
    assertEquals(1.0, horas(r, "recursos", "Sala 102"), 0.001);
    // Projetor = G ainda SOLICITADA, não aprovada = 0,0 h
    assertEquals(0.0, horas(r, "recursos", "Projetor"), 0.001);
    // Prof. Ana = A (2,0 h) + F (1,0 h) + H fora do período (0 h) = 3,0 h
    assertEquals(3.0, horas(r, "professores", "Prof. Ana"), 0.001);
    // Professores não aparecem na lista de salas e materiais
    assertNull(linha(r, "recursos", "Prof. Ana"));
    // Conflitos: 10:00 e 12:00 do dia D estão dentro; D-1 e 18:00 (fim exclusivo) ficam fora = 2
    assertEquals(2, r.get("conflitosEvitados").asInt());
  }

  @Test
  void periodoVazioRetornaZeros() throws Exception {
    semearAgendaDoDiaD();
    // Dia D+5, 08:00–18:00: nenhuma reserva e nenhum conflito nesse período.
    JsonNode r = relatorio(login("gestor", SENHA), h(5 * 24 + 8, 0), h(5 * 24 + 18, 0));

    for (String nome : List.of("Sala 101", "Sala 102", "Projetor"))
      assertEquals(0.0, horas(r, "recursos", nome), 0.001, nome);
    assertEquals(0.0, horas(r, "professores", "Prof. Ana"), 0.001);
    assertEquals(0, r.get("conflitosEvitados").asInt());
  }

  @Test
  void reservaCanceladaNaoContaComoUso() throws Exception {
    long c = reservar("Cancelada", h(9, 0), h(12, 0), sala101, profAna);
    service.transition(c, "CANCELADA", "ana", false);

    JsonNode r = relatorio(login("gestor", SENHA), h(8, 0), h(18, 0));
    // A única reserva (3 h) foi cancelada: 0 h para a sala e para o professor.
    assertEquals(0.0, horas(r, "recursos", "Sala 101"), 0.001);
    assertEquals(0.0, horas(r, "professores", "Prof. Ana"), 0.001);
  }

  @Test
  void conflitoRecusadoPelaApiEntraNoPeriodoDaTentativa() throws Exception {
    reservar("Original", h(9, 0), h(10, 0), sala101);
    var ana = login("ana", SENHA);
    mvc.perform(
            post("/api/bookings")
                .with(csrf())
                .session(ana)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "title", "Sobreposta",
                            "start", h(9, 30).toString(),
                            "end", h(10, 30).toString(),
                            "resources", List.of(sala101)))))
        .andExpect(status().isConflict());

    // A tentativa aconteceu agora: um período de 1 h antes até 1 h depois deve contar 1 conflito.
    Instant agora = Instant.now();
    JsonNode r =
        relatorio(
            login("gestor", SENHA),
            agora.minus(1, ChronoUnit.HOURS),
            agora.plus(1, ChronoUnit.HOURS));
    assertEquals(1, r.get("conflitosEvitados").asInt());
  }

  @Test
  void acessoSomenteResponsavelEAdministrador() throws Exception {
    var inicio = h(8, 0).toString();
    var fim = h(18, 0).toString();
    mvc.perform(get(URL).param("inicio", inicio).param("fim", fim))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(URL).param("inicio", inicio).param("fim", fim).session(login("ana", SENHA)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(URL).param("inicio", inicio).param("fim", fim).session(login("gestor", SENHA)))
        .andExpect(status().isOk());
    mvc.perform(
            get(URL).param("inicio", inicio).param("fim", fim).session(login("admin", SENHA_ADMIN)))
        .andExpect(status().isOk());
  }

  @Test
  void periodoInvalidoRetorna400() throws Exception {
    var gestor = login("gestor", SENHA);
    mvc.perform(
            get(URL)
                .param("inicio", h(18, 0).toString())
                .param("fim", h(8, 0).toString())
                .session(gestor))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("O término deve ser posterior ao início."));
    mvc.perform(get(URL).param("inicio", h(8, 0).toString()).session(gestor))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  // ---------------------------------------------------------------- apoio

  long recurso(String nome, String tipo, boolean restrito) {
    return db.queryForObject(
        "insert into resource(name,type,capacity,location,restricted) values (?,?,30,'Bloco R',?)"
            + " returning id",
        Long.class,
        nome,
        tipo,
        restrito);
  }

  long reservar(String titulo, Instant inicio, Instant fim, Long... recursos) {
    return service.save(null, "ana", titulo, inicio, fim, List.of(recursos));
  }

  void conflitoEm(Instant quando) {
    db.update(
        "insert into avoided_conflict(actor,created_at) values ('ana',?)", Timestamp.from(quando));
  }

  MockHttpSession login(String usuario, String senha) throws Exception {
    var resultado =
        mvc.perform(formLogin("/api/login").user(usuario).password(senha))
            .andExpect(status().isNoContent())
            .andReturn();
    return (MockHttpSession) resultado.getRequest().getSession(false);
  }

  JsonNode relatorio(MockHttpSession sessao, Instant inicio, Instant fim) throws Exception {
    String corpo =
        mvc.perform(
                get(URL)
                    .param("inicio", inicio.toString())
                    .param("fim", fim.toString())
                    .session(sessao))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(corpo);
  }

  static JsonNode linha(JsonNode relatorio, String lista, String nome) {
    for (JsonNode item : relatorio.get(lista)) if (nome.equals(item.get("name").asText())) return item;
    return null;
  }

  static double horas(JsonNode relatorio, String lista, String nome) {
    JsonNode item = linha(relatorio, lista, nome);
    assertNotNull(item, nome + " ausente em '" + lista + "'");
    return item.get("horas").asDouble();
  }
}
