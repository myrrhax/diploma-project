create table t_confirmation
(
    user_id      uuid primary key,
    code         char(6)                  not null,
    created_at   timestamp with time zone not null default now(),
    expires_at   timestamp with time zone,

    foreign key (user_id) references t_users on delete cascade
);

create index if not exists uq_confirmation_code on t_confirmation(user_id, code);