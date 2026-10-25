package br.edu.recursos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RulesTest {
  private Instant t(long hour) {
    return Instant.EPOCH.plusSeconds(hour * 3600);
  }

  @ParameterizedTest
  @CsvSource({
    "0,2,1,3,true",
    "0,2,2,3,false",
    "0,2,0,2,true",
    "1,2,0,3,true",
    "0,3,1,2,true",
    "2,3,0,2,false",
    "3,4,0,2,false"
  })
  void overlap(long a, long b, long c, long d, boolean expected) {
    assertEquals(expected, Rules.overlaps(t(a), t(b), t(c), t(d)));
  }

  @Test
  void invalidIntervals() {
    assertThrows(IllegalArgumentException.class, () -> Rules.interval(t(1), t(1)));
    assertThrows(IllegalArgumentException.class, () -> Rules.interval(t(2), t(1)));
    assertThrows(IllegalArgumentException.class, () -> Rules.interval(null, t(1)));
    assertThrows(IllegalArgumentException.class, () -> Rules.interval(t(1), null));
  }

  @ParameterizedTest
  @CsvSource({
    "SOLICITADA,APROVADA,true",
    "SOLICITADA,REJEITADA,true",
    "SOLICITADA,CANCELADA,true",
    "SOLICITADA,EM_USO,false",
    "APROVADA,EM_USO,true",
    "APROVADA,CANCELADA,true",
    "APROVADA,NAO_COMPARECEU,true",
    "APROVADA,CONCLUIDA,false",
    "EM_USO,CONCLUIDA,true",
    "EM_USO,CANCELADA,false",
    "CONCLUIDA,APROVADA,false",
    "REJEITADA,APROVADA,false",
    "CANCELADA,APROVADA,false",
    "NAO_COMPARECEU,APROVADA,false"
  })
  void states(String from, String to, boolean expected) {
    assertEquals(expected, Rules.transition(from, to));
  }
}
