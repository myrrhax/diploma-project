ALTER TABLE t_ddl_scripts
DROP COLUMN script,
ADD COLUMN script_file_id uuid not null references t_files;
