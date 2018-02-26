package com.pinterest.deployservice.common;

import com.pinterest.deployservice.knox.CommandLineKnox;
import com.pinterest.deployservice.knox.FileSystemKnox;
import com.pinterest.deployservice.knox.Knox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class KnoxDBKeyReader {

    private static final Logger LOG = LoggerFactory.getLogger(KnoxDBKeyReader.class);

    private static Knox mKnox;

    // initialize to credentials in test env
    private static String testUserName = "root";
    private static String testPassword = "";

    public static void init(String roleString) {
        try {
           LOG.info("Init the db key with role {}", roleString);
            String mysqlKeys = String.format("mysql:rbac:%s:credentials", roleString);
            // Register key
            File file = new File("/var/lib/knox/v0/keys/" + mysqlKeys);
            if (!file.exists()) {
                CommandLineKnox cmdKnox = new CommandLineKnox(mysqlKeys, Runtime.getRuntime());

                    if (cmdKnox.register() != 0) {
                        throw new RuntimeException("Error registering mysql keys: " + mysqlKeys);
                    }

                long startTime = System.currentTimeMillis();
                while (!file.exists() && System.currentTimeMillis() - startTime < 5000) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            mKnox = new FileSystemKnox(mysqlKeys);
        } catch (Exception e) {
            LOG.error("Using default credentials due to exception :" + e.getMessage());
        }
        // Note that default credentials will work in test environment but will later cause
        // another exception in prod environments due to wrong credentials.
    }


    /**
     * Get db username
     * @return The db username
     */
    public static String getUserName() {
        if (mKnox == null) {
            LOG.error("Using default username since mKnox is null");
            return testUserName;
        }
        try {
            String userSpec;
            String mysqlCredsPriString = new String(mKnox.getPrimaryKey(), "UTF-8");
            userSpec = mysqlCredsPriString.split("\\|")[0];
            String userName = userSpec.split("\\@")[0];
            return userName;
        } catch (Exception e) {
            LOG.error("Using default username due to exception :" + e.getMessage());
            return testUserName;
        }
    }

    /**
     * Get db password
     * @return The db password
     */
    public static String getPassword() {
        if (mKnox == null) {
            LOG.error("Using default password since mKnox is null");
            return testPassword;
        }
        try {
            String mysqlCredsPriString = new String(mKnox.getPrimaryKey(), "UTF-8");
            String password = mysqlCredsPriString.split("\\|")[1];
            return password;
        } catch (Exception e) {
            LOG.error("Using default password due to exception :" + e.getMessage());
            return testPassword;
        }
    }
}
