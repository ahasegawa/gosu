/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.util;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public class GosuBundle {
  @NonNls
  private static final String BUNDLE = "messages.Gosu";

  @NotNull
  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return CommonBundle.message(ResourceBundle.getBundle(BUNDLE), key, params);
  }
}
