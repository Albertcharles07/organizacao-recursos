package br.edu.recursos;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/**
 * Matriz de autorização executável (Aula 12).
 *
 * <p>As células vêm de {@code docs/seguranca/matriz-autorizacao.md}: cada linha da tabela vira quatro
 * casos (anônimo, Solicitante, Responsável, Administrador), executados com login real em
 * {@code POST /api/login}. O cenário de cada linha (dados e requisição) está em {@link #CENARIOS}.
 */
@SpringBootTest(properties = "app.admin-password=" + AutorizacaoMatrizIT.SENHA_ADMIN)
@AutoConfigureMockMvc
@Testcontainers
class AutorizacaoMatrizIT {
  static final String SENHA_ADMIN = "matriz-admin-senha-123";
  static final String SENHA = "senha-matriz-123";
  static final Path MATRIZ = Path.of("docs", "seguranca", "matriz-autorizacao.md");
  static final ObjectMapper JSON = new ObjectMapper();
  static final AtomicInteger SEQUENCIA = new AtomicInteger();

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

  enum Perfil {
    ANONIMO(null, null),
    SOLICITANTE("ana", SENHA),
    RESPONSAVEL("gestor", SENHA),
    ADMINISTRADOR("admin", SENHA_ADMIN);

    final String usuario;
    final String senha;

    Perfil(String usuario, String senha) {
      this.usuario = usuario;
      this.senha = senha;
    }
  }

  /** Dados criados antes de cada caso. Todas as reservas pertencem à {@code ana}. */
  static class Cenario {
    long sala, salaIniciada, material, materialEmUso, professor, livre;
    long reservaAprovada, solicitacaoRestrita, reservaIniciada, reservaEmUso;
    long manutencao, usuarioBia;
    Instant inicio, fim;
  }

  Cenario c;

  // ---------------------------------------------------------------- cenários de cada linha da matriz

  static final Map<String, Function<Cenario, MockHttpServletRequestBuilder>> CENARIOS =
      new LinkedHashMap<>();

  static {
    CENARIOS.put("M01", c -> get("/"));
    CENARIOS.put("M02", c -> get("/api/csrf"));
    CENARIOS.put("M03", c -> post("/api/logout"));
    CENARIOS.put("M04", c -> get("/api/me"));
    CENARIOS.put("M05", c -> get("/api/resources"));
    CENARIOS.put("M06", c -> get("/api/resources").param("includeInactive", "true"));
    CENARIOS.put("M07", c -> json(post("/api/resources"), recurso("Sala da matriz")));
    CENARIOS.put("M08", c -> json(put("/api/resources/{id}", c.livre), recurso("Sala editada")));
    CENARIOS.put("M09", c -> get("/api/users"));
    CENARIOS.put(
        "M10",
        c ->
            json(
                post("/api/users"),
                Map.of(
                    "username", "matriz" + SEQUENCIA.incrementAndGet(),
                    "password", "senha-nova-123",
                    "role", "SOLICITANTE")));
    CENARIOS.put(
        "M11",
        c -> json(put("/api/users/{id}/enabled", c.usuarioBia), Map.of("enabled", true)));
    CENARIOS.put("M12", c -> get("/api/bookings"));
    CENARIOS.put(
        "M13",
        c ->
            json(
                post("/api/bookings"),
                reserva("Nova", c.inicio.plus(20, ChronoUnit.HOURS), c.livre)));
    CENARIOS.put(
        "M14",
        c ->
            json(
                put("/api/bookings/{id}", c.reservaAprovada),
                Map.of(
                    "title", "Aula alterada",
                    "start", c.inicio.toString(),
                    "end", c.fim.toString(),
                    "resources", List.of(c.sala))));
    CENARIOS.put("M15", c -> estado(c.reservaAprovada, "CANCELADA"));
    CENARIOS.put("M16", c -> estado(c.solicitacaoRestrita, "APROVADA"));
    CENARIOS.put("M17", c -> estado(c.solicitacaoRestrita, "REJEITADA"));
    CENARIOS.put("M18", c -> estado(c.reservaIniciada, "EM_USO"));
    CENARIOS.put("M19", c -> estado(c.reservaIniciada, "NAO_COMPARECEU"));
    CENARIOS.put("M20", c -> estado(c.reservaEmUso, "CONCLUIDA"));
    CENARIOS.put(
        "M21",
        c ->
            json(
                post("/api/bookings/{id}/movements", c.reservaEmUso),
                Map.of("resource", c.materialEmUso, "kind", "RETIRADA")));
    CENARIOS.put("M22", c -> get("/api/movements"));
    CENARIOS.put("M23", c -> get("/api/maintenance"));
    CENARIOS.put(
        "M24",
        c ->
            json(
                post("/api/maintenance"),
                Map.of(
                    "resource", c.livre,
                    "start", c.inicio.plus(30, ChronoUnit.HOURS).toString(),
                    "end", c.inicio.plus(31, ChronoUnit.HOURS).toString(),
                    "reason", "Reparo")));
    CENARIOS.put("M25", c -> delete("/api/maintenance/{id}", c.manutencao));
    CENARIOS.put("M26", c -> get("/api/audit"));
    CENARIOS.put("M27", c -> get("/api/notifications"));
    CENARIOS.put("M28", c -> get("/api/reports"));
  }

  static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, Object body) {
    try {
      return b.contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static MockHttpServletRequestBuilder estado(long reserva, String alvo) {
    return json(post("/api/bookings/{id}/state", reserva), Map.of("state", alvo));
  }

  static Map<String, Object> recurso(String nome) {
    return Map.of(
        "name", nome,
        "type", "SALA",
        "capacity", 30,
        "location", "Bloco M",
        "competence", "",
        "restricted", false,
        "active", true);
  }

  static Map<String, Object> reserva(String titulo, Instant inicio, long recurso) {
    return Map.of(
        "title", titulo,
        "start", inicio.toString(),
        "end", inicio.plus(1, ChronoUnit.HOURS).toString(),
        "resources", List.of(recurso));
  }

  // ---------------------------------------------------------------- dados

  @BeforeEach
  void prepararCenario() {
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

    c = new Cenario();
    c.inicio = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    c.fim = c.inicio.plus(1, ChronoUnit.HOURS);
    c.sala = recurso("SALA", false);
    c.salaIniciada = recurso("SALA", false);
    c.material = recurso("MATERIAL", true);
    c.materialEmUso = recurso("MATERIAL", true);
    c.professor = recurso("PROFESSOR", false);
    c.livre = recurso("SALA", false);
    c.usuarioBia = db.queryForObject("select id from app_user where username='bia'", Long.class);

    // Reserva aprovada futura (sala comum não exige aprovação).
    c.reservaAprovada = service.save(null, "ana", "Aula", c.inicio, c.fim, List.of(c.sala));
    // Solicitação pendente de recurso restrito.
    c.solicitacaoRestrita =
        service.save(null, "ana", "Projetor", c.inicio, c.fim, List.of(c.material));
    // Reserva aprovada cujo horário já começou.
    c.reservaIniciada =
        service.save(
            null,
            "ana",
            "Iniciada",
            c.inicio.plus(2, ChronoUnit.HOURS),
            c.inicio.plus(3, ChronoUnit.HOURS),
            List.of(c.salaIniciada));
    iniciarAgora(c.reservaIniciada);
    // Reserva com material, já em uso.
    c.reservaEmUso =
        service.save(
            null,
            "ana",
            "Em uso",
            c.inicio.plus(4, ChronoUnit.HOURS),
            c.inicio.plus(5, ChronoUnit.HOURS),
            List.of(c.materialEmUso));
    iniciarAgora(c.reservaEmUso);
    db.update("update booking set state='EM_USO' where id=?", c.reservaEmUso);
    // Bloqueio de manutenção.
    service.maintenance(
        c.professor,
        c.inicio.plus(6, ChronoUnit.HOURS),
        c.inicio.plus(7, ChronoUnit.HOURS),
        "Reparo",
        "admin");
    c.manutencao =
        db.queryForObject("select id from allocation where booking_id is null", Long.class);
  }

  long recurso(String tipo, boolean restrito) {
    return db.queryForObject(
        "insert into resource(name,type,capacity,location,restricted) values (?,?,30,'Bloco M',?)"
            + " returning id",
        Long.class,
        tipo + " " + SEQUENCIA.incrementAndGet(),
        tipo,
        restrito);
  }

  void iniciarAgora(long reserva) {
    db.update(
        "update booking set start_at=now()-interval '1 minute',end_at=now()+interval '1 hour'"
            + " where id=?",
        reserva);
  }

  MockHttpSession login(Perfil perfil) throws Exception {
    if (perfil.usuario == null) return null;
    var resultado =
        mvc.perform(formLogin("/api/login").user(perfil.usuario).password(perfil.senha))
            .andExpect(status().isNoContent())
            .andReturn();
    return (MockHttpSession) resultado.getRequest().getSession(false);
  }

  int executar(MockHttpServletRequestBuilder requisicao, MockHttpSession sessao) throws Exception {
    requisicao.with(csrf());
    if (sessao != null) requisicao.session(sessao);
    return mvc.perform(requisicao).andReturn().getResponse().getStatus();
  }

  // ---------------------------------------------------------------- leitura da matriz

  record Linha(String id, String rota, Map<Perfil, Integer> esperado) {}

  static List<Linha> linhasDaMatriz() throws Exception {
    List<Linha> linhas = new ArrayList<>();
    for (String texto : Files.readAllLines(MATRIZ, StandardCharsets.UTF_8)) {
      if (!texto.startsWith("| M")) continue;
      String[] celula = texto.split("\\|");
      Map<Perfil, Integer> esperado = new EnumMap<>(Perfil.class);
      Perfil[] perfis = Perfil.values();
      for (int i = 0; i < perfis.length; i++)
        esperado.put(perfis[i], Integer.parseInt(celula[4 + i].trim()));
      linhas.add(new Linha(celula[1].trim(), celula[3].trim(), esperado));
    }
    return linhas;
  }

  static Stream<Arguments> celulas() throws Exception {
    return linhasDaMatriz().stream()
        .flatMap(
            l ->
                Arrays.stream(Perfil.values())
                    .map(p -> Arguments.of(l.id(), l.rota(), p, l.esperado().get(p))));
  }

  // ---------------------------------------------------------------- testes

  @Test
  void todaLinhaDaMatrizTemCenarioETodoCenarioTemLinha() throws Exception {
    var ids = linhasDaMatriz().stream().map(Linha::id).toList();
    assertEquals(28, ids.size(), "A matriz deve ter 28 operações");
    assertEquals(new TreeSet<>(CENARIOS.keySet()), new TreeSet<>(ids));
    assertEquals(ids.size(), new HashSet<>(ids).size(), "IDs repetidos na matriz");
  }

  @ParameterizedTest(name = "{0} {1} como {2} deve responder {3}")
  @MethodSource("celulas")
  void celulaDaMatriz(String id, String rota, Perfil perfil, int esperado) throws Exception {
    var cenario = CENARIOS.get(id);
    assertNotNull(cenario, "Linha " + id + " da matriz sem cenário no teste");
    int obtido = executar(cenario.apply(c), login(perfil));
    assertEquals(esperado, obtido, id + " " + rota + " como " + perfil);
  }

  @Test
  void solicitanteNaoMexeNaReservaDeOutro() throws Exception {
    var bia =
        mvc.perform(formLogin("/api/login").user("bia").password(SENHA))
            .andExpect(status().isNoContent())
            .andReturn();
    var sessao = (MockHttpSession) bia.getRequest().getSession(false);

    assertEquals(403, executar(CENARIOS.get("M14").apply(c), sessao));
    assertEquals(403, executar(estado(c.reservaAprovada, "CANCELADA"), sessao));
    mvc.perform(get("/api/bookings").session(sessao))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    var reserva = service.get(c.reservaAprovada);
    assertEquals("Aula", reserva.get("title"));
    assertEquals("APROVADA", reserva.get("state"));
  }

  @Test
  void entradasInvalidasRetornam400SemDetalhesInternos() throws Exception {
    var ana = login(Perfil.SOLICITANTE);
    var admin = login(Perfil.ADMINISTRADOR);
    String revise = "Revise os campos: valores obrigatórios, formatos, tamanhos e datas.";

    var terminoAntes =
        json(
            post("/api/bookings"),
            Map.of(
                "title", "Invertida",
                "start", c.inicio.plus(2, ChronoUnit.HOURS).toString(),
                "end", c.inicio.plus(1, ChronoUnit.HOURS).toString(),
                "resources", List.of(c.livre)));
    semDetalhesInternos(
        mvc.perform(terminoAntes.with(csrf()).session(ana))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("O término deve ser posterior ao início."))
            .andReturn()
            .getResponse()
            .getContentAsString());

    var capacidadeNegativa = new HashMap<>(recurso("Negativa"));
    capacidadeNegativa.put("capacity", -5);
    semDetalhesInternos(
        mvc.perform(json(post("/api/resources"), capacidadeNegativa).with(csrf()).session(admin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(revise))
            .andReturn()
            .getResponse()
            .getContentAsString());

    for (String corpo : List.of("{}", "{\"title\": ")) {
      semDetalhesInternos(
          mvc.perform(
                  post("/api/bookings")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(corpo)
                      .with(csrf())
                      .session(ana))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.message").value(revise))
              .andReturn()
              .getResponse()
              .getContentAsString());
    }

    semDetalhesInternos(
        mvc.perform(
                json(put("/api/resources/abc"), recurso("Id inválido")).with(csrf()).session(admin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(revise))
            .andReturn()
            .getResponse()
            .getContentAsString());

    var sobreposta = json(post("/api/bookings"), reserva("Sobreposta", c.inicio, c.sala));
    semDetalhesInternos(
        mvc.perform(sobreposta.with(csrf()).session(ana))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", startsWith("Horário indisponível")))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  static void semDetalhesInternos(String corpo) {
    for (String proibido :
        List.of(
            "Exception",
            "java.",
            "org.springframework",
            "org.postgresql",
            "br.edu.recursos",
            "SQL",
            "select ",
            "insert ",
            "\tat "))
      assertFalse(corpo.contains(proibido), "Resposta de erro expõe '" + proibido + "': " + corpo);
  }
}
