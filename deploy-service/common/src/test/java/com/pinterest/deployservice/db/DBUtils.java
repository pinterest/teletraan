package com.pinterest.deployservice.db;

import com.ibatis.common.jdbc.ScriptRunner;
import org.apache.commons.dbcp.BasicDataSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;


public class DBUtils {
  public static BasicDataSource runMigrations(BasicDataSource dataSource) throws Exception {
    Connection conn = dataSource.getConnection();
    ScriptRunner runner = new ScriptRunner(conn, false, true);
    runner.runScript(
          new BufferedReader(
              new InputStreamReader(DBUtils.class.getResourceAsStream("/sql/cleanup.sql"))));
    runner.runScript(
          new BufferedReader(
             new InputStreamReader(DBUtils.class.getResourceAsStream("/sql/deploy.sql"))));
    conn.prepareStatement("SET sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''));").execute();
    conn.close();
    return dataSource;
  }
}
