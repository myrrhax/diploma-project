CREATE TABLE t_users
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    is_confirmed  BOOLEAN      NOT NULL    DEFAULT false,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE t_schemes
(
    id           UUID PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    creator_id   UUID         NOT NULL REFERENCES t_users ON DELETE CASCADE,
    current_v_id BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE t_scheme_version
(
    id              BIGSERIAL PRIMARY KEY,
    scheme_id       UUID REFERENCES t_schemes ON DELETE CASCADE,
    hash_sum        VARCHAR(256),
    parent_id       BIGINT  REFERENCES t_scheme_version ON DELETE SET NULL,
    is_initial      BOOLEAN NOT NULL         DEFAULT false,
    tag             VARCHAR(55),
    schema          TEXT,
    is_working_copy BOOLEAN                  DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);

ALTER TABLE t_schemes
    ADD CONSTRAINT fK_schemes_scheme_version FOREIGN KEY (current_v_id) REFERENCES t_scheme_version;

CREATE UNIQUE INDEX uidx_scheme_id_hash
    ON t_scheme_version (scheme_id, hash_sum);

CREATE UNIQUE INDEX uidx_scheme_id_tag
    ON t_scheme_version (scheme_id, tag);

CREATE TABLE t_authorities
(
    id         SERIAL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES t_users ON DELETE CASCADE,
    scheme_id  UUID        NOT NULL REFERENCES t_schemes ON DELETE CASCADE,
    type       VARCHAR(55) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE UNIQUE INDEX uidx_authority
    ON t_authorities (user_id, scheme_id, type);

CREATE TABLE t_ddl_scripts
(
    id         UUID PRIMARY KEY,
    dbms_type  VARCHAR(55) NOT NULL,
    dtype      VARCHAR(31) NOT NULL,
    script     TEXT        NOT NULL,
    v_id       BIGINT      NOT NULL REFERENCES t_scheme_version ON DELETE CASCADE,
    from_v_id  BIGINT      NOT NULL REFERENCES t_scheme_version ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);