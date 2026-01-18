create table t_confirmation
(
    user_id      uuid primary key,
    code         numeric(6)               not null,
    is_confirmed boolean                  not null default false,
    confirmed_at timestamp with time zone,
    created_at   timestamp with time zone not null default now(),

    foreign key (user_id) references t_users on delete cascade
);