-- Upgrade repository authorization scopes without changing existing collations.
-- With utf8, each full composite key is at most (175 + 16 + 64) * 3 = 765 bytes,
-- which also fits the 767-byte index limit of older InnoDB row formats.
-- MODIFY would otherwise reset a column-specific collation to the table default.
-- Read the existing charset and collation so authorization equivalence is unchanged.
SET @repository_scope_alter = (
    SELECT CONCAT('ALTER TABLE users_and_roles MODIFY COLUMN resource_id VARCHAR(175) CHARACTER SET ',
                  CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NOT NULL')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='users_and_roles' AND COLUMN_NAME='resource_id'
);
PREPARE repository_scope_upgrade FROM @repository_scope_alter;
EXECUTE repository_scope_upgrade;
DEALLOCATE PREPARE repository_scope_upgrade;

SET @repository_scope_alter = (
    SELECT CONCAT('ALTER TABLE groups_and_roles MODIFY COLUMN resource_id VARCHAR(175) CHARACTER SET ',
                  CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NOT NULL')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='groups_and_roles' AND COLUMN_NAME='resource_id'
);
PREPARE repository_scope_upgrade FROM @repository_scope_alter;
EXECUTE repository_scope_upgrade;
DEALLOCATE PREPARE repository_scope_upgrade;

SET @repository_scope_alter = (
    SELECT CONCAT('ALTER TABLE tokens_and_roles MODIFY COLUMN resource_id VARCHAR(175) CHARACTER SET ',
                  CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME, ' NOT NULL')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='tokens_and_roles' AND COLUMN_NAME='resource_id'
);
PREPARE repository_scope_upgrade FROM @repository_scope_alter;
EXECUTE repository_scope_upgrade;
DEALLOCATE PREPARE repository_scope_upgrade;

UPDATE schema_versions SET version=25;
