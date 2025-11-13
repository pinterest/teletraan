-- This script upgrade DB schema from version 22 to version 23
CREATE TABLE IF NOT EXISTS worker_jobs (
    id                CHAR(36)                                                 NOT NULL,
    job_type          ENUM('INFRA_APPLY')                                      NOT NULL DEFAULT 'INFRA_APPLY',
    config            JSON                                                     NOT NULL,
    status            ENUM('INITIALIZED', 'RUNNING', 'COMPLETED', 'FAILED')    NOT NULL DEFAULT 'INITIALIZED',
    create_at         BIGINT                                                   NOT NULL,
    last_update_at    BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
CREATE INDEX worker_jobs_priority_idx ON worker_jobs(job_type, status, create_at);

-- make sure to update the schema version to 23
UPDATE schema_versions SET version=23;
