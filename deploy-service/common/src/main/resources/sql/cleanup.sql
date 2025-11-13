CREATE DATABASE IF NOT EXISTS deploy;

USE deploy;

/* This is for unit tests only - it will wipe all data - do NOT run it in prodution!*/

DROP TABLE IF EXISTS schema_versions;
DROP TABLE IF EXISTS agents;
DROP TABLE IF EXISTS agent_errors;
DROP TABLE IF EXISTS builds;
DROP TABLE IF EXISTS config_history;
DROP TABLE IF EXISTS datas;
DROP TABLE IF EXISTS deploys;
DROP TABLE IF EXISTS environs;
DROP TABLE IF EXISTS global_envs;
DROP TABLE IF EXISTS groups_and_envs;
DROP TABLE IF EXISTS groups_and_roles;
DROP TABLE IF EXISTS hosts;
DROP TABLE IF EXISTS host_tags;
DROP TABLE IF EXISTS hosts_and_envs;
DROP TABLE IF EXISTS hotfixes;
DROP TABLE IF EXISTS promotes;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS tokens_and_roles;
DROP TABLE IF EXISTS user_ratings;
DROP TABLE IF EXISTS users_and_roles;
DROP TABLE IF EXISTS schedules;
DROP TABLE IF EXISTS hosts_and_agents;
DROP TABLE IF EXISTS deploy_constraints;
DROP TABLE IF EXISTS agent_counts;
DROP TABLE IF EXISTS pindeploy;
DROP TABLE IF EXISTS worker_jobs;
