/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu;

import gw.config.BaseService;
import gw.config.IGosuLocalizationService;
import gw.lang.parser.resources.ResourceKey;
import gw.lang.parser.resources.Res;

public class DefaultLocalizationService extends BaseService implements IGosuLocalizationService
{
  public String localize(ResourceKey key, Object... args) {
    return Res.get(key, args);
  }

  public boolean exists(ResourceKey key) {
    return Res.exists( key );
  }
}
