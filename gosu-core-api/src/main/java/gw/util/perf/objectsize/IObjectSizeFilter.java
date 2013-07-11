/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.util.perf.objectsize;

import java.lang.reflect.Field;

public interface IObjectSizeFilter {
    public boolean skipField(Field field);
    public boolean skipObject(Object obj);
}
