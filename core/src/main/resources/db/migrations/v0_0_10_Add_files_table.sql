CREATE TABLE t_files
(
    id uuid primary key,
    original_name varchar(256) not null,
    size_bytes bigint not null,
    media_type varchar(55) not null,
    storage_provider varchar(55) not null,
    created_at timestamptz not null default now()
);