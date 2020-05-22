-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT   
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

CREATE INDEX agent_first_deploy_state_idx ON agents (env_id,first_deploy, deploy_stage ,state);

-- make sure to update the schema version to 2
UPDATE schema_versions SET version=2;
