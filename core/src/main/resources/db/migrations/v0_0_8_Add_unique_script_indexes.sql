CREATE UNIQUE INDEX idx_uq_full_script
ON t_ddl_scripts (v_id, script_type)
WHERE generated_type = 'FULL';

CREATE UNIQUE INDEX idx_uq_migration_script
ON t_ddl_scripts (v_id, from_v_id, script_type)
WHERE generated_type = 'MIGRATION';