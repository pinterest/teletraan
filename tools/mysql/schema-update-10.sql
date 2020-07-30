-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 9 to version 10

CREATE TABLE IF NOT EXISTS shards_and_envs (
    shard_name  VARCHAR(64)         NOT NULL,
    env_id      VARCHAR(22)         NOT NULL,
    PRIMARY KEY (shard_name, env_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX rev_shard_env_idx ON shards_and_envs (env_id, shard_name);

-- make sure to update the schema version to 10
UPDATE schema_versions SET version=10;
