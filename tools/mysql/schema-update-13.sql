-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 12 to version 13

ALTER TABLE hosts_and_agents ADD COLUMN host_name VARCHAR(64) DEFAULT NULL;
ALTER TABLE hosts_and_agents ADD COLUMN ip VARCHAR(64) DEFAULT NULL;
ALTER TABLE hosts_and_agents ADD COLUMN create_date BIGINT(20) DEFAULT NULL;
ALTER TABLE hosts_and_agents ADD COLUMN last_update BIGINT(20) DEFAULT NULL;
ALTER TABLE hosts_and_agents ADD COLUMN auto_scaling_group VARCHAR(128) DEFAULT NULL;

CREATE INDEX hosts_host_name_idx ON hosts_and_agents (host_name);

-- make sure to update the schema version to 13
UPDATE schema_versions SET version=13;
