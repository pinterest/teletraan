-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 2 to version 3


ALTER TABLE environs ADD COLUMN project_id INT(11);

-- make sure to update the schema version to 3
UPDATE schema_versions SET version=3;
