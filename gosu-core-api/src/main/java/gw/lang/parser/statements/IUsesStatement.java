/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.parser.statements;

import gw.lang.parser.IStatement;

public interface IUsesStatement extends IStatement
{
  String getTypeName();
}
