-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 12 to version 13

ALTER TABLE environs ADD CONSTRAINT unique_agent_config UNIQUE (adv_config_id);
ALTER TABLE environs ADD CONSTRAINT unique_script_config UNIQUE (sc_config_id);
ALTER TABLE environs ADD CONSTRAINT unique_metrics_config UNIQUE (metrics_config_id);
ALTER TABLE environs ADD CONSTRAINT unique_alarm_config UNIQUE (alarm_config_id);
ALTER TABLE environs ADD CONSTRAINT unique_schedule_config UNIQUE (schedule_id);

-- make sure to update the schema version to 13
UPDATE schema_versions SET version=13;
