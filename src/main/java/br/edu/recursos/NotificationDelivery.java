package br.edu.recursos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationDelivery {
  public record Event(long notificationId, long bookingId) {}

  private final JdbcTemplate db;
  private final NotificationGateway gateway;
  private final String url;

  public NotificationDelivery(
      JdbcTemplate db, NotificationGateway gateway, @Value("${app.notification-url}") String url) {
    this.db = db;
    this.gateway = gateway;
    this.url = url;
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deliver(Event event) {
    if (!url.isBlank())
      db.update(
          "update notification set delivery=? where id=?",
          gateway.send(url, event.bookingId()) ? "ENVIADA" : "FALHOU",
          event.notificationId());
  }
}
