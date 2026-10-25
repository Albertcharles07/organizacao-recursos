CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE app_user(id bigserial PRIMARY KEY, username varchar(80) UNIQUE NOT NULL, password varchar(100) NOT NULL, role varchar(20) NOT NULL CHECK(role IN ('SOLICITANTE','RESPONSAVEL','ADMIN')), enabled boolean NOT NULL DEFAULT true);
CREATE TABLE resource(id bigserial PRIMARY KEY, name varchar(120) NOT NULL, type varchar(20) NOT NULL CHECK(type IN ('SALA','PROFESSOR','MATERIAL')), capacity integer NOT NULL CHECK(capacity>0), location varchar(120) NOT NULL, competence varchar(120) NOT NULL DEFAULT '', restricted boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true);
CREATE TABLE booking(id bigserial PRIMARY KEY, owner varchar(80) NOT NULL REFERENCES app_user(username), title varchar(160) NOT NULL, start_at timestamptz NOT NULL, end_at timestamptz NOT NULL, state varchar(20) NOT NULL CHECK(state IN ('SOLICITADA','APROVADA','EM_USO','CONCLUIDA','REJEITADA','CANCELADA','NAO_COMPARECEU')), CHECK(end_at>start_at));
CREATE TABLE allocation(id bigserial PRIMARY KEY, resource_id bigint NOT NULL REFERENCES resource(id), booking_id bigint REFERENCES booking(id), start_at timestamptz NOT NULL, end_at timestamptz NOT NULL, active boolean NOT NULL DEFAULT true, reason varchar(160), CHECK(end_at>start_at), EXCLUDE USING gist(resource_id WITH =, tstzrange(start_at,end_at,'[)') WITH &&) WHERE(active));
CREATE INDEX allocation_booking ON allocation(booking_id);
CREATE TABLE audit(id bigserial PRIMARY KEY, actor varchar(80) NOT NULL, action varchar(50) NOT NULL, entity_id bigint, detail text NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE FUNCTION immutable_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Audit is append-only'; END $$;
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit FOR EACH ROW EXECUTE FUNCTION immutable_audit();
CREATE TABLE notification(id bigserial PRIMARY KEY, booking_id bigint NOT NULL REFERENCES booking(id), recipient varchar(80) NOT NULL, message varchar(250) NOT NULL, created_at timestamptz NOT NULL DEFAULT now());
CREATE TABLE material_movement(id bigserial PRIMARY KEY, booking_id bigint NOT NULL REFERENCES booking(id), resource_id bigint NOT NULL REFERENCES resource(id), kind varchar(10) NOT NULL CHECK(kind IN ('RETIRADA','DEVOLUCAO')), actor varchar(80) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(booking_id,resource_id,kind));
CREATE TABLE avoided_conflict(id bigserial PRIMARY KEY, actor varchar(80) NOT NULL, created_at timestamptz NOT NULL DEFAULT now());

ALTER TABLE notification ADD COLUMN delivery varchar(12) NOT NULL DEFAULT 'SIMULADA';

ALTER TABLE allocation ADD COLUMN current_version boolean NOT NULL DEFAULT true;
