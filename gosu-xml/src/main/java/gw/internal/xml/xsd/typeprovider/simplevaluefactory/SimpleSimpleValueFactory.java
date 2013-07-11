/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.xml.xsd.typeprovider.simplevaluefactory;

import gw.internal.xml.XmlDeserializationContext;
import gw.internal.xml.XmlSerializationContext;
import gw.internal.xml.XmlSimpleValueBase;
import gw.lang.reflect.IType;
import gw.lang.reflect.java.IJavaType;
import gw.lang.reflect.java.JavaTypes;
import gw.lang.reflect.java.JavaTypes;
import gw.xml.XmlSimpleValue;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

public class SimpleSimpleValueFactory extends XmlSimpleValueFactory {

  private IType _gosuValueType;

  public SimpleSimpleValueFactory( IType gosuValueType ) {
    _gosuValueType = gosuValueType;
  }

  @Override
  public IType getGosuValueType() {
    return _gosuValueType;
  }

  @Override
  protected XmlSimpleValue _gosuValueToStorageValue( Object gosuValue ) {
    return new Value( gosuValue );
  }

  @Override
  protected XmlSimpleValue _deserialize( XmlDeserializationContext context, String stringValue, boolean isDefault ) {
    return new Value( getGosuValueType().getTypeInfo().getConstructor( JavaTypes.STRING() ).getConstructor().newInstance( stringValue ) );
  }

  private class Value extends XmlSimpleValueBase {

    private final Object _gosuValue;

    public Value( Object gosuValue ) {
      _gosuValue = gosuValue;
    }

    @Override
    public IType getGosuValueType() {
      return SimpleSimpleValueFactory.this.getGosuValueType();
    }

    @Override
    public Object _getGosuValue() {
      return _gosuValue;
    }

    public String _getStringValue( boolean isEnumCode ) {
      return String.valueOf( _gosuValue );
    }

    @Override
    public String _serialize( XmlSerializationContext context ) {
      return String.valueOf( _gosuValue );
    }

    @Override
    public List<QName> _getQNames() {
      return Collections.emptyList();
    }

  }

}
