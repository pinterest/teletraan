package com.pinterest.deployservice.knox;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * CommandLineKnox uses knox command line calls to interface with Knox.
 */
public class CommandLineKnox implements Knox {
  private final String keyId;
  private final Runtime runtime;
  private final String hostname;

  public CommandLineKnox(String keyId, Runtime runtime) {
    this.keyId = keyId;
    this.runtime = runtime;
    String host;
    try {
      host = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      // This should never happen, perhaps we should log something here.
      host = "";
    }
    this.hostname = host;
  }

  /**
   * register registers the key with Knox
   */
  public int register() throws Exception {
    String[] args = {"knox", "register", "-k", this.keyId};
    String[] envp = {"KNOX_MACHINE_AUTH=" + this.hostname};
    Process process = runtime.exec(args, envp);
    return process.waitFor();
  }

  /**
   * getPrimaryKey returns the primary key for the specified keyID
   */
  public byte[] getPrimaryKey() throws Exception {
    String[] args = {"knox", "get", "-j", ""};
    args[3] = this.keyId;
    String[] envp = new String[1];
    envp[0] = "KNOX_MACHINE_AUTH=" + this.hostname;
    Process process = runtime.exec(args, envp);
    return KnoxJsonDecoder.getPrimaryKey(process.getInputStream());
  }

  /**
   * getActiveKeys returns a list of active keys for the specified keyID
   */
  public byte[][] getActiveKeys() throws Exception {
    String[] args = {"knox", "get", "-j", ""};
    args[3] = this.keyId;
    String[] envp = new String[1];
    String hostname = InetAddress.getLocalHost().getHostName();
    envp[0] = "KNOX_MACHINE_AUTH=" + hostname;
    Process process = runtime.exec(args, envp);
    return KnoxJsonDecoder.getActiveKeys(process.getInputStream());
  }

}
