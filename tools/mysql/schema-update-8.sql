-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 7 to version 8

ALTER TABLE environs ADD COLUMN stage_type VARCHAR(32) NOT NULL DEFAULT 'PRODUCTION';
ALTER TABLE groups MODIFY group_name VARCHAR(128) NOT NULL;
ALTER TABLE groups_and_envs MODIFY group_name VARCHAR(128) NOT NULL;

CREATE TABLE IF NOT EXISTS hosts_and_agents (
    host_id         VARCHAR(64)         NOT NULL,
    agent_version   VARCHAR(64)  DEFAULT 'UNKNOWN',

    PRIMARY KEY    (host_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- make sure to update the schema version to 8
UPDATE schema_versions SET version=8;
