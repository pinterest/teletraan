-- This script upgrade DB schema from version 23 to version 24
ALTER TABLE environs ADD COLUMN use_entitlements TINYINT(1) NOT NULL DEFAULT '0';

-- make sure to update the schema version to 24
UPDATE schema_versions SET version=24;
