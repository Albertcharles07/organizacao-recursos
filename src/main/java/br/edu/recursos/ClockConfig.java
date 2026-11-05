package br.edu.recursos;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio único da aplicação. Testes unitários injetam um {@code Clock.fixed}. */
@Configuration
public class ClockConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
