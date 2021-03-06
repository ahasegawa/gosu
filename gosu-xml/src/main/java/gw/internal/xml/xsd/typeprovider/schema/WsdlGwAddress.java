/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.xml.xsd.typeprovider.schema;

import gw.internal.xml.xsd.typeprovider.XmlSchemaIndex;
import gw.lang.reflect.LocationInfo;

public final class WsdlGwAddress extends XmlSchemaObject<WsdlGwAddress> {

  private final String _location;

  public WsdlGwAddress( XmlSchemaIndex schemaIndex, LocationInfo locationInfo, String location ) {
    super( schemaIndex, locationInfo );
    _location = location;
  }

  public String getLocation() {
    return _location;
  }

}
