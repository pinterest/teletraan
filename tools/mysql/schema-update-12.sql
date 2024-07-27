-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 11 to version 12

CREATE TABLE IF NOT EXISTS agent_counts (
    env_id         VARCHAR(22)         NOT NULL,
    deploy_id         VARCHAR(22)         NOT NULL,
    existing_count   int(11) NOT NULL DEFAULT '0',
    active_count   int(11) NOT NULL DEFAULT '0',
    last_refresh    bigint(20) NOT NULL,
    PRIMARY KEY    (env_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- make sure to update the schema version to 12
UPDATE schema_versions SET version=12;
