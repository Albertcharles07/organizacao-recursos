-- Histórico auditável de mudanças de estado das reservas (RF10, Aula 11).
-- Cada linha: reserva, usuário, instante, estado anterior (nulo na criação), estado novo e motivo.
CREATE TABLE booking_history(
  id bigserial PRIMARY KEY,
  booking_id bigint NOT NULL REFERENCES booking(id),
  actor varchar(80) NOT NULL,
  at timestamptz NOT NULL,
  previous_state varchar(20),
  new_state varchar(20) NOT NULL,
  reason varchar(250) NOT NULL
);
CREATE INDEX booking_history_booking ON booking_history(booking_id);
-- Mesmo bloqueio da tabela audit: o histórico só aceita inserção.
CREATE TRIGGER immutable_booking_history BEFORE UPDATE OR DELETE ON booking_history
  FOR EACH ROW EXECUTE FUNCTION immutable_audit();
