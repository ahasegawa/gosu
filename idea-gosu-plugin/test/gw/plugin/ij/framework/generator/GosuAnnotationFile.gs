/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.framework.generator

class GosuAnnotationFile extends GosuTestingResource {

  construct(classContent: String) {
    super(getFileName(classContent), classContent)
  }

    static function isGosuAnnotation(text: String): boolean {
    text = removeMarkers(text)
    return text.contains("package") &&  text.contains("class") && text.contains("extends") && text.contains("IAnnotation")
  }

    static function getFileName(text: String): String {
    text = removeMarkers(text)
    var i1 = text.indexOf("package ") + 8
    var i2 = wordEnd(text, i1)
    var pkg = text.substring(i1, i2).trim()
    i1 = text.indexOf("class ") + 5
    while (text.charAt(i1) == ' ') i1++
    i2 = wordEnd(text, i1)
    var cls = text.substring(i1, i2).trim()
    return pkg.replace('.', '/') + "/" + cls + ".java"
  }

}