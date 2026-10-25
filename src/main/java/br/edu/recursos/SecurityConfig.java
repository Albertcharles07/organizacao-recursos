package br.edu.recursos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(JdbcTemplate db) {
    return username ->
        db
            .query(
                "select username,password,role,enabled from app_user where username=?",
                (rs, n) ->
                    User.withUsername(rs.getString(1))
                        .password(rs.getString(2))
                        .roles(rs.getString(3))
                        .disabled(!rs.getBoolean(4))
                        .build(),
                username)
            .stream()
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Usuário inválido"));
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/", "/index.html", "/app.js", "/style.css", "/api/csrf", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/login")
                    .successHandler((q, r, a) -> r.setStatus(204))
                    .failureHandler((q, r, e) -> r.sendError(401, "Credenciais inválidas")))
        .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((q, r, a) -> r.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((q, r, x) -> r.sendError(401, "Entre na sua conta"))
                    .accessDeniedHandler((q, r, x) -> r.sendError(403, "Acesso negado")))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self';"
                                + " frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
        .build();
  }

  @Bean
  ApplicationRunner bootstrap(
      JdbcTemplate db, PasswordEncoder encoder, @Value("${app.admin-password}") String password) {
    return args -> {
      if (db.queryForObject("select count(*) from app_user", Long.class) == 0) {
        if (password.length() < 12)
          throw new IllegalStateException("Defina ADMIN_PASSWORD com pelo menos 12 caracteres.");
        db.update(
            "insert into app_user(username,password,role) values (?,?,?)",
            "admin",
            encoder.encode(password),
            "ADMIN");
      }
    };
  }
}
