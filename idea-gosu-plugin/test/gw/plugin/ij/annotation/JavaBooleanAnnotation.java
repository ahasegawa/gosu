/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention( RetentionPolicy.RUNTIME )
public @interface JavaBooleanAnnotation {
  boolean value();
}
