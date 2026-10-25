package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.admin-password=blackbox-admin-123")
@Testcontainers
class BlackBoxIT {
  @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
  }

  @LocalServerPort int port;

  @Test
  void publicHttpLoginCsrfAndResourceFlow() throws Exception {
    var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    var client = HttpClient.newBuilder().cookieHandler(cookies).build();
    String base = "http://localhost:" + port;
    var parser = new ObjectMapper();
    var csrf =
        parser.readTree(
            client
                .send(
                    HttpRequest.newBuilder(URI.create(base + "/api/csrf")).GET().build(),
                    HttpResponse.BodyHandlers.ofString())
                .body());
    var login =
        HttpRequest.newBuilder(URI.create(base + "/api/login"))
            .header(csrf.get("headerName").asText(), csrf.get("token").asText())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=admin&password=blackbox-admin-123"))
            .build();
    assertEquals(204, client.send(login, HttpResponse.BodyHandlers.ofString()).statusCode());
    csrf =
        parser.readTree(
            client
                .send(
                    HttpRequest.newBuilder(URI.create(base + "/api/csrf")).GET().build(),
                    HttpResponse.BodyHandlers.ofString())
                .body());
    var create =
        HttpRequest.newBuilder(URI.create(base + "/api/resources"))
            .header(csrf.get("headerName").asText(), csrf.get("token").asText())
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    parser.writeValueAsString(
                        Map.of(
                            "name",
                            "Sala HTTP",
                            "type",
                            "SALA",
                            "capacity",
                            30,
                            "location",
                            "A",
                            "competence",
                            "",
                            "active",
                            true,
                            "restricted",
                            false))))
            .build();
    assertEquals(200, client.send(create, HttpResponse.BodyHandlers.ofString()).statusCode());
    var result =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/api/resources?type=SALA")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, result.statusCode());
    assertTrue(result.body().contains("Sala HTTP"));
    assertFalse(result.body().contains("password"));
  }
}
