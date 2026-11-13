/**
 * Copyright (c) 2026 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ibatis.common.jdbc.ScriptRunner;
import com.pinterest.deployservice.bean.GroupRolesBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DBScmRepositoryRolesMigrationTest {
    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0-oracle")
                    .withDatabaseName("deploy")
                    .withUsername("test")
                    .withPassword("test")
                    .withUrlParam("characterEncoding", "UTF-8")
                    .withCommand("--sql-mode=STRICT_ALL_TABLES");

    private static BasicDataSource dataSource;
    private static DBUserRolesDAOImpl users;
    private static DBGroupRolesDAOImpl groups;
    private static DBTokenRolesDAOImpl tokens;

    @BeforeAll
    static void createDataSource() {
        dataSource =
                DatabaseUtil.createDataSource(
                        DatabaseUtil.MYSQL_JDBC_DRIVER,
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword(),
                        "0:8:8:0",
                        DatabaseUtil.MAX_WAIT_TIME_FOR_CONN_IN_MS);
        users = new DBUserRolesDAOImpl(dataSource);
        groups = new DBGroupRolesDAOImpl(dataSource);
        tokens = new DBTokenRolesDAOImpl(dataSource);
    }

    @AfterAll
    static void closeDataSource() throws SQLException {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void migrationPreservesLegacyGrantsAndStoresDistinctMaximumLengthScopes() throws Exception {
        createLegacyTables();
        Map<String, String> originalCollations = resourceCollations();
        insertGrants(
                "用户",
                "工程组",
                "发布脚本",
                "团队/Release",
                AuthZResource.Type.ENV,
                TeletraanPrincipalRole.ADMIN,
                "legacy-token");

        String prefix = repeated('g', 34) + "/" + repeated('o', 39) + "/" + repeated('r', 99);
        String firstScope = prefix + "a";
        String secondScope = prefix + "b";
        assertEquals(175, firstScope.length());
        String userName = repeated('用', 64);
        String groupName = repeated('组', 64);
        String scriptName = repeated('本', 64);

        // Strict mode exposes the old 64-character limit instead of silently truncating scopes.
        assertThrows(
                SQLException.class,
                () ->
                        insertGrants(
                                userName,
                                groupName,
                                scriptName,
                                firstScope,
                                AuthZResource.Type.SCM_REPOSITORY,
                                TeletraanPrincipalRole.READER,
                                "too-long"));
        try (Connection connection = dataSource.getConnection();
                Reader migration =
                        new InputStreamReader(
                                getClass().getResourceAsStream("/sql/schema-update-25.sql"),
                                StandardCharsets.UTF_8)) {
            new ScriptRunner(connection, false, true).runScript(migration);
        }

        assertEquals(originalCollations, resourceCollations());
        assertGrants(
                "用户",
                "工程组",
                "发布脚本",
                "团队/Release",
                AuthZResource.Type.ENV,
                TeletraanPrincipalRole.ADMIN,
                "legacy-token");
        assertNull(users.getByNameAndResource("用户", "团队/release", AuthZResource.Type.ENV));
        assertNull(groups.getByNameAndResource("工程组", "团队/release", AuthZResource.Type.ENV));
        assertNull(tokens.getByNameAndResource("发布脚本", "团队/release", AuthZResource.Type.ENV));
        insertGrants(
                userName,
                groupName,
                scriptName,
                firstScope,
                AuthZResource.Type.SCM_REPOSITORY,
                TeletraanPrincipalRole.READER,
                "first-token");
        insertGrants(
                userName,
                groupName,
                scriptName,
                secondScope,
                AuthZResource.Type.SCM_REPOSITORY,
                TeletraanPrincipalRole.ADMIN,
                "second-token");
        assertGrants(
                userName,
                groupName,
                scriptName,
                firstScope,
                AuthZResource.Type.SCM_REPOSITORY,
                TeletraanPrincipalRole.READER,
                "first-token");
        assertGrants(
                userName,
                groupName,
                scriptName,
                secondScope,
                AuthZResource.Type.SCM_REPOSITORY,
                TeletraanPrincipalRole.ADMIN,
                "second-token");

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT version FROM schema_versions")) {
                result.next();
                assertEquals(25, result.getInt(1));
            }
            for (String table :
                    Arrays.asList("users_and_roles", "groups_and_roles", "tokens_and_roles")) {
                try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    result.next();
                    assertEquals(3, result.getInt(1), table);
                }
                try (ResultSet result =
                        statement.executeQuery(
                                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"
                                        + table
                                        + "' AND COLUMN_NAME='resource_id'")) {
                    result.next();
                    assertEquals(175, result.getInt(1), table);
                }
                try (ResultSet result =
                        statement.executeQuery(
                                "SELECT COUNT(*), SUM(SUB_PART IS NOT NULL) FROM information_schema.STATISTICS "
                                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"
                                        + table
                                        + "' AND INDEX_NAME='PRIMARY'")) {
                    result.next();
                    assertEquals(3, result.getInt(1), table);
                    assertEquals(0, result.getInt(2), table);
                }
            }
        }
    }

    private static void createLegacyTables() throws SQLException {
        String scopeColumns =
                "resource_id VARCHAR(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL, resource_type VARCHAR(16) NOT NULL, "
                        + "role VARCHAR(22) NOT NULL, ";
        String tableOptions =
                " ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ROW_FORMAT=COMPACT";
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE users_and_roles (user_name VARCHAR(64) NOT NULL, "
                            + scopeColumns
                            + "PRIMARY KEY (resource_id, resource_type, user_name))"
                            + tableOptions);
            statement.execute(
                    "CREATE TABLE groups_and_roles (group_name VARCHAR(64) NOT NULL, "
                            + scopeColumns
                            + "PRIMARY KEY (resource_id, resource_type, group_name))"
                            + tableOptions);
            statement.execute(
                    "CREATE TABLE tokens_and_roles (script_name VARCHAR(64) NOT NULL, "
                            + scopeColumns
                            + "token VARCHAR(22) NOT NULL, expire_date BIGINT NOT NULL, "
                            + "PRIMARY KEY (resource_id, resource_type, script_name))"
                            + tableOptions);
            statement.execute("CREATE TABLE schema_versions (version INT NOT NULL PRIMARY KEY)");
            statement.execute("INSERT INTO schema_versions VALUES (24)");
        }
    }

    private static Map<String, String> resourceCollations() throws SQLException {
        Map<String, String> collations = new TreeMap<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT TABLE_NAME, CHARACTER_SET_NAME, COLLATION_NAME FROM information_schema.COLUMNS "
                                        + "WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='resource_id'")) {
            while (result.next()) {
                collations.put(
                        result.getString(1), result.getString(2) + "/" + result.getString(3));
            }
        }
        assertEquals(3, collations.size());
        return collations;
    }

    private static void insertGrants(
            String userName,
            String groupName,
            String scriptName,
            String scope,
            AuthZResource.Type type,
            TeletraanPrincipalRole role,
            String token)
            throws Exception {
        UserRolesBean user = new UserRolesBean();
        user.setUser_name(userName);
        user.setResource_id(scope);
        user.setResource_type(type);
        user.setRole(role);
        users.insert(user);
        GroupRolesBean group = new GroupRolesBean();
        group.setGroup_name(groupName);
        group.setResource_id(scope);
        group.setResource_type(type);
        group.setRole(role);
        groups.insert(group);
        TokenRolesBean script = new TokenRolesBean();
        script.setScript_name(scriptName);
        script.setResource_id(scope);
        script.setResource_type(type);
        script.setRole(role);
        script.setToken(token);
        script.setExpire_date(Long.MAX_VALUE);
        tokens.insert(script);
    }

    private static void assertGrants(
            String userName,
            String groupName,
            String scriptName,
            String scope,
            AuthZResource.Type type,
            TeletraanPrincipalRole role,
            String token)
            throws Exception {
        UserRolesBean user = users.getByNameAndResource(userName, scope, type);
        assertNotNull(user);
        assertEquals(userName, user.getUser_name());
        assertEquals(scope, user.getResource_id());
        assertEquals(role, user.getRole());
        GroupRolesBean group = groups.getByNameAndResource(groupName, scope, type);
        assertNotNull(group);
        assertEquals(groupName, group.getGroup_name());
        assertEquals(scope, group.getResource_id());
        assertEquals(role, group.getRole());
        TokenRolesBean script = tokens.getByNameAndResource(scriptName, scope, type);
        assertNotNull(script);
        assertEquals(scriptName, script.getScript_name());
        assertEquals(scope, script.getResource_id());
        assertEquals(role, script.getRole());
        assertEquals(scope, tokens.getByToken(token).getResource_id());
        assertEquals(token, tokens.getByToken(token).getToken());
    }

    private static String repeated(char value, int length) {
        char[] characters = new char[length];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
