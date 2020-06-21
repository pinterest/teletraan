-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 7 to version 8

ALTER TABLE environs ADD COLUMN stage_type VARCHAR(32) NOT NULL;

-- make sure to update the schema version to 8
UPDATE schema_versions SET version=8;
