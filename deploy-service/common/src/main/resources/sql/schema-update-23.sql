-- This script upgrade DB schema from version 21 to version 23
CREATE TABLE IF NOT EXISTS infra_jobs (
    id                  CHAR(36)        NOT NULL,
    infra_config        VARCHAR(8192)   NOT NULL,
    create_at           BIGINT          NOT NULL,
    last_update_at      BIGINT          NOT NULL,
    status              CHAR(11)        NOT NULL DEFAULT "INITIALIZED",
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX infra_jobs_priority_idx ON infra_jobs(create_at, status);

-- make sure to update the schema version to 23
UPDATE schema_versions SET version=23;
