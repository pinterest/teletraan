CREATE DATABASE IF NOT EXISTS deploy;

USE deploy;

/* This is for unit tests only - it will wipe all data - do NOT run it in prodution!*/

DROP TABLE IF EXISTS agents;
DROP TABLE IF EXISTS agent_errors;
DROP TABLE IF EXISTS asg_lifecycle_events;
DROP TABLE IF EXISTS asg_alarms;
DROP TABLE IF EXISTS base_images;
DROP TABLE IF EXISTS builds;
DROP TABLE IF EXISTS clusters;
DROP TABLE IF EXISTS config_history;
DROP TABLE IF EXISTS config_history;
DROP TABLE IF EXISTS datas;
DROP TABLE IF EXISTS deploys;
DROP TABLE IF EXISTS environs;
DROP TABLE IF EXISTS global_envs;
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS groups_and_envs;
DROP TABLE IF EXISTS groups_and_roles;
DROP TABLE IF EXISTS health_checks;
DROP TABLE IF EXISTS healthcheck_errors;
DROP TABLE IF EXISTS host_types;
DROP TABLE IF EXISTS hosts;
DROP TABLE IF EXISTS hosts_and_envs;
DROP TABLE IF EXISTS hotfixes;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS lending_activities;
DROP TABLE IF EXISTS managing_groups;
DROP TABLE IF EXISTS new_instances_reports;
DROP TABLE IF EXISTS placements;
DROP TABLE IF EXISTS promotes;
DROP TABLE IF EXISTS security_zones;
DROP TABLE IF EXISTS spot_auto_scaling_groups;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS tokens_and_roles;
DROP TABLE IF EXISTS user_ratings;
DROP TABLE IF EXISTS users_and_roles;
DROP TABLE IF EXISTS cluster_upgrade_events;
DROP TABLE IF EXISTS pas_configs;
