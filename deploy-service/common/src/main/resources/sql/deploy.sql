CREATE DATABASE IF NOT EXISTS deploy;

USE deploy;

set storage_engine = InnoDB;

CREATE TABLE IF NOT EXISTS environs (
    env_id        VARCHAR(22)         NOT NULL,
    env_name      VARCHAR(64)         NOT NULL,
    stage_name    VARCHAR(64)         NOT NULL,
    env_state     VARCHAR(32)         NOT NULL,
    description   VARCHAR(1024),
    build_name    VARCHAR(64),
    branch        VARCHAR(64),
    chatroom      VARCHAR(64),
    deploy_id     VARCHAR(22),
    deploy_type   VARCHAR(32),
    max_parallel  INT                 NOT NULL,
    priority      VARCHAR(16)         NOT NULL,
    stuck_th      INT                 NOT NULL,
    success_th    INT                 NOT NULL,
    adv_config_id VARCHAR(22),
    sc_config_id  VARCHAR(22),
    last_operator VARCHAR(64)         NOT NULL,
    last_update   BIGINT              NOT NULL,
    accept_type   VARCHAR(32)         NOT NULL,
    email_recipients VARCHAR(1024),
    notify_authors   TINYINT(1),
    watch_recipients VARCHAR(1024),
    metrics_config_id VARCHAR(22),
    alarm_config_id VARCHAR(22),
    webhooks_config_id VARCHAR(22),
    max_deploy_num  INT                 NOT NULL,
    max_deploy_day  INT                 NOT NULL,
    PRIMARY KEY   (env_id)
);
CREATE UNIQUE INDEX env_name_stage_idx ON environs (env_name, stage_name);

CREATE TABLE IF NOT EXISTS promotes (
    env_id          VARCHAR(22)         NOT NULL,
    type            VARCHAR(32)         NOT NULL,
    pred_stage      VARCHAR(64),
    queue_size      INT                 NOT NULL,
    schedule        VARCHAR(32),
    delay           INT                 NOT NULL DEFAULT 0,
    disable_policy  VARCHAR(32)         NOT NULL,
    fail_policy     VARCHAR(32)         NOT NULL,
    last_operator   VARCHAR(64)         NOT NULL,
    last_update     BIGINT              NOT NULL,
    PRIMARY KEY     (env_id)
);

/*
Arbitrary text data table, leave the application to interprate the text
The data_kind could be config, envvar or script
*/
CREATE TABLE IF NOT EXISTS datas (
    data_id       VARCHAR(22)         NOT NULL,
    data_kind     VARCHAR(32)         NOT NULL,
    operator      VARCHAR(64)         NOT NULL,
    timestamp     BIGINT              NOT NULL,
    data          TEXT                NOT NULL,
    PRIMARY KEY   (data_id)
);

CREATE TABLE IF NOT EXISTS deploys (
    deploy_id     VARCHAR(22)         NOT NULL,
    deploy_type   VARCHAR(32)         NOT NULL,
    env_id        VARCHAR(22)         NOT NULL,
    build_id      VARCHAR(30)         NOT NULL,
    alias         VARCHAR(22),
    state         VARCHAR(32)         NOT NULL,
    start_date    BIGINT              NOT NULL,
    operator      VARCHAR(64)         NOT NULL,
    last_update   BIGINT              NOT NULL,
    description   VARCHAR(512),
    suc_total     INT                 NOT NULL DEFAULT 0,
    fail_total    INT                 NOT NULL DEFAULT 0,
    total         INT                 NOT NULL DEFAULT 0,
    suc_date      BIGINT,
    acc_status    VARCHAR(32)         NOT NULL,
    from_deploy   VARCHAR(22),
    PRIMARY KEY   (deploy_id)
);
CREATE INDEX deploy_env_idx ON deploys (env_id);
CREATE INDEX deploy_build_idx ON deploys (build_id);

