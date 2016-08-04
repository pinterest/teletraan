# !!!!!!!!!!!!!!!!!!!!!!!!!!!!
# BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script assumes your current DB schema is the same as release 1.0.1(SHA #fdd68c0) or newer
# It will upgrade the schema to release 1.0.2, or SHA #xxxxx

# Change the following statement to use your DB if needed
USE deploy;

# Delete all the tables not needed in release 1.0.2
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS asg_alarms;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS health_checks;
DROP TABLE IF EXISTS healthcheck_errors;
DROP TABLE IF EXISTS new_instances_reports;

# Create new tables not in release 1.0.1
CREATE TABLE IF NOT EXISTS tags (
  id VARCHAR(30) NOT NULL,
  value VARCHAR(30) NOT NULL,
  target_type varchar(30) NOT NULL,
  target_id varchar(64) NOT NULL,
  operator varchar(64) NOT NULL,
  created_date bigint(20) NOT NULL,
  comments varchar(256),
  meta_info text,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
#CREATE INDEX tags_target_id_idx ON tags(target_id, target_type, created_date);
#CREATE INDEX tags_value_target_type_idx ON tags(value, target_type, created_date);
#CREATE INDEX tags_target_type_idx ON tags(target_type, created_date);

CREATE TABLE IF NOT EXISTS schedules (
    id                  VARCHAR(22)     NOT NULL,
    total_sessions      INT             NOT NULL DEFAULT 0,
    cooldown_times      VARCHAR(32)     NOT NULL,
    host_numbers        VARCHAR(32)     NOT NULL,
    current_session     INT             NOT NULL DEFAULT 0,
    state               VARCHAR(32)     NOT NULL DEFAULT "NOT_STARTED",
    state_start_time    BIGINT          NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Add new columns for release 1.0.2. Ignore any errors if column already exists, no harm done
ALTER TABLE builds ADD COLUMN publisher VARCHAR(64);
ALTER TABLE hosts ADD COLUMN can_retire TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE environs ADD COLUMN system_priority INT;
ALTER TABLE environs ADD COLUMN is_docker TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE environs ADD COLUMN max_parallel_pct TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE environs ADD COLUMN state VARCHAR(32) NOT NULL DEFAULT "NORMAL";
ALTER TABLE environs ADD COLUMN cluster_name VARCHAR(128);
ALTER TABLE environs ADD COLUMN max_parallel_rp INT NOT NULL DEFAULT 1;
ALTER TABLE environs ADD COLUMN override_policy VARCHAR(32) NOT NULL DEFAULT "OVERRIDE";
ALTER TABLE environs ADD COLUMN schedule_id VARCHAR(22);

