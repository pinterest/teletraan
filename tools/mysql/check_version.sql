# Create schema_versions if necessary, and set the current version to 0
CREATE TABLE IF NOT EXISTS schema_versions (
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Insert if not already
INSERT INTO schema_versions (version)
SELECT 0 FROM DUAL
WHERE NOT EXISTS (SELECT * FROM schema_versions);

# Return the actual version
SELECT version FROM schema_versions;
