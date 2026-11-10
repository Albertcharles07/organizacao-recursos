package br.edu.recursos;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/**
 * Entrega da notificação para a API externa (RF11), com PostgreSQL real e a API externa simulada
 * pelo WireMock.
 *
 * <p>A entrega acontece depois do commit da reserva, em transação própria: o resultado fica na
 * coluna {@code delivery} da notificação (ENVIADA ou FALHOU), e uma falha externa não desfaz a
 * reserva.
 */
@SpringBootTest(properties = "app.admin-password=notificacao-admin-senha-123")
@Testcontainers
class NotificacaoEntregaIT {
  @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");
  static final WireMockServer api = new WireMockServer(options().dynamicPort());

  static {
    api.start();
  }

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("app.notification-url", () -> api.baseUrl() + "/notificacoes");
  }

  @AfterAll
  static void parar() {
    api.stop();
  }

  @Autowired JdbcTemplate db;
  @Autowired BookingService service;
  @Autowired PasswordEncoder encoder;
  long sala;

  @BeforeEach
  void preparar() {
    api.resetAll();
    db.execute(
        "TRUNCATE material_movement,notification,allocation,booking,avoided_conflict,resource"
            + " RESTART IDENTITY CASCADE");
    db.update(
        "insert into app_user(username,password,role) values ('ana',?,'SOLICITANTE') on"
            + " conflict(username) do nothing",
        encoder.encode("senha-notificacao-123"));
    sala =
        db.queryForObject(
            "insert into resource(name,type,capacity,location) values ('Sala N','SALA',30,'Bloco"
                + " N') returning id",
            Long.class);
  }

  long reservar() {
    Instant inicio = Instant.now().plusSeconds(86400);
    return service.save(null, "ana", "Aula", inicio, inicio.plusSeconds(3600), List.of(sala));
  }

  String entrega(long reserva) {
    return db.queryForObject(
        "select delivery from notification where booking_id=? order by id desc limit 1",
        String.class,
        reserva);
  }

  @Test
  void apiExternaRespondendo2xxMarcaNotificacaoComoEnviada() {
    api.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));
    long id = reservar();

    assertEquals("ENVIADA", entrega(id));
    api.verify(
        1,
        postRequestedFor(urlEqualTo("/notificacoes"))
            .withRequestBody(equalToJson("{\"bookingId\":" + id + "}")));
  }

  @Test
  void falhaDaApiExternaMarcaFalhouSemDesfazerAReserva() {
    api.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(503)));
    long id = reservar();

    assertEquals("FALHOU", entrega(id));
    assertEquals("APROVADA", service.get(id).get("state"), "a reserva continua gravada");
  }

  @Test
  void apiExternaForaDoArMarcaFalhou() {
    api.stubFor(
        post("/notificacoes")
            .willReturn(
                aResponse()
                    .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));
    long id = reservar();

    assertEquals("FALHOU", entrega(id));
  }

  @Test
  void cadaMudancaDeEstadoGeraNovaEntrega() {
    api.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(204)));
    long id = reservar();
    service.transition(id, "CANCELADA", "ana", false);

    assertEquals(
        List.of("ENVIADA", "ENVIADA"),
        db.queryForList(
            "select delivery from notification where booking_id=? order by id",
            String.class,
            id));
    api.verify(2, postRequestedFor(urlEqualTo("/notificacoes")));
  }
}
