package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(properties = "app.admin-password=integration-admin-123")
@AutoConfigureMockMvc
@Testcontainers
class ResourceApiIT {
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
  long room, teacher, material;
  Instant start, end;

  @BeforeEach
  void setup() {
    db.execute(
        "TRUNCATE material_movement,notification,allocation,booking,avoided_conflict,resource"
            + " RESTART IDENTITY CASCADE");
    for (String name : List.of("ana", "bia", "gestor"))
      db.update(
          "insert into app_user(username,password,role) values (?,?,?) on conflict(username) do"
              + " nothing",
          name,
          encoder.encode("senha-teste-123"),
          name.equals("gestor") ? "RESPONSAVEL" : "SOLICITANTE");
    room = resource("SALA", false);
    teacher = resource("PROFESSOR", false);
    material = resource("MATERIAL", true);
    start = Instant.now().plusSeconds(86400);
    end = start.plusSeconds(3600);
  }

  long resource(String type, boolean restricted) {
    return db.queryForObject(
        "insert into resource(name,type,capacity,location,restricted) values (?,?,30,'Bloco A',?)"
            + " returning id",
        Long.class,
        type,
        type,
        restricted);
  }

  String payload(List<Long> ids) throws Exception {
    return json.writeValueAsString(
        Map.of("title", "Aula de Java", "start", start, "end", end, "resources", ids));
  }

  @Test
  void simultaneousRequestsAcceptOnlyOne() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    String body = payload(List.of(room));
    Callable<Integer> action =
        () -> {
          ready.countDown();
          assertTrue(go.await(10, TimeUnit.SECONDS));
          return mvc.perform(
                  post("/api/bookings")
                      .with(user("ana").roles("SOLICITANTE"))
                      .with(csrf())
                      .contentType("application/json")
                      .content(body))
              .andReturn()
              .getResponse()
              .getStatus();
        };
    try {
      var a = pool.submit(action);
      var b = pool.submit(action);
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      go.countDown();
      assertEquals(
          Set.of(200, 409), Set.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
      assertEquals(1, db.queryForObject("select count(*) from booking", Integer.class));
      assertEquals(1, db.queryForObject("select count(*) from avoided_conflict", Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void teacherCannotBeInTwoRooms() {
    long other = resource("SALA", false);
    service.save(null, "ana", "Aula", start, end, List.of(room, teacher));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> service.save(null, "bia", "Aula", start, end, List.of(other, teacher)));
    assertEquals(1, db.queryForObject("select count(*) from booking", Integer.class));
  }

  @Test
  void adjacentReservationsAreAccepted() {
    service.save(null, "ana", "Aula", start, end, List.of(room));
    assertDoesNotThrow(
        () -> service.save(null, "bia", "Aula 2", end, end.plusSeconds(3600), List.of(room)));
  }

  @Test
  void maintenanceAndReservationsShareExclusion() {
    service.maintenance(room, start, end, "Reparo", "admin");
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> service.save(null, "ana", "Aula", start, end, List.of(room)));
  }

  @Test
  void maintenanceCannotOverrideBooking() {
    service.save(null, "ana", "Aula", start, end, List.of(room));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> service.maintenance(room, start, end, "Reparo", "admin"));
  }

  @Test
  void restrictedApprovalAndAudit() {
    long id = service.save(null, "ana", "Projetor", start, end, List.of(material));
    assertEquals("SOLICITADA", service.get(id).get("state"));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.transition(id, "APROVADA", "ana", false));
    service.transition(id, "APROVADA", "gestor", true);
    assertEquals("APROVADA", service.get(id).get("state"));
    assertTrue(
        db.queryForObject(
                "select count(*) from audit where entity_id=? and action='ESTADO'",
                Integer.class,
                id)
            > 0);
    assertEquals(
        2,
        db.queryForObject(
            "select count(*) from notification where booking_id=?", Integer.class, id));
  }

  @Test
  void cancelReleasesResources() {
    long id = service.save(null, "ana", "Aula", start, end, List.of(room));
    service.transition(id, "CANCELADA", "ana", false);
    assertDoesNotThrow(() -> service.save(null, "bia", "Outra", start, end, List.of(room)));
  }