CREATE TABLE IF NOT EXISTS hotfixes (
    id              VARCHAR(22)     NOT NULL,
    env_name        VARCHAR(64)     NOT NULL,
    state           VARCHAR(32)     NOT NULL,
    operator        VARCHAR(32)     NOT NULL,
    job_num         VARCHAR(32),
    job_name        VARCHAR(64)     NOT NULL,
    base_deploy     VARCHAR(22)     NOT NULL,
    base_commit     VARCHAR(64),
    repo            VARCHAR(64)     NOT NULL,
    commits         VARCHAR(2048)   NOT NULL,
    timeout         INT             NOT NULL,
    start_time      BIGINT          NOT NULL,
    progress        INT             NOT NULL,
    error_message   VARCHAR(2048),
    last_worked_on  BIGINT          NOT NULL,
    PRIMARY KEY    (id)
);

CREATE TABLE IF NOT EXISTS hosts (
    host_id         VARCHAR(64)         NOT NULL,
    host_name       VARCHAR(64),
    group_name      VARCHAR(64)         NOT NULL,
    ip              VARCHAR(64),
    create_date     BIGINT              NOT NULL,
    last_update     BIGINT              NOT NULL,
    state           VARCHAR(32)         NOT NULL,
    PRIMARY KEY    (host_id, group_name)
);
CREATE UNIQUE INDEX rev_group_host_idx ON hosts (group_name, host_name);
CREATE INDEX hosts_host_name_idx ON hosts (host_name);
CREATE INDEX hosts_state_idx ON hosts (state);

/*
Report from certain host per environment
*/
CREATE TABLE IF NOT EXISTS agents (
    host_id        VARCHAR(64)         NOT NULL,
    host_name      VARCHAR(64)         NOT NULL,
    env_id         VARCHAR(22)         NOT NULL,
    deploy_id      VARCHAR(22)         NOT NULL,
    deploy_stage   VARCHAR(32)         NOT NULL,
    state          VARCHAR(32)         NOT NULL,
    status         VARCHAR(32)         NOT NULL,
    start_date     BIGINT              NOT NULL,
    last_update    BIGINT              NOT NULL,
    last_operator  VARCHAR(64)         NOT NULL,
    last_err_no    INT                 NOT NULL DEFAULT 0,
    fail_count     INT                 NOT NULL DEFAULT 0,
    first_deploy   TINYINT(1)          NOT NULL DEFAULT 0,
    first_deploy_time     BIGINT,
    stage_start_date     BIGINT,
    PRIMARY KEY    (host_id, env_id)
);
CREATE INDEX agent_env_idx ON agents (env_id, host_name);
CREATE INDEX agent_name_idx ON agents (host_name);
CREATE INDEX agent_stage_idx ON agents (env_id,deploy_stage);

/*
Agent detailed error message
*/
CREATE TABLE IF NOT EXISTS agent_errors (
    host_id     VARCHAR(64)         NOT NULL,
    host_name   VARCHAR(64)         NOT NULL,
    env_id      VARCHAR(22)         NOT NULL,
    error_msg   TEXT                NOT NULL,
    PRIMARY KEY (host_name, env_id)
);

/* Associate individual hosts to env */
CREATE TABLE IF NOT EXISTS hosts_and_envs (
    host_name   VARCHAR(64)         NOT NULL,
    env_id      VARCHAR(22)         NOT NULL,
    PRIMARY KEY (host_name, env_id)
);
CREATE UNIQUE INDEX rev_host_env_idx ON hosts_and_envs (env_id, host_name);

/* Associate groups to env */
CREATE TABLE IF NOT EXISTS groups_and_envs (
    group_name  VARCHAR(64)         NOT NULL,
    env_id      VARCHAR(22)         NOT NULL,
    PRIMARY KEY (group_name, env_id)
);
CREATE UNIQUE INDEX rev_group_env_idx ON groups_and_envs (env_id, group_name);

CREATE TABLE IF NOT EXISTS builds (
    build_id         VARCHAR(30)         NOT NULL,
    build_name       VARCHAR(64)         NOT NULL,
    build_version    VARCHAR(64),
    artifact_url     VARCHAR(512)        NOT NULL,
    scm              VARCHAR(64),
    scm_repo         VARCHAR(64)         NOT NULL,
    scm_branch       VARCHAR(64)         NOT NULL,
    scm_commit_7     VARCHAR(7)          NOT NULL,
    scm_commit       VARCHAR(64)         NOT NULL,
    commit_date      BIGINT              NOT NULL,
    publish_info     VARCHAR(512),
    publish_date     BIGINT              NOT NULL,
    scm_info         VARCHAR(512),
    PRIMARY KEY    (build_id)
);
CREATE INDEX build_name_idx ON builds (build_name, scm_branch, publish_date);
CREATE INDEX build_commit_idx ON builds (scm_commit_7);

CREATE TABLE IF NOT EXISTS global_envs (
    env_name      VARCHAR(64)         NOT NULL,
    pipeline      VARCHAR(16)         NOT NULL,
    last_operator VARCHAR(64)         NOT NULL,
    last_update   BIGINT              NOT NULL,
    PRIMARY KEY   (env_name)
);

CREATE TABLE IF NOT EXISTS user_ratings (
    rating_id  VARCHAR(22)     NOT NULL,
    author     VARCHAR(32)     NOT NULL,
    rating     VARCHAR(8)      NOT NULL,
    feedback   VARCHAR(4096),
    timestamp  BIGINT          NOT NULL,
    PRIMARY KEY (rating_id)
);

CREATE TABLE IF NOT EXISTS groups (
    group_name          VARCHAR(64)     NOT NULL,
    launch_config_id    VARCHAR(81),
    last_update         BIGINT(20)      NOT NULL,
    chatroom            VARCHAR(64),
    email_recipients    VARCHAR(1024),
    pager_recipients    VARCHAR(1024),
    watch_recipients    VARCHAR(1024),
    launch_latency_th   INT             NOT NULL DEFAULT 600,
    instance_type       VARCHAR(64),
    image_id            VARCHAR(128),
    security_group      VARCHAR(128),
    subnets             VARCHAR(128),
    user_data           TEXT,
    iam_role            VARCHAR(128),
    assign_public_ip    TINYINT         NOT NULL DEFAULT 0,
    asg_status          VARCHAR(64),
    healthcheck_state   TINYINT(1)      NOT NULL DEFAULT 0,
    healthcheck_period  BIGINT          NOT NULL DEFAULT 3600,
    lifecycle_state     TINYINT(1)      NOT NULL DEFAULT 0,
    lifecycle_timeout   BIGINT          NOT NULL DEFAULT 600,
    PRIMARY KEY (group_name)
);

CREATE TABLE IF NOT EXISTS asg_alarms (
    alarm_id            VARCHAR(22)        NOT NULL,
    metric_name         VARCHAR(80)        NOT NULL,
    metric_source       VARCHAR(128)       NOT NULL,
    comparator          VARCHAR(30)        NOT NULL,
    action_type         VARCHAR(10)        NOT NULL,
    group_name          VARCHAR(64)        NOT NULL,
    threshold           BIGINT             NOT NULL,
    evaluation_time     INT                NOT NULL,
    last_update         BIGINT             NOT NULL,
    from_aws_metric       TINYINT(1)         NOT NULL DEFAULT 0,
    PRIMARY KEY (alarm_id)
);

CREATE TABLE IF NOT EXISTS config_history (
    config_id           VARCHAR(64)     NOT NULL,
    change_id           VARCHAR(22)     NOT NULL,
    creation_time       BIGINT(20)      NOT NULL,
    operator            VARCHAR(64)     NOT NULL,
    type                VARCHAR(64)     NOT NULL,
    config_change       VARCHAR(8192)   NOT NULL,
    PRIMARY KEY (config_id, change_id)
);

CREATE TABLE IF NOT EXISTS users_and_roles (
    user_name     VARCHAR(64)     NOT NULL,
    resource_id   VARCHAR(64)     NOT NULL,
    resource_type VARCHAR(16)     NOT NULL,
    role          VARCHAR(22)     NOT NULL,
    PRIMARY KEY (resource_id, resource_type, user_name)
);

