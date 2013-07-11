/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser.java.classinfo;

import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.IType;
import gw.lang.reflect.java.IJavaClassGenericArrayType;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.java.IJavaClassType;
import gw.lang.reflect.module.IModule;

public class JavaSourceArrayType implements IJavaClassGenericArrayType {
  private IJavaClassType _componentType;

  public JavaSourceArrayType(IJavaClassType componentType) {
    _componentType = componentType;
  }

  @Override
  public IJavaClassType getGenericComponentType() {
    return _componentType;
  }

  @Override
  public IType getActualType(TypeVarToTypeMap typeMap) {
    return getGenericComponentType().getActualType(typeMap).getArrayType();
  }

  @Override
  public IType getActualType(TypeVarToTypeMap typeMap, boolean bKeepTypeVars) {
    return getGenericComponentType().getActualType(typeMap, bKeepTypeVars).getArrayType();
  }

  @Override
  public IJavaClassType getConcreteType() {
    return new JavaSourceArrayClassInfo((IJavaClassInfo) _componentType.getConcreteType());
  }

  @Override
  public String getName() {
    return _componentType.getName() + "[]";
  }

  @Override
  public String getSimpleName() {
    return getName();
  }

  @Override
  public IModule getModule() {
    return _componentType.getModule();
  }

  @Override
  public String getNamespace() {
    return _componentType.getNamespace();
  }

  public String toString() {
    return _componentType.getName() + "[]";
  }
}
