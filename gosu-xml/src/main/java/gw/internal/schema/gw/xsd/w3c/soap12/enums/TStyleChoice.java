package gw.internal.schema.gw.xsd.w3c.soap12.enums;

/***************************************************************************/
/* THIS IS AUTOGENERATED CODE - DO NOT MODIFY OR YOUR CHANGES WILL BE LOST */
/* THIS CODE CAN BE REGENERATED USING 'xsd-codegen'                        */
/***************************************************************************/
public enum TStyleChoice implements gw.lang.reflect.gs.IGosuObject, gw.lang.reflect.IEnumValue, gw.xml.IXmlSchemaEnumValue, gw.internal.xml.IXmlGeneratedClass {
  Rpc( "rpc" ), Document( "document" );

  public static final gw.util.concurrent.LockingLazyVar<gw.lang.reflect.IType> TYPE = new gw.util.concurrent.LockingLazyVar<gw.lang.reflect.IType>( gw.lang.reflect.TypeSystem.getGlobalLock() ) {
          @Override
          protected gw.lang.reflect.IType init() {
            return gw.lang.reflect.TypeSystem.getByFullName( "gw.xsd.w3c.soap12.enums.TStyleChoice" );
          }
        };

  private final java.lang.String _serializedValue;

  private TStyleChoice( java.lang.String serializedValue ) {
    _serializedValue = serializedValue;
  }

  @Override
  public gw.lang.reflect.IType getIntrinsicType() {
    return TYPE.get();
  }

  @Override
  public java.lang.String toString() {
    return _serializedValue;
  }

  @Override
  public java.lang.Object getValue() {
    return this;
  }

  @Override
  public java.lang.String getCode() {
    return name();
  }

  @Override
  public int getOrdinal() {
    return ordinal();
  }

  @Override
  public java.lang.String getDisplayName() {
    return name();
  }


  public java.lang.String getGosuValue() {
    return (java.lang.String) TYPE.get().getTypeInfo().getProperty( "GosuValue" ).getAccessor().getValue( this );
  }


  public java.lang.String getSerializedValue() {
    return (java.lang.String) TYPE.get().getTypeInfo().getProperty( "SerializedValue" ).getAccessor().getValue( this );
  }

  @SuppressWarnings( {"UnusedDeclaration"} )
  private static final long FINGERPRINT = 2359489450031053463L;

}