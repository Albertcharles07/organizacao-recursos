package br.edu.recursos;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class ActiveAccountFilter extends OncePerRequestFilter {
  private final JdbcTemplate db;

  public ActiveAccountFilter(JdbcTemplate db) {
    this.db = db;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.isAuthenticated()
        && !auth.getName().equals("anonymousUser")
        && db.queryForObject(
                "select count(*) from app_user where username=? and enabled",
                Long.class,
                auth.getName())
            == 0) {
      SecurityContextHolder.clearContext();
      if (q.getSession(false) != null) q.getSession(false).invalidate();
      r.sendError(401, "Conta desativada");
      return;
    }
    chain.doFilter(q, r);
  }
}
