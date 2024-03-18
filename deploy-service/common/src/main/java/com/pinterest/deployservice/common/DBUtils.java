package com.pinterest.deployservice.common;

import com.ibatis.common.jdbc.ScriptRunner;
import com.pinterest.deployservice.db.DatabaseUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class DBUtils {
  private static final String DEFAULT_DB_NAME = "deploy";
  private static final int DEFAULT_PORT = 3303;
  private static final String CONTAINER_NAME = "local-mysql8";
  private static final String IMAGE_NAME = "mysql:8-oracle";
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseUtil.class);
  private static final int MAX_RETRY = 5;
  private static final int START_MYSQL_MAX_RETRY = 60;

  public static BasicDataSource setupDataSource() throws Exception {
    boolean createDBInstance = !Boolean.parseBoolean(System.getenv("USE_LOCAL_MYSQL_INSTANCE"));
    return DBUtils.setupDataSource(DEFAULT_PORT, createDBInstance);
  }

  public static void startMysqlContainer(int port) throws Exception {
    String runDocker =
        String.format(
            "docker run --rm -d -p %d:%d -e MYSQL_TCP_PORT=%d -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e"
                + " MYSQL_DATABASE=%s --name %s %s",
            port, port, port, DEFAULT_DB_NAME, CONTAINER_NAME, IMAGE_NAME);

    LOG.info("run docker command: " + runDocker);

    Process process = Runtime.getRuntime().exec(runDocker);
    process.waitFor();

    // wait for mysql db is ready
    int counter = 0;
    String logs = null;
    while (true) {
      if (counter == START_MYSQL_MAX_RETRY) {
        throw new DatabaseStartException(logs);
      }
      Thread.sleep(1000);
      runDocker = String.format("docker exec %s mysql -u root deploy", CONTAINER_NAME);
      process = Runtime.getRuntime().exec(runDocker);
      int exitValue = process.waitFor();
      if (exitValue == 0) {
        break;
      }

      logs = getLogs(process.getErrorStream());
      LOG.info(
          "wait for mysql ready: command = {}, exitValue = {}, logs = {}",
          runDocker,
          exitValue,
          logs);
      counter++;
    }
  }

  public static void stopMysqlContainer() throws Exception {
    String runDocker = String.format("docker stop %s", CONTAINER_NAME);

    LOG.info("run docker command: " + runDocker);
    Process process = Runtime.getRuntime().exec(runDocker);
    process.waitFor();
  }

  public static BasicDataSource setupDataSource(String baseDir, int port) throws Exception {
    return DBUtils.setupDataSource(port, true);
  }

  public static BasicDataSource setupDataSource(int port, boolean createDBInstance)
      throws Exception {
    if (createDBInstance) {
      try {
        // making sure we do not have anything running
        stopMysqlContainer();
      } catch (Exception e) {
        // ignore
      }

      // start the docker container
      startMysqlContainer(port);
    }

    // use local mysql container for unit testing because the MXJ embedded db only support mysql5 in
    // AMD.
    // mysql docker container opens up our test db to be mysql8+, and different arch like ARM64
    BasicDataSource dataSource = DatabaseUtil.createLocalDataSource(DEFAULT_DB_NAME, port);

    Connection conn = getConnectionWithRetry(dataSource);
    ScriptRunner runner = new ScriptRunner(conn, false, true);
    runner.runScript(
        new BufferedReader(
            new InputStreamReader(DBUtils.class.getResourceAsStream("/sql/cleanup.sql"))));
    runner.runScript(
        new BufferedReader(
            new InputStreamReader(DBUtils.class.getResourceAsStream("/sql/deploy.sql"))));

    //runSchemaUpdateScripts(runner);

    conn.close();
    return dataSource;
  }

  public static Connection getConnectionWithRetry(BasicDataSource dataSource)
      throws SQLException, InterruptedException {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try {
        return dataSource.getConnection();
      } catch (SQLException ex) {
        retryCount++;
        LOG.warn("Failed to get connection after {} attempt(s)", retryCount);
        if (retryCount == MAX_RETRY) {
          throw ex;
        }
        TimeUnit.MILLISECONDS.sleep(1000 * retryCount);
      }
    }
    return null;
  }

  public static void tearDownDataSource() throws Exception {
    if (!Boolean.parseBoolean(System.getenv("USE_LOCAL_MYSQL_INSTANCE"))) {
      stopMysqlContainer();
    }
  }

  public static void truncateAllTables(BasicDataSource dataSource) throws Exception {
    try (Connection conn = dataSource.getConnection();
        Statement query = conn.createStatement();
        Statement stmt = conn.createStatement(); ) {
      ResultSet rs =
          query.executeQuery(
              "SELECT table_name FROM information_schema.tables WHERE table_schema = SCHEMA()");
      stmt.addBatch("SET FOREIGN_KEY_CHECKS=0");
      while (rs.next()) {
        String sqlStatement = String.format("TRUNCATE `%s`", rs.getString(1));
        stmt.addBatch(sqlStatement);
      }
      stmt.addBatch("SET FOREIGN_KEY_CHECKS=1");
      stmt.executeBatch();
      LOG.info(String.format("Truncated all tables"));
    }
  }

  private static void runSchemaUpdateScripts(ScriptRunner runner) {
    int version = 22;
    int executed = 0;
    while (true) {
      String scriptName = String.format("/sql/tools/mysql/schema-update-%d.sql", version);
      try {
        runner.runScript(new InputStreamReader(DBUtils.class.getResourceAsStream(scriptName)));
        LOG.info(String.format("Applied script: %s", scriptName));
      } catch (Exception e) {
        if (executed == 0) {
          throw new Error(
              "Could not run a single update script. Check starting version is correct");
        }
        break;
      }
      version++;
      executed++;
    }
  }

  private static String getLogs(InputStream in) {
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    StringBuilder sb = new StringBuilder();

    while (true) {
      try {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        sb.append(line);
      } catch (Exception e) {
        throw new LogReadException(e);
      }
    }
    return sb.toString();
  }

  private static class DatabaseStartException extends RuntimeException {

    DatabaseStartException(String logs) {
      super("Can't connect to MySQL database: " + logs);
    }
  }

  private static class LogReadException extends RuntimeException {

    LogReadException(Exception e) {
      super("Can't read logs", e);
    }
  }
}
