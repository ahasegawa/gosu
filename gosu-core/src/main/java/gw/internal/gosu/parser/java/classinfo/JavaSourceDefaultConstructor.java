/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser.java.classinfo;

import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IFeatureInfo;
import gw.lang.reflect.IParameterInfo;
import gw.lang.reflect.java.IJavaClassConstructor;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.module.IModule;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class JavaSourceDefaultConstructor implements IJavaClassConstructor {
  private IJavaClassInfo _classInfo;

  public JavaSourceDefaultConstructor(IJavaClassInfo classInfo) {
    _classInfo = classInfo;
  }

  @Override
  public IParameterInfo[] convertGenericParameterTypes(IFeatureInfo container, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars) {
    return new IParameterInfo[0];
  }

  @Override
  public IJavaClassInfo getEnclosingClass() {
    return _classInfo;
  }

  @Override
  public IJavaClassInfo[] getExceptionTypes() {
    return new IJavaClassInfo[0];
  }

  @Override
  public int getModifiers() {
    return Modifier.PUBLIC;
  }

  @Override
  public IJavaClassInfo[] getParameterTypes() {
    return new IJavaClassInfo[0];
  }

  @Override
  public IAnnotationInfo getAnnotation(Class annotationClass) {
    return null;
  }

  @Override
  public IAnnotationInfo[] getDeclaredAnnotations() {
    return new IAnnotationInfo[0];
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return false;
  }

  @Override
  public Object newInstance(Object... objects) throws InvocationTargetException, IllegalAccessException, InstantiationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDefault() {
    return true;
  }

}
