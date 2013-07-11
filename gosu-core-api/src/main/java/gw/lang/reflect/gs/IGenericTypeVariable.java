/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.reflect.gs;

import gw.lang.parser.expressions.ITypeVariableDefinition;
import gw.lang.reflect.IType;

public interface IGenericTypeVariable
{
  String getName();

  String getNameWithBounds( boolean bRelative );

  ITypeVariableDefinition getTypeVariableDefinition();

  IType getBoundingType();

  IGenericTypeVariable clone();

  void createTypeVariableDefinition(IType enclosingType);
}
