-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

-- This script upgrade DB schema from version 18 to version 19

CREATE TABLE `pindeploy` (
  `env_id` varchar(22) NOT NULL,
  `is_pindeploy` tinyint(1) NOT NULL DEFAULT '0',
  `pipeline` varchar(128) NOT NULL DEFAULT "",
  PRIMARY KEY (`env_id`),
  KEY `pindeploy_env_id_idx` (`env_id`, `is_pindeploy`, `pipeline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

UPDATE schema_versions SET version=19;