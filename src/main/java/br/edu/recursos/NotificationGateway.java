package br.edu.recursos;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationGateway {
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(3))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  public boolean send(String url, long bookingId) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(5))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{\"bookingId\":" + bookingId + "}"))
              .build();
      int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      return status >= 200 && status < 300;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (IOException | IllegalArgumentException e) {
      return false;
    }
  }
}