  @Test
  void ownerIsEnforced() {
    long id = service.save(null, "ana", "Aula", start, end, List.of(room));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.save(id, "bia", "Invasão", start, end, List.of(room)));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> service.transition(id, "CANCELADA", "bia", false));
  }

  @Test
  void failedEditRollsBack() {
    long first = service.save(null, "ana", "Primeira", start, end, List.of(room));
    service.save(null, "bia", "Segunda", end, end.plusSeconds(3600), List.of(room));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> service.save(first, "ana", "Inválida", end, end.plusSeconds(3600), List.of(room)));
    assertEquals("Primeira", service.get(first).get("title"));
    assertEquals(
        2, db.queryForObject("select count(*) from allocation where active", Integer.class));
  }

  @Test
  void inputsAndPermissions() throws Exception {
    mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/bookings")
                .with(user("ana"))
                .contentType("application/json")
                .content(payload(List.of(room))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/bookings")
                .with(user("ana"))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/users").with(user("ana").roles("SOLICITANTE")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/audit").with(user("ana").roles("SOLICITANTE")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/resources").with(user("ana")).param("start", start.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void availabilityFiltersAndPrivacy() throws Exception {
    service.save(null, "ana", "Privada", start, end, List.of(room));
    mvc.perform(
            get("/api/resources")
                .with(user("ana"))
                .param("type", "SALA")
                .param("start", start.toString())
                .param("end", end.toString()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(get("/api/bookings").with(user("bia").roles("SOLICITANTE")))
        .andExpect(content().json("[]"));
  }

  @Test
  void startedBookingAndMaterialLifecycle() {
    long id = service.save(null, "ana", "Uso", start, end, List.of(material));
    service.transition(id, "APROVADA", "gestor", true);
    db.update(
        "update booking set start_at=now()-interval '1 minute',end_at=now()+interval '1 hour' where"
            + " id=?",
        id);
    assertThrows(
        IllegalArgumentException.class, () -> service.transition(id, "CANCELADA", "ana", false));
    service.transition(id, "EM_USO", "gestor", true);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.movement(id, material, "DEVOLUCAO", "gestor"));
    service.movement(id, material, "RETIRADA", "gestor");
    assertThrows(
        IllegalArgumentException.class, () -> service.transition(id, "CONCLUIDA", "gestor", true));
    service.movement(id, material, "DEVOLUCAO", "gestor");
    service.transition(id, "CONCLUIDA", "gestor", true);
    assertEquals("CONCLUIDA", service.get(id).get("state"));
  }

  @Test
  void auditCannotBeRewritten() {
    service.audit("ana", "TESTE", null, "original");
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> db.update("update audit set detail='modificado'"));
  }

  @Test
  void adminResourceAndUserFlows() throws Exception {
    String body =
        json.writeValueAsString(
            Map.of(
                "name",
                "Laboratório",
                "type",
                "SALA",
                "capacity",
                40,
                "location",
                "Bloco B",
                "competence",
                "Java",
                "restricted",
                false,
                "active",
                true));
    mvc.perform(
            post("/api/resources")
                .with(user("ana").roles("SOLICITANTE"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    var created =
        mvc.perform(
                post("/api/resources")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn();
    long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            put("/api/resources/" + id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("Laboratório", "Laboratório 2")))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/users")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "username",
                            "nova",
                            "password",
                            "senha-nova-123",
                            "role",
                            "SOLICITANTE"))))
        .andExpect(status().isOk());
    long uid = db.queryForObject("select id from app_user where username='nova'", Long.class);
    mvc.perform(
            put("/api/users/" + uid + "/enabled")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"enabled\":false}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/me").with(user("nova").roles("SOLICITANTE")))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void maintenanceEndpointsAndReporting() throws Exception {
    String body =
        json.writeValueAsString(
            Map.of("resource", room, "start", start, "end", end, "reason", "Reparo"));
    mvc.perform(
            post("/api/maintenance")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
    mvc.perform(get("/api/maintenance").with(user("ana"))).andExpect(status().isOk());
    long id = db.queryForObject("select id from allocation where booking_id is null", Long.class);
    mvc.perform(delete("/api/maintenance/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/maintenance/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/reports").with(user("gestor").roles("RESPONSAVEL")))
        .andExpect(status().isOk());
    mvc.perform(get("/api/audit").with(user("gestor").roles("RESPONSAVEL")))
        .andExpect(status().isOk());
    mvc.perform(get("/api/notifications").with(user("ana"))).andExpect(status().isOk());
    mvc.perform(get("/api/movements").with(user("gestor").roles("RESPONSAVEL")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Dado recurso restrito, quando solicitante reserva, então responsável deve aprovar")
  void bddRestrictedResource() throws Exception {
    var created =
        mvc.perform(
                post("/api/bookings")
                    .with(user("ana").roles("SOLICITANTE"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload(List.of(material))))
            .andExpect(status().isOk())
            .andReturn();
    long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    assertEquals("SOLICITADA", service.get(id).get("state"));
    mvc.perform(
            post("/api/bookings/" + id + "/state")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"state\":\"APROVADA\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/bookings/" + id + "/state")
                .with(user("gestor").roles("RESPONSAVEL"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"state\":\"APROVADA\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/bookings/" + id)
                .with(user("ana").roles("SOLICITANTE"))
                .with(csrf())
                .contentType("application/json")
                .content(payload(List.of(material))))
        .andExpect(status().isOk());
    assertEquals("SOLICITADA", service.get(id).get("state"));
  }

  @Test
  void invalidRulesAndMissingRecords() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.save(
                null, "ana", "Passado", Instant.now().minusSeconds(100), end, List.of(room)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.save(null, "ana", "Vazia", start, end, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.save(null, "ana", "Ausente", start, end, List.of(99999L)));
    db.update("update resource set active=false where id=?", room);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.save(null, "ana", "Inativo", start, end, List.of(room)));
    assertThrows(IllegalArgumentException.class, () -> service.get(99999));
    long id = service.save(null, "ana", "Aula", start, end, List.of(teacher));
    assertThrows(
        IllegalArgumentException.class, () -> service.transition(id, "CONCLUIDA", "gestor", true));
    assertThrows(
        IllegalArgumentException.class, () -> service.transition(id, "EM_USO", "gestor", true));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.transition(id, "NAO_COMPARECEU", "gestor", true));
    assertThrows(
        IllegalArgumentException.class, () -> service.movement(id, material, "RETIRADA", "gestor"));
  }

  @Test
  @DisplayName("Regressão D04: recurso com alocação futura ativa não pode ser editado")
  void regressaoD04RecursoComReservaFuturaNaoPodeSerEditado() throws Exception {
    service.save(null, "ana", "Aula", start, end, List.of(room));
    String body =
        json.writeValueAsString(
            Map.of(
                "name", "SALA",
                "type", "SALA",
                "capacity", 30,
                "location", "Bloco A",
                "competence", "",
                "restricted", true,
                "active", true));
    mvc.perform(
            put("/api/resources/" + room)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Encerre ou cancele as alocações futuras antes de editar o recurso."));
    assertEquals(
        false, db.queryForObject("select restricted from resource where id=?", Boolean.class, room));
  }
}
