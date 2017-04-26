-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 1 to version 2

ALTER TABLE schedules MODIFY COLUMN cooldown_times VARCHAR(2048) NOT NULL;
ALTER TABLE schedules MODIFY COLUMN host_numbers VARCHAR(2048) NOT NULL;

-- make sure to update the schema version to current+1
UPDATE schema_versions SET version=2;