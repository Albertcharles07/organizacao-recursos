package br.edu.recursos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final JdbcTemplate db;
  private final BookingService service;
  private final PasswordEncoder encoder;

  public ApiController(JdbcTemplate db, BookingService service, PasswordEncoder encoder) {
    this.db = db;
    this.service = service;
    this.encoder = encoder;
  }

  public record Reservation(
      @NotBlank @Size(max = 160) String title,
      @NotNull Instant start,
      @NotNull Instant end,
      @NotEmpty @Size(max = 30) List<@NotNull @Positive Long> resources) {}

  public record Resource(
      @NotBlank @Size(max = 120) String name,
      @Pattern(regexp = "SALA|PROFESSOR|MATERIAL") @NotNull String type,
      @Min(1) int capacity,
      @NotBlank @Size(max = 120) String location,
      @NotNull @Size(max = 120) String competence,
      boolean restricted,
      boolean active) {}

  public record UserInput(
      @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") @NotNull String username,
      @Size(min = 12, max = 72) @NotNull String password,
      @Pattern(regexp = "SOLICITANTE|RESPONSAVEL|ADMIN") @NotNull String role) {}

  public record Maintenance(
      @Positive long resource,
      @NotNull Instant start,
      @NotNull Instant end,
      @NotBlank @Size(max = 160) String reason) {}

  public record State(@NotBlank String state, @Size(max = 250) String reason) {}

  public record Movement(
      @Positive long resource, @Pattern(regexp = "RETIRADA|DEVOLUCAO") @NotNull String kind) {}

  private boolean has(Authentication a, String role) {
    return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_" + role));
  }

  private void admin(Authentication a) {
    if (!has(a, "ADMIN")) throw new AccessDeniedException("Operação exclusiva do administrador.");
  }

  private void responsible(Authentication a) {
    if (!has(a, "RESPONSAVEL"))
      throw new AccessDeniedException("Operação exclusiva do responsável.");
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken t) {
    return Map.of("token", t.getToken(), "headerName", t.getHeaderName());
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication a) {
    return Map.of("username", a.getName(), "roles", a.getAuthorities());
  }

  @GetMapping("/resources")
  public List<Map<String, Object>> resources(
      @RequestParam(defaultValue = "") String type,
      @RequestParam(defaultValue = "") String location,
      @RequestParam(defaultValue = "") String competence,
      @RequestParam(defaultValue = "1") int capacity,
      @RequestParam(required = false) Instant start,
      @RequestParam(required = false) Instant end,
      @RequestParam(defaultValue = "false") boolean includeInactive,
      Authentication authentication) {
    if (includeInactive) admin(authentication);
    if ((start == null) != (end == null))
      throw new IllegalArgumentException("Informe início e fim juntos.");
    if (start != null) Rules.interval(start, end);
    // SQL fixo (defeito D09): as variações vêm só de parâmetros, nunca de texto concatenado.
    return db.queryForList(
        "select r.* from resource r where (? or r.active) and r.type like ? and lower(r.location)"
            + " like ? and lower(r.competence) like ? and r.capacity>=? and (cast(? as"
            + " timestamptz) is null or not exists(select 1 from allocation a where"
            + " a.resource_id=r.id and a.active and a.start_at<cast(? as timestamptz) and"
            + " a.end_at>cast(? as timestamptz))) order by r.type,r.name",
        includeInactive,
        type.isEmpty() ? "%" : type,
        "%" + location.toLowerCase(Locale.ROOT) + "%",
        "%" + competence.toLowerCase(Locale.ROOT) + "%",
        capacity,
        start == null ? null : BookingService.ts(end),
        start == null ? null : BookingService.ts(end),
        start == null ? null : BookingService.ts(start));
  }

  @PostMapping("/resources")
  @Transactional
  public Map<String, Long> resource(@Valid @RequestBody Resource r, Authentication a) {
    admin(a);
    long id =
        db.queryForObject(
            "insert into resource(name,type,capacity,location,competence,restricted,active) values"
                + " (?,?,?,?,?,?,?) returning id",
            Long.class,
            r.name(),
            r.type(),
            r.capacity(),
            r.location(),
            r.competence(),
            r.restricted(),
            r.active());
    service.audit(a.getName(), "RECURSO_CRIADO", id, r.toString());
    return Map.of("id", id);
  }

  @PutMapping("/resources/{id}")
  @Transactional
  public void editResource(
      @PathVariable long id, @Valid @RequestBody Resource r, Authentication a) {
    admin(a);
    var previous = db.queryForList("select * from resource where id=? for update", id);
    if (previous.isEmpty()) throw new IllegalArgumentException("Recurso não encontrado.");
    if (db.queryForObject(
            "select count(*) from allocation where resource_id=? and active and end_at>now()",
            Long.class,
            id)
        > 0)
      throw new IllegalArgumentException(
          "Encerre ou cancele as alocações futuras antes de editar o recurso.");
    db.update(
        "update resource set name=?,type=?,capacity=?,location=?,competence=?,restricted=?,active=?"
            + " where id=?",
        r.name(),
        r.type(),
        r.capacity(),
        r.location(),
        r.competence(),
        r.restricted(),
        r.active(),
        id);
    service.audit(a.getName(), "RECURSO_EDITADO", id, "Antes=" + previous + "; depois=" + r);
  }

  @GetMapping("/users")
  public List<Map<String, Object>> users(Authentication a) {
    admin(a);
    return db.queryForList("select id,username,role,enabled from app_user order by id");
  }

  @PostMapping("/users")
  @Transactional
  public void user(@Valid @RequestBody UserInput u, Authentication a) {
    admin(a);
    db.update(
        "insert into app_user(username,password,role) values (?,?,?)",
        u.username(),
        encoder.encode(u.password()),
        u.role());
    service.audit(a.getName(), "USUARIO_CRIADO", null, u.username() + " / " + u.role());
  }

  @PutMapping("/users/{id}/enabled")
  @Transactional
  public void enable(
      @PathVariable long id, @RequestBody Map<String, Boolean> body, Authentication a) {
    admin(a);
    Boolean enabled = body.get("enabled");
    if (enabled == null) throw new IllegalArgumentException("Informe enabled.");
    var users = db.queryForList("select username from app_user where id=?", id);
    if (users.isEmpty()) throw new IllegalArgumentException("Usuário não encontrado.");
    if (a.getName().equals(users.get(0).get("username")))
      throw new IllegalArgumentException("Não desative sua própria conta.");
    db.update("update app_user set enabled=? where id=?", enabled, id);
    service.audit(a.getName(), "USUARIO_ATIVO", id, enabled.toString());
  }

  @GetMapping("/bookings")
  public List<Map<String, Object>> bookings(Authentication a) {
    // Solicitante vê só as próprias reservas; os demais perfis veem todas. SQL fixo (D09).
    return db.queryForList(
        "select b.*, coalesce((select string_agg(r.name, ', ' order by r.name) from allocation al"
            + " join resource r on r.id=al.resource_id where al.booking_id=b.id and"
            + " al.current_version),'') as resources from booking b where (? or b.owner=?)"
            + " order by b.start_at desc",
        !has(a, "SOLICITANTE"),
        a.getName());
  }

  @PostMapping("/bookings")
  public Map<String, Long> create(@Valid @RequestBody Reservation r, Authentication a) {
    return Map.of(
        "id", service.save(null, a.getName(), r.title(), r.start(), r.end(), r.resources()));
  }

  @PutMapping("/bookings/{id}")
  public Map<String, Long> update(
      @PathVariable long id, @Valid @RequestBody Reservation r, Authentication a) {
    return Map.of(
        "id", service.save(id, a.getName(), r.title(), r.start(), r.end(), r.resources()));
  }

  @PostMapping("/bookings/{id}/state")
  public void state(@PathVariable long id, @Valid @RequestBody State s, Authentication a) {
    service.transition(id, s.state(), a.getName(), has(a, "RESPONSAVEL"), s.reason());
  }

  @GetMapping("/bookings/{id}/history")
  public List<Map<String, Object>> history(@PathVariable long id, Authentication a) {
    if (!has(a, "ADMIN") && !has(a, "RESPONSAVEL"))
      throw new AccessDeniedException("Sem permissão para o histórico da reserva.");
    service.get(id);
    return db.queryForList(
        "select id,booking_id,actor,at,previous_state,new_state,reason from booking_history where"
            + " booking_id=? order by id",
        id);
  }

  @PostMapping("/bookings/{id}/movements")
  public void movement(@PathVariable long id, @Valid @RequestBody Movement m, Authentication a) {
    responsible(a);
    service.movement(id, m.resource(), m.kind(), a.getName());
  }

  @GetMapping("/movements")
  public List<Map<String, Object>> movements(Authentication a) {
    responsible(a);
    return db.queryForList("select * from material_movement order by id desc");
  }

  @GetMapping("/maintenance")
  public List<Map<String, Object>> maintenance() {
    return db.queryForList(
        "select a.*,r.name from allocation a join resource r on r.id=a.resource_id where booking_id"
            + " is null and a.active order by start_at");
  }

  @PostMapping("/maintenance")
  public void maintenance(@Valid @RequestBody Maintenance m, Authentication a) {
    admin(a);
    service.maintenance(m.resource(), m.start(), m.end(), m.reason(), a.getName());
  }

  @DeleteMapping("/maintenance/{id}")
  @Transactional
  public void removeMaintenance(@PathVariable long id, Authentication a) {
    admin(a);
    if (db.update(
            "update allocation set active=false where id=? and booking_id is null and active", id)
        != 1) throw new IllegalArgumentException("Bloqueio não encontrado.");
    service.audit(a.getName(), "MANUTENCAO_REMOVIDA", id, "Bloqueio desativado");
  }

  @GetMapping("/audit")
  public List<Map<String, Object>> audit(Authentication a) {
    if (!has(a, "ADMIN") && !has(a, "RESPONSAVEL"))
      throw new AccessDeniedException("Sem permissão para auditoria.");
    return db.queryForList("select * from audit order by id desc limit 500");
  }

  @GetMapping("/notifications")
  public List<Map<String, Object>> notifications(Authentication a) {
    return db.queryForList(
        "select * from notification where recipient=? order by id desc limit 100", a.getName());
  }

  @GetMapping("/reports")
  public Map<String, Object> reports(Authentication a) {
    if (!has(a, "ADMIN") && !has(a, "RESPONSAVEL"))
      throw new AccessDeniedException("Sem permissão para relatórios.");
    return Map.of(
        "resources",
        db.queryForList(
            "select r.id,r.name,r.type,count(b.id) as reservations,coalesce(sum(extract(epoch from"
                + " (al.end_at-al.start_at))/3600) filter(where b.id is not null),0) as hours from"
                + " resource r left join allocation al on al.resource_id=r.id and al.active left"
                + " join booking b on b.id=al.booking_id and b.state in"
                + " ('APROVADA','EM_USO','CONCLUIDA') group by r.id order by r.name"),
        "avoidedConflicts",
        db.queryForObject("select count(*) from avoided_conflict", Long.class));
  }

  /**
   * Relatório de utilização por período (RF12). Conta só a parte de cada reserva APROVADA, EM_USO
   * ou CONCLUIDA que fica dentro de [inicio, fim). Conflitos evitados: tentativas recusadas por
   * sobreposição cujo instante está dentro do período.
   */
  @GetMapping("/relatorios/utilizacao")
  public Map<String, Object> utilizacao(
      @RequestParam Instant inicio, @RequestParam Instant fim, Authentication a) {
    if (!has(a, "ADMIN") && !has(a, "RESPONSAVEL"))
      throw new AccessDeniedException("Sem permissão para relatórios.");
    Rules.interval(inicio, fim);
    var i = BookingService.ts(inicio);
    var f = BookingService.ts(fim);
    String sql =
        "select r.id,r.name,r.type,coalesce((select round(sum(extract(epoch from"
            + " (least(al.end_at,cast(? as timestamptz))-greatest(al.start_at,cast(? as"
            + " timestamptz)))))/3600,2) from allocation al join booking b on b.id=al.booking_id"
            + " where al.resource_id=r.id and al.active and b.state in"
            + " ('APROVADA','EM_USO','CONCLUIDA') and al.start_at<cast(? as timestamptz) and"
            + " al.end_at>cast(? as timestamptz)),0)::float8 as horas from resource r where"
            + " (r.type='PROFESSOR')=? order by r.name,r.id";
    var relatorio = new LinkedHashMap<String, Object>();
    relatorio.put("inicio", inicio.toString());
    relatorio.put("fim", fim.toString());
    relatorio.put("recursos", db.queryForList(sql, f, i, f, i, false));
    relatorio.put("professores", db.queryForList(sql, f, i, f, i, true));
    relatorio.put(
        "conflitosEvitados",
        db.queryForObject(
            "select count(*) from avoided_conflict where created_at>=cast(? as timestamptz) and"
                + " created_at<cast(? as timestamptz)",
            Long.class,
            i,
            f));
    return relatorio;
  }
}
