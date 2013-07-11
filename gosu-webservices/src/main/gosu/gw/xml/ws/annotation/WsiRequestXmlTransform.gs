/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.xml.ws.annotation

uses gw.lang.annotation.AnnotationUsage
uses gw.lang.annotation.UsageTarget
uses gw.lang.annotation.UsageModifier
uses gw.xml.XmlElement

/**
 * Allows high-level transformation of the incoming request prior to handling.
 */
@AnnotationUsage( UsageTarget.TypeTarget, UsageModifier.One )
class WsiRequestXmlTransform implements IAnnotation {

  var _transform( envelope : XmlElement ) 

  construct( xform( envelope : XmlElement ) ) {
    _transform = xform
  }
  
  property get Transform() : block( envelope : XmlElement ) {
    return _transform
  }

}