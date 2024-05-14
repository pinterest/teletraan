-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 5 to version 6

ALTER TABLE environs ADD COLUMN ensure_trusted_build TINYINT(1) NOT NULL DEFAULT 0;

-- make sure to update the schema version to 6
UPDATE schema_versions SET version=6;