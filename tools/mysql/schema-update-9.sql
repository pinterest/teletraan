-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 8 to version 9

ALTER TABLE hosts MODIFY group_name VARCHAR(128) NOT NULL;

-- make sure to update the schema version to 9
UPDATE schema_versions SET version=9;
