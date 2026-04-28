ALTER TABLE t_files
ADD COLUMN is_public boolean NOT NULL DEFAULT false,
ADD COLUMN scheme_id uuid references t_schemes;