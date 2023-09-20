-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 18 to version 19

ALTER TABLE agents ADD COLUMN container_health_status VARCHAR(32) NOT NULL DEFAULT "N/A (Not Applicable)";

UPDATE schema_versions SET version=19;