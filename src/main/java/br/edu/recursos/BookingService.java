package br.edu.recursos;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
  private final JdbcTemplate db;
  private final org.springframework.context.ApplicationEventPublisher events;
  private final Clock clock;

  static final String MOTIVO_PADRAO = "Não informado";

  public BookingService(
      JdbcTemplate db, org.springframework.context.ApplicationEventPublisher events, Clock clock) {
    this.db = db;
    this.events = events;
    this.clock = clock;
  }

  private Instant now() {
    return Instant.now(clock);
  }

  /** Registra uma mudança de estado na mesma transação de quem chamou (RF10). */
  private void history(long booking, String actor, String from, String to, String reason) {
    db.update(
        "insert into booking_history(booking_id,actor,at,previous_state,new_state,reason) values"
            + " (?,?,?,?,?,?)",
        booking,
        actor,
        ts(now()),
        from,
        to,
        reason == null || reason.isBlank() ? MOTIVO_PADRAO : reason.strip());
  }

  public static Timestamp ts(Instant t) {
    return Timestamp.from(t);
  }

  public void audit(String actor, String action, Long id, String detail) {
    db.update(
        "insert into audit(actor,action,entity_id,detail) values (?,?,?,?)",
        actor,
        action,
        id,
        detail);
  }

  public Map<String, Object> get(long id) {
    return db.queryForList("select * from booking where id=?", id).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
  }

  private List<Long> lockResources(List<Long> ids) {
    var sorted = ids.stream().distinct().sorted().toList();
    if (sorted.isEmpty()) throw new IllegalArgumentException("Selecione ao menos um recurso.");
    for (Long id : sorted) {
      var rows = db.queryForList("select * from resource where id=? for update", id);
      if (rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get("active")))
        throw new IllegalArgumentException("Recurso inexistente ou desativado.");
    }
    return sorted;
  }

  @Transactional
  public long save(
      Long id, String actor, String title, Instant start, Instant end, List<Long> resources) {
    Rules.interval(start, end);
    if (!start.isAfter(now()))
      throw new IllegalArgumentException("A reserva deve começar no futuro.");
    boolean creating = id == null;
    String previous = null;
    if (id != null) {
      db.queryForList("select id from booking where id=? for update", id);
      var old = get(id);
      previous = (String) old.get("state");
      if (!actor.equals(old.get("owner")))
        throw new AccessDeniedException("Somente o solicitante pode alterar sua reserva.");
      if (!Set.of("SOLICITADA", "APROVADA").contains(old.get("state"))
          || !((Timestamp) old.get("start_at")).toInstant().isAfter(now()))
        throw new IllegalArgumentException("Reserva iniciada ou encerrada não pode ser alterada.");
    }
    var ids = lockResources(resources);
    boolean restricted = false;
    for (long resource : ids) {
      restricted |=
          Boolean.TRUE.equals(
              db.queryForObject(
                  "select restricted from resource where id=?", Boolean.class, resource));
    }
    String state = restricted ? "SOLICITADA" : "APROVADA";
    if (id == null)
      id =
          db.queryForObject(
              "insert into booking(owner,title,start_at,end_at,state) values (?,?,?,?,?) returning"
                  + " id",
              Long.class,
              actor,
              title,
              ts(start),
              ts(end),
              state);
    else {
      db.update("update allocation set active=false,current_version=false where booking_id=?", id);
      db.update(
          "update booking set title=?,start_at=?,end_at=?,state=? where id=?",
          title,
          ts(start),
          ts(end),
          state,
          id);
    }
    for (long resource : ids)
      db.update(
          "insert into allocation(resource_id,booking_id,start_at,end_at) values (?,?,?,?)",
          resource,
          id,
          ts(start),
          ts(end));
    audit(
        actor,
        "RESERVA_SALVA",
        id,
        "Estado=" + state + "; início=" + start + "; fim=" + end + "; recursos=" + ids);
    history(id, actor, previous, state, creating ? "Reserva criada" : "Reserva alterada");
    notify(id, actor, "Reserva #" + id + ": " + state);
    return id;
  }

  @Transactional
  public void transition(long id, String target, String actor, boolean responsible) {
    transition(id, target, actor, responsible, null);
  }

  @Transactional
  public void transition(
      long id, String target, String actor, boolean responsible, String reason) {
    db.queryForList("select id from booking where id=? for update", id);
    var b = get(id);
    String from = (String) b.get("state");
    if (target.equals("CANCELADA")) {
      if (!actor.equals(b.get("owner")) && !responsible)
        throw new AccessDeniedException("Reserva de outro solicitante.");
      if (!((Timestamp) b.get("start_at")).toInstant().isAfter(now()))
        throw new IllegalArgumentException("Reserva iniciada não pode ser cancelada.");
    } else if (!responsible)
      throw new AccessDeniedException("Somente responsáveis alteram este estado.");
    if (!Rules.transition(from, target))
      throw new IllegalArgumentException("Transição de estado inválida.");
    Instant start = ((Timestamp) b.get("start_at")).toInstant(),
        end = ((Timestamp) b.get("end_at")).toInstant();
    if (target.equals("EM_USO") && (now().isBefore(start) || !now().isBefore(end)))
      throw new IllegalArgumentException("Uso permitido somente no intervalo reservado.");
    if (target.equals("NAO_COMPARECEU") && now().isBefore(start))
      throw new IllegalArgumentException("A reserva ainda não começou.");
    if (target.equals("APROVADA") && !start.isAfter(now()))
      throw new IllegalArgumentException("A solicitação expirou.");
    if (target.equals("CONCLUIDA")
        && db.queryForObject(
                "select count(*) from material_movement m where m.booking_id=? and"
                    + " m.kind='RETIRADA' and not exists(select 1 from material_movement d where"
                    + " d.booking_id=m.booking_id and d.resource_id=m.resource_id and"
                    + " d.kind='DEVOLUCAO')",
                Long.class,
                id)
            > 0)
      throw new IllegalArgumentException("Registre a devolução dos materiais antes de concluir.");
    db.update("update booking set state=? where id=?", target, id);
    if (Set.of("CANCELADA", "REJEITADA", "NAO_COMPARECEU").contains(target))
      db.update("update allocation set active=false where booking_id=?", id);
    audit(actor, "ESTADO", id, from + " → " + target);
    history(id, actor, from, target, reason);
    notify(id, (String) b.get("owner"), "Reserva #" + id + ": " + target);
  }

  private void notify(long id, String owner, String message) {
    long nid =
        db.queryForObject(
            "insert into notification(booking_id,recipient,message) values (?,?,?) returning id",
            Long.class,
            id,
            owner,
            message);
    events.publishEvent(new NotificationDelivery.Event(nid, id));
  }

  @Transactional
  public void maintenance(long resource, Instant start, Instant end, String reason, String actor) {
    Rules.interval(start, end);
    lockResources(List.of(resource));
    db.update(
        "insert into allocation(resource_id,start_at,end_at,reason) values (?,?,?,?)",
        resource,
        ts(start),
        ts(end),
        reason);
    audit(actor, "MANUTENCAO", resource, reason + "; " + start + " / " + end);
  }

  @Transactional
  public void movement(long id, long resource, String kind, String actor) {
    db.queryForList("select id from booking where id=? for update", id);
    var b = get(id);
    if (!b.get("state").equals("EM_USO"))
      throw new IllegalArgumentException("A reserva deve estar em uso.");
    if (db.queryForObject(
            "select count(*) from allocation a join resource r on r.id=a.resource_id where"
                + " a.booking_id=? and a.resource_id=? and a.active and r.type='MATERIAL'",
            Long.class,
            id,
            resource)
        == 0) throw new IllegalArgumentException("Material não pertence à reserva.");
    if (!Set.of("RETIRADA", "DEVOLUCAO").contains(kind))
      throw new IllegalArgumentException("Movimento inválido.");
    if (kind.equals("DEVOLUCAO")
        && db.queryForObject(
                "select count(*) from material_movement where booking_id=? and resource_id=? and"
                    + " kind='RETIRADA'",
                Long.class,
                id,
                resource)
            == 0) throw new IllegalArgumentException("Registre a retirada primeiro.");
    db.update(
        "insert into material_movement(booking_id,resource_id,kind,actor) values (?,?,?,?)",
        id,
        resource,
        kind,
        actor);
    audit(actor, kind, id, "Material #" + resource);
  }
}
