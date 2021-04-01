-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 6 to version 7

ALTER TABLE environs MODIFY chatroom VARCHAR(128) DEFAULT NULL;

-- make sure to update the schema version to 7
UPDATE schema_versions SET version=7;
