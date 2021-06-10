-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
-- ALWAYS BACKUP YOUR DATA BEFORE EXECUTING THIS SCRIPT
-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

# This script upgrade DB schema from version 14 to version 15

CREATE TABLE IF NOT EXISTS services (
    svc_name VARCHAR(22) NOT NULL;
    system_priority int(11) DEFAULT NULL;
    PRIMARY KEY (svc_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE schema_versions SET version=15;
