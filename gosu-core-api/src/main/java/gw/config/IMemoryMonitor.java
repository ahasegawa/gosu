/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.config;

import gw.lang.reflect.RefreshRequest;

public interface IMemoryMonitor extends IService {

  void reclaimMemory(RefreshRequest request);

}
