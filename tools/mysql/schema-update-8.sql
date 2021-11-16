-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 7 to version 8

CREATE TABLE IF NOT EXISTS groups (
    group_name varchar(128) NOT NULL,
    launch_config_id varchar(81) DEFAULT NULL,
    last_update bigint(20) NOT NULL DEFAULT '0',
    chatroom varchar(64) DEFAULT NULL,
    watch_recipients varchar(1024) DEFAULT NULL,
    email_recipients varchar(1024) DEFAULT NULL,
    launch_latency_th int(11) NOT NULL DEFAULT '600',
    instance_type varchar(64) DEFAULT NULL,
    image_id varchar(128) DEFAULT NULL,
    security_group varchar(128) DEFAULT NULL,
    user_data text,
    subnets varchar(128) DEFAULT NULL,
    iam_role varchar(128) DEFAULT NULL,
    assign_public_ip tinyint(1) NOT NULL DEFAULT '0',
    asg_status varchar(64) NOT NULL DEFAULT 'UNKNOWN',
    pager_recipients varchar(1024) DEFAULT NULL,
    healthcheck_state tinyint(1) NOT NULL DEFAULT '0',
    healthcheck_period bigint(20) NOT NULL DEFAULT '3600',
    lifecycle_state tinyint(1) NOT NULL DEFAULT '0',
    lifecycle_timeout bigint(20) NOT NULL DEFAULT '600',
    lifecycle_notifications tinyint(1) NOT NULL DEFAULT '0',
    load_balancers varchar(64) DEFAULT NULL,
    PRIMARY KEY (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

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
