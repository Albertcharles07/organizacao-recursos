package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** Pesquisa de recursos por tipo, capacidade, localização e competência (RF03). */
@SpringBootTest(properties = "app.admin-password=filtros-admin-senha-123")
@AutoConfigureMockMvc
@Testcontainers
class FiltrosRecursosIT {
  @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;

  @BeforeEach
  void preparar() {
    db.execute(
        "TRUNCATE material_movement,notification,allocation,booking,avoided_conflict,resource"
            + " RESTART IDENTITY CASCADE");
    db.update(
        "insert into app_user(username,password,role) values ('ana','x','SOLICITANTE') on"
            + " conflict(username) do nothing");
    recurso("Sala 101", "SALA", 30, "Bloco A", "", true);
    recurso("Laboratório Java", "SALA", 40, "Bloco B", "Java", true);
    recurso("Prof. Carla", "PROFESSOR", 1, "Campus Centro", "Banco de Dados", true);
    recurso("Projetor", "MATERIAL", 1, "Bloco A", "HDMI", true);
    recurso("Sala Desativada", "SALA", 100, "Bloco B", "", false);
  }

  void recurso(String nome, String tipo, int capacidade, String local, String comp, boolean ativo) {
    db.update(
        "insert into resource(name,type,capacity,location,competence,active) values"
            + " (?,?,?,?,?,?)",
        nome,
        tipo,
        capacidade,
        local,
        comp,
        ativo);
  }

  List<String> nomes(MockHttpServletRequestBuilder busca) throws Exception {
    String corpo =
        mvc.perform(busca.with(user("ana").roles("SOLICITANTE")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> nomes = new ArrayList<>();
    json.readTree(corpo).forEach(r -> nomes.add(r.get("name").asText()));
    Collections.sort(nomes);
    return nomes;
  }

  @Test
  void semFiltroListaSoRecursosAtivos() throws Exception {
    assertEquals(
        List.of("Laboratório Java", "Prof. Carla", "Projetor", "Sala 101"),
        nomes(get("/api/resources")));
  }

  @Test
  void filtraPorTipo() throws Exception {
    assertEquals(List.of("Prof. Carla"), nomes(get("/api/resources").param("type", "PROFESSOR")));
  }

  @Test
  void filtraPorCapacidadeMinima() throws Exception {
    // Capacidade mínima 31: só o laboratório (40); a sala desativada (100) não aparece.
    assertEquals(
        List.of("Laboratório Java"), nomes(get("/api/resources").param("capacity", "31")));
    // Capacidade mínima 30 inclui o limite exato.
    assertEquals(
        List.of("Laboratório Java", "Sala 101"),
        nomes(get("/api/resources").param("capacity", "30")));
  }

  @Test
  void filtraPorLocalizacaoSemDiferenciarMaiusculas() throws Exception {
    assertEquals(
        List.of("Projetor", "Sala 101"), nomes(get("/api/resources").param("location", "bloco a")));
  }

  @Test
  void filtraPorCompetenciaPorTrecho() throws Exception {
    assertEquals(
        List.of("Prof. Carla"), nomes(get("/api/resources").param("competence", "banco")));
  }

  @Test
  void combinaFiltros() throws Exception {
    assertEquals(
        List.of("Laboratório Java"),
        nomes(
            get("/api/resources")
                .param("type", "SALA")
                .param("location", "Bloco B")
                .param("competence", "java")));
  }
}
