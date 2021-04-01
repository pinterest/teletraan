-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 10 to version 11

ALTER TABLE environs MODIFY COLUMN stage_type VARCHAR(32) NOT NULL DEFAULT 'DEFAULT';

-- make sure to update the schema version to 11
UPDATE schema_versions SET version=11;
