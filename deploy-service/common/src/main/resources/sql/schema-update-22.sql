-- This script upgrade DB schema from version 21 to version 22
ALTER TABLE environs ADD COLUMN external_project_name VARCHAR(255) DEFAULT NULL;

-- make sure to update the schema version to 22
UPDATE schema_versions SET version=22;
