/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.reflect.java;

import java.io.Serializable;

public interface IJavaClassAnnotation extends Serializable {
  String annotationTypeName();

  Object getValue(String name);
}