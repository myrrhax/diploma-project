ALTER TABLE t_invitation
ADD COLUMN receiver_email VARCHAR (255) NOT NULL;

CREATE UNIQUE INDEX idx_uq_schema_id_receiver_email ON t_invitation (scheme_id, receiver_email);