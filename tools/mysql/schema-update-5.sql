-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 4 to version 5

ALTER TABLE environs ADD COLUMN allow_private_build TINYINT(1) NOT NULL DEFAULT 0;

-- make sure to update the schema version to 5
UPDATE schema_versions SET version=5;
