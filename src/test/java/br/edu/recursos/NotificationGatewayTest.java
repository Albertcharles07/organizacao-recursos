package br.edu.recursos;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;

class NotificationGatewayTest {
  WireMockServer server;
  NotificationGateway gateway = new NotificationGateway();

  @BeforeEach
  void start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop();
  }

  @Test
  void sendsOnlyBookingIdentifier() {
    server.stubFor(post("/notify").willReturn(aResponse().withStatus(204)));
    assertTrue(gateway.send(server.baseUrl() + "/notify", 42));
    server.verify(
        postRequestedFor(urlEqualTo("/notify")).withRequestBody(equalToJson("{\"bookingId\":42}")));
  }

  @Test
  void handlesRemoteFailure() {
    server.stubFor(post("/notify").willReturn(aResponse().withStatus(503)));
    assertFalse(gateway.send(server.baseUrl() + "/notify", 42));
  }

  @Test
  void doesNotFollowRedirects() {
    server.stubFor(
        post("/notify").willReturn(aResponse().withStatus(302).withHeader("Location", "/private")));
    assertFalse(gateway.send(server.baseUrl() + "/notify", 42));
    server.verify(0, getRequestedFor(urlEqualTo("/private")));
  }

  @Test
  void malformedUrlIsSafe() {
    assertFalse(gateway.send(":invalid", 42));
  }
}
