/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.parser;

import gw.lang.reflect.gs.IFileSystemGosuClassRepository;

public class FileSource implements ISource {
  private IFileSystemGosuClassRepository.IClassFileInfo _file;

  public FileSource(IFileSystemGosuClassRepository.IClassFileInfo file) {
    _file = file;
  }

  public String getSource() {
    return _file.getContent();
  }

}
