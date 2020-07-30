-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 10 to version 11
CREATE TABLE IF NOT EXISTS shards_and_hosts (
    host_id         VARCHAR(64)         NOT NULL,
    host_name       VARCHAR(64),
    shard_name      VARCHAR(64)         NOT NULL,
    PRIMARY KEY    (host_id, shard_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX rev_shard_host_idx ON shards_and_hosts (shard_name, host_name);

-- make sure to update the schema version to 11
UPDATE schema_versions SET version=11;
