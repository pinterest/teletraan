-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 19 to version 20

ALTER TABLE hosts_and_agents ADD COLUMN normandie_status VARCHAR(64) DEFAULT NULL;
ALTER TABLE hosts_and_agents ADD COLUMN knox_status VARCHAR(64) DEFAULT NULL;


-- make sure to update the schema version to 13
UPDATE schema_versions SET version=20;
