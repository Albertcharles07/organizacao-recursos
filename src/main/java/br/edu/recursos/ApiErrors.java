package br.edu.recursos;

import java.security.Principal;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  private final JdbcTemplate db;

  public ApiErrors(JdbcTemplate db) {
    this.db = db;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<?> invalid(IllegalArgumentException e) {
    return error(400, e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<?> validation(Exception e) {
    return error(400, "Revise os campos: valores obrigatórios, formatos, tamanhos e datas.");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied(AccessDeniedException e) {
    return error(403, e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(DataIntegrityViolationException e, Principal principal) {
    Throwable cause = e;
    boolean overlap = false;
    while (cause != null) {
      if (cause instanceof java.sql.SQLException sql && "23P01".equals(sql.getSQLState()))
        overlap = true;
      cause = cause.getCause();
    }
    if (overlap) {
      db.update(
          "insert into avoided_conflict(actor) values (?)",
          principal == null ? "anonymous" : principal.getName());
      return error(409, "Horário indisponível: existe reserva ou manutenção para um dos recursos.");
    }
    return error(409, "Registro duplicado ou operação incompatível com os dados existentes.");
  }

  private ResponseEntity<?> error(int status, String message) {
    return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
  }
}
