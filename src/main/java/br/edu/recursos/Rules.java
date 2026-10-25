package br.edu.recursos;

import java.time.Instant;
import java.util.Set;

public final class Rules {
  private Rules() {}

  public static void interval(Instant start, Instant end) {
    if (start == null || end == null || !end.isAfter(start))
      throw new IllegalArgumentException("O término deve ser posterior ao início.");
  }

  public static boolean overlaps(Instant a, Instant b, Instant c, Instant d) {
    interval(a, b);
    interval(c, d);
    return a.isBefore(d) && c.isBefore(b);
  }

  public static boolean transition(String from, String to) {
    return switch (from) {
      case "SOLICITADA" -> Set.of("APROVADA", "REJEITADA", "CANCELADA").contains(to);
      case "APROVADA" -> Set.of("EM_USO", "CANCELADA", "NAO_COMPARECEU").contains(to);
      case "EM_USO" -> to.equals("CONCLUIDA");
      default -> false;
    };
  }
}
