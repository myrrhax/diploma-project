ALTER TABLE t_ddl_scripts
RENAME COLUMN dbms_type to script_type;

ALTER TABLE t_ddl_scripts
DROP COLUMN dtype;

ALTER TABLE t_ddl_scripts
ADD COLUMN generated_type VARCHAR(55) NOT NULL;