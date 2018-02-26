package com.pinterest.deployservice.knox;

import java.io.File;

/**
 * FileSystemKnox uses filesystem reads and writes to interface with Knox.
 */
public class FileSystemKnox implements Knox {
  private final String keyId;
  public static final String KEYS_DIR = "/var/lib/knox/v0/keys/";

  public FileSystemKnox(String keyId) {
    this.keyId = keyId;
  }

  /**
   * getPrimaryKey returns the primary key for the specified keyID
   */
  public byte[] getPrimaryKey() throws Exception {
    File f = new File(this.KEYS_DIR, this.keyId);
    return KnoxJsonDecoder.getPrimaryKey(f);
  }

  /**
   * getActiveKeys returns a list of active keys for the specified keyID
   */
  public byte[][] getActiveKeys() throws Exception {
    File f = new File(this.KEYS_DIR, this.keyId);
    return KnoxJsonDecoder.getActiveKeys(f);
  }

}
