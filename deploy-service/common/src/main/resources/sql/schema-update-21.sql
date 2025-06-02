-- This script upgrade DB schema from version 20 to version 21
ALTER TABLE hotfixes ADD COLUMN ci_platform VARCHAR(64);

-- make sure to update the schema version to 21
UPDATE schema_versions SET version=21;
