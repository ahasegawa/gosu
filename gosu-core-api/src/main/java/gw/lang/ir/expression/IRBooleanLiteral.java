/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.ir.expression;

import gw.lang.ir.IRExpression;
import gw.lang.ir.IRType;
import gw.lang.ir.IRTypeConstants;
import gw.lang.UnstableAPI;

@UnstableAPI
public class IRBooleanLiteral extends IRExpression {
  private boolean _value;

  public IRBooleanLiteral(boolean value) {
    _value = value;
  }

  public boolean getValue() {
    return _value;
  }

  @Override
  public IRType getType() {
    return IRTypeConstants.pBOOLEAN();
  }
}