CREATE TABLE IF NOT EXISTS tokens_and_roles (
    script_name   VARCHAR(64)   NOT NULL,
    resource_id   VARCHAR(64)   NOT NULL,
    resource_type VARCHAR(16)   NOT NULL,
    token         VARCHAR(22)   NOT NULL,
    role          VARCHAR(22)   NOT NULL,
    expire_date BIGINT          NOT NULL,
    PRIMARY KEY (resource_id, resource_type, script_name)
);
CREATE INDEX tokens_and_roles_token_idx ON tokens_and_roles (token);


CREATE TABLE IF NOT EXISTS images (
    id               VARCHAR(30)         NOT NULL,
    app_name         VARCHAR(64)         NOT NULL,
    publish_info     VARCHAR(512),
    publish_date     BIGINT              NOT NULL,
    qualified        TINYINT(1)          NOT NULL DEFAULT 0,
    PRIMARY KEY    (id)
);

CREATE TABLE IF NOT EXISTS groups_and_roles (
  group_name    VARCHAR(64)     NOT NULL,
  resource_id   VARCHAR(64)     NOT NULL,
  resource_type VARCHAR(16)     NOT NULL,
  role          VARCHAR(22)     NOT NULL,
  PRIMARY KEY (resource_id, resource_type, group_name)
);

CREATE TABLE IF NOT EXISTS health_checks (
    id                      VARCHAR(64)      NOT NULL,
    group_name              VARCHAR(64)      NOT NULL,
    env_id                  VARCHAR(22)      NOT NULL,
    deploy_id               VARCHAR(64)      NOT NULL,
    ami_id                  VARCHAR(64)      NOT NULL,
    state                   VARCHAR(64)      NOT NULL,
    status                  VARCHAR(32)      NOT NULL,
    type                    VARCHAR(32)      NOT NULL,
    host_id                 VARCHAR(64),
    host_launch_time        BIGINT,
    host_terminated         TINYINT(1),
    error_message           TEXT,
    deploy_start_time       BIGINT,
    deploy_complete_time    BIGINT,
    state_start_time        BIGINT           NOT NULL,
    start_time              BIGINT           NOT NULL,
    last_worked_on          BIGINT           NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS healthcheck_errors (
    id                  VARCHAR(64)         NOT NULL,
    env_id              VARCHAR(22)         NOT NULL,
    deploy_stage        VARCHAR(32)         NOT NULL,
    agent_state         VARCHAR(32)         NOT NULL,
    agent_status        VARCHAR(32)         NOT NULL,
    last_err_no         INT                 NOT NULL DEFAULT 0,
    fail_count          INT                 NOT NULL DEFAULT 0,
    error_msg           TEXT                NOT NULL,
    agent_start_date    BIGINT              NOT NULL,
    agent_last_update   BIGINT              NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS new_instances_reports (
   host_id              VARCHAR(64)        NOT NULL,
   env_id               VARCHAR(64)        NOT NULL,
   launch_time          BIGINT,
   reported             TINYINT            NOT NULL DEFAULT 0,
   PRIMARY KEY (host_id, env_id)
);

CREATE TABLE IF NOT EXISTS asg_lifecycle_events (
   token_id       VARCHAR(128)  NOT NULL,
   hook_id        VARCHAR(64)   NOT NULL,
   group_name     VARCHAR(64)   NOT NULL,
   host_id        VARCHAR(64)   NOT NULL,
   start_date     BIGINT        NOT NULL,
   PRIMARY KEY (token_id)
);


CREATE TABLE IF NOT EXISTS lending_activities (
   id          VARCHAR(32)   NOT NULL,
   group_name  VARCHAR(32)   NOT NULL,
   actity_type VARCHAR(10)   NOT NULL,
   reason      VARCHAR(1024) NOT NULL,
   update_time  BIGINT       NOT NULL,
   PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS managing_groups (
   group_name          VARCHAR(32) NOT NULL,
   max_lending_size    INT         NOT NULL,
   lending_priority    VARCHAR(16) NOT NULL,
   batch_size          INT         NOT NULL,
   cool_down           INT         NOT NULL,
   lent_size           INT         NOT NULL,
   last_activity_time  BIGINT      NOT NULL,
   PRIMARY KEY (group_name)
);

