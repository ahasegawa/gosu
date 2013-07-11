/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.reflect.module;

import gw.fs.IDirectory;
import gw.lang.reflect.gs.TypeName;
import gw.util.fingerprint.FP64;
import gw.lang.UnstableAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

@UnstableAPI
public interface IClassPath
{
  String GW_API_PREFIX = "gw.";
  String GW_INTERNAL_PREFIX = "gw.internal.";
  String SUN_CLASS_PREFIX = "sun.";
  String COM_SUN_CLASS_PREFIX = "com.sun.";

  String PLACEHOLDER_FOR_PACKAGE = "PLACEHOLDER";

  ClassPathFilter ALLOW_ALL_FILTER =
    new ClassPathFilter()
    {
      public boolean acceptClass( String className )
      {
        // Do not expose Sun's classes. We shouldn't encourage their use and some
        // of them behave badly during static initialization, which interferes
        // with the typeinfo database.
        return !className.startsWith( SUN_CLASS_PREFIX ) &&
               !className.startsWith( COM_SUN_CLASS_PREFIX );
      }
    };

  ClassPathFilter ONLY_API_CLASSES =
    new ClassPathFilter()
    {
      public boolean acceptClass( String className )
      {
        return className.startsWith( GW_API_PREFIX ) && !className.startsWith(GW_INTERNAL_PREFIX);
      }
    };

  ClassPathFilter ALLOW_ALL_WITH_SUN_FILTER =
    new IClassPath.ClassPathFilter() {
      public boolean acceptClass( String className ) {
        return true;
      }
    };

  ArrayList<IDirectory> getPaths();

  Set<String> getFilteredClassNames();

  Set<TypeName> getTypeNames(String namespace);

  public static interface ClassPathFilter
  {
    public boolean acceptClass( String className );
  }
}
