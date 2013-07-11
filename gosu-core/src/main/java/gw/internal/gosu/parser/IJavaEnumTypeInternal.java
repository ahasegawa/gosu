/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.lang.reflect.IEnumType;
import gw.lang.reflect.IType;

/**
 */
public interface IJavaEnumTypeInternal extends IJavaTypeInternal, IEnumType
{
  IType getEnumType();
}
