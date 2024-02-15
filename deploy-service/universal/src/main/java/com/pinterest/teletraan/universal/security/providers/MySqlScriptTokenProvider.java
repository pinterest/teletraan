package com.pinterest.teletraan.universal.security.providers;


import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.teletraan.universal.security.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TokenRolesBean;

@Deprecated
public class MySqlScriptTokenProvider {
  private static final Logger LOG = LoggerFactory.getLogger(MySqlScriptTokenProvider.class);

  private static final String GET_BY_TOKEN =
      "SELECT * FROM tokens_and_roles WHERE token = ? AND expire_date>?";
  private static final String INSERT_TOKEN =
      "INSERT INTO tokens_and_roles"
          + "(script_name, resource_id, token , roles, expire_date, group_name)"
          + " VALUES(?,?,?,?,?,?)";

  private BasicDataSource dataSource;

  public MySqlScriptTokenProvider(BasicDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public ServicePrincipal getScriptTokenPrincipal(String token) {
    try {
      List<TokenRolesBean> roles = getTokenRoles(token);
      if (roles != null && roles.size() > 0) {
        return new ServicePrincipal(
            roles.get(0).getScript_name(), roles, roles.get(0).getGroup_name());
      }
    } catch (Exception e) {
      LOG.error("failed to get Script token principal", e);
    }
    return null;
  }

  public List<TokenRolesBean> getTokenRoles(String token) throws Exception {
    ResultSetHandler<List<TokenRolesBean>> h =
        new BeanListHandler<TokenRolesBean>(TokenRolesBean.class);
    return new QueryRunner(dataSource).query(GET_BY_TOKEN, h, token, System.currentTimeMillis());
  }

  public void createToken(String scriptName, String resource_id, long roleMask, String group)
      throws Exception {
    SecureRandom random = new SecureRandom();
    random.setSeed(System.currentTimeMillis());
    byte bytes[] = new byte[24];
    random.nextBytes(bytes);
    this.createToken(
        scriptName, resource_id, roleMask, Base64.getEncoder().encodeToString(bytes), group);
  }

  public void createToken(
      String scriptName, String resource_id, long roleMask, String token, String group)
      throws Exception {
    new QueryRunner(dataSource)
        .update(
            INSERT_TOKEN,
            scriptName,
            resource_id,
            token,
            roleMask,
            DateTime.now().plusYears(1).toDate().getTime(),
            group);
  }
}
