/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention( RetentionPolicy.RUNTIME )
public @interface JavaStringAndStringArrayAnnotation {
  @NotNull String[] foo();
  @NotNull String bar();
}
