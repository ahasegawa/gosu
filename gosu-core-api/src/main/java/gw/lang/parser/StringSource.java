/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.parser;

public class StringSource implements ISource {
  private String _strSource;

  public StringSource(String strSource) {
    _strSource = strSource;
  }

  public String getSource() {
    return _strSource;
  }

}
