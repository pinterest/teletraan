package com.pinterest.deployservice.knox;

/**
 * Knox is an interface for Java services to interface with the Knox service.
 */
public interface Knox {

  /**
   * getPrimaryKey returns the primary key for the specified keyID
   */
  public byte[] getPrimaryKey() throws Exception;

  /**
   * getActiveKeys returns a list of active keys for the specified keyID
   */
  public byte[][] getActiveKeys() throws Exception;

}
