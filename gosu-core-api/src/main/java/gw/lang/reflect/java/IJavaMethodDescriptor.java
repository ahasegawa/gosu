/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.reflect.java;

import gw.lang.reflect.IScriptabilityModifier;

import java.io.Serializable;

public interface IJavaMethodDescriptor extends Serializable {
  IJavaClassMethod getMethod();

  String getName();

  boolean isHiddenViaFeatureDescriptor();

  boolean isVisibleViaFeatureDescriptor(IScriptabilityModifier constraint);
}