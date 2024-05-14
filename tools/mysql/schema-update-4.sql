-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 3 to version 4

ALTER TABLE environs DROP COLUMN project_id;
ALTER TABLE environs ADD COLUMN external_id CHAR(36);

-- make sure to update the schema version to 4
UPDATE schema_versions SET version=4;
