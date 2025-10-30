-- This script upgrade DB schema from version 22 to version 23
ALTER TABLE environs ADD COLUMN use_entitlements TINYINT(1) NOT NULL DEFAULT '0';

-- make sure to update the schema version to 23
UPDATE schema_versions SET version=23;
