CREATE TABLE t_invitation
(
    id           UUID PRIMARY KEY,
    initiator_id UUID                     NOT NULL REFERENCES t_users ON DELETE CASCADE,
    scheme_id    UUID                     NOT NULL REFERENCES t_schemes ON DELETE CASCADE,
    is_confirmed BOOLEAN                  NOT NULL DEFAULT false,
    authorities  varchar(50)[] NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);