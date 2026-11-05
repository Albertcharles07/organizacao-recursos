package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Teste unitário da regra "toda mudança de estado gera um registro de histórico" (RF10, Aula 11).
 *
 * <p>Isolado do banco com um dublê de {@link JdbcTemplate} que só anota os comandos executados, e
 * com um {@link Clock} fixo, para provar que o instante gravado vem do relógio injetado e não do
 * relógio do sistema. A gravação real, na mesma transação e com rollback, é verificada em
 * {@link HistoricoEstadosIT}.
 */
class HistoricoEstadosTest {
  static final Instant AGORA = Instant.parse("2030-01-10T12:00:00Z");
  static final Instant INICIO = Instant.parse("2030-01-11T09:00:00Z");
  static final Instant FIM = Instant.parse("2030-01-11T11:00:00Z");
  static final String INSERT_HISTORICO = "insert into booking_history";

  /** Dublê do banco: devolve uma reserva fixa e registra cada update executado. */
  static class BancoFalso extends JdbcTemplate {
    final List<List<Object>> updates = new ArrayList<>();
    final Map<String, Object> reserva = new HashMap<>();

    BancoFalso(String estado) {
      reserva.put("id", 7L);
      reserva.put("owner", "ana");
      reserva.put("state", estado);
      reserva.put("start_at", Timestamp.from(INICIO));
      reserva.put("end_at", Timestamp.from(FIM));
    }

    @Override
    public int update(String sql, Object... args) {
      var linha = new ArrayList<Object>();
      linha.add(sql);
      linha.addAll(Arrays.asList(args));
      updates.add(linha);
      return 1;
    }

    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) {
      if (sql.startsWith("select * from booking")) return List.of(reserva);
      if (sql.startsWith("select * from resource")) return List.of(Map.of("active", true));
      return List.of();
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> tipo, Object... args) {
      if (tipo == Long.class) return tipo.cast(sql.contains("count(*)") ? 0L : 7L);
      if (tipo == Boolean.class) return tipo.cast(false);
      return null;
    }

    List<List<Object>> historico() {
      return updates.stream().filter(u -> u.get(0).toString().startsWith(INSERT_HISTORICO)).toList();
    }
  }

  BookingService servico(BancoFalso db) {
    return new BookingService(db, evento -> {}, Clock.fixed(AGORA, ZoneOffset.UTC));
  }

  @Test
  void transicaoGravaReservaUsuarioInstanteEstadosEMotivo() {
    var db = new BancoFalso("SOLICITADA");
    servico(db).transition(7, "APROVADA", "gestor", true, "Liberado pela coordenação");

    assertEquals(1, db.historico().size());
    var registro = db.historico().get(0);
    assertEquals(7L, registro.get(1), "reserva");
    assertEquals("gestor", registro.get(2), "usuário");
    assertEquals(Timestamp.from(AGORA), registro.get(3), "instante deve vir do Clock injetado");
    assertEquals("SOLICITADA", registro.get(4), "estado anterior");
    assertEquals("APROVADA", registro.get(5), "estado novo");
    assertEquals("Liberado pela coordenação", registro.get(6), "motivo");
  }

  @Test
  void transicaoSemMotivoGravaMotivoPadrao() {
    for (String motivo : Arrays.asList(null, "", "   ")) {
      var db = new BancoFalso("APROVADA");
      servico(db).transition(7, "CANCELADA", "ana", false, motivo);
      assertEquals("Não informado", db.historico().get(0).get(6));
    }
  }

  @Test
  void transicaoInvalidaNaoGravaHistoricoNemMudaEstado() {
    var db = new BancoFalso("SOLICITADA");
    assertThrows(
        IllegalArgumentException.class,
        () -> servico(db).transition(7, "CONCLUIDA", "gestor", true, "Tentativa"));
    assertTrue(db.historico().isEmpty());
    assertTrue(db.updates.stream().noneMatch(u -> u.get(0).toString().startsWith("update booking")));
  }

  @Test
  void criacaoGravaHistoricoSemEstadoAnterior() {
    var db = new BancoFalso("APROVADA");
    long id = servico(db).save(null, "ana", "Aula", INICIO, FIM, List.of(3L));

    var registro = db.historico().get(0);
    assertEquals(id, registro.get(1));
    assertEquals("ana", registro.get(2));
    assertEquals(Timestamp.from(AGORA), registro.get(3));
    assertNull(registro.get(4), "uma reserva nova não tem estado anterior");
    assertEquals("APROVADA", registro.get(5));
    assertEquals("Reserva criada", registro.get(6));
  }

  @Test
  void alteracaoGravaEstadoAnterior() {
    var db = new BancoFalso("APROVADA");
    servico(db).save(7L, "ana", "Aula alterada", INICIO, FIM, List.of(3L));

    var registro = db.historico().get(0);
    assertEquals(7L, registro.get(1));
    assertEquals("APROVADA", registro.get(4));
    assertEquals("APROVADA", registro.get(5));
    assertEquals("Reserva alterada", registro.get(6));
  }

  @Test
  void regrasDeTempoUsamOClockInjetado() {
    // Para o Clock fixo, a reserva de 11/01/2030 ainda não começou: aprovar é permitido.
    var db = new BancoFalso("SOLICITADA");
    assertDoesNotThrow(() -> servico(db).transition(7, "APROVADA", "gestor", true, null));

    // Com o relógio depois do início, a mesma aprovação expira.
    var depois = new BookingService(db, e -> {}, Clock.fixed(FIM, ZoneOffset.UTC));
    assertThrows(
        IllegalArgumentException.class, () -> depois.transition(7, "APROVADA", "gestor", true, null));
  }
}
