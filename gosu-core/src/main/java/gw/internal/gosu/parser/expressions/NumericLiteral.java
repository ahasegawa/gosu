/*
 * Copyright 2013 Guidewire Software, Inc.
 */
package gw.internal.gosu.parser.expressions;

import gw.internal.gosu.parser.StringCache;
import gw.lang.parser.expressions.INumericLiteralExpression;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.TypeSystemShutdownListener;
import gw.lang.reflect.java.JavaTypes;
import gw.util.concurrent.LockingLazyVar;

/**
 * An expression representing number literals as defined in the Gosu grammar.
 */
public final class NumericLiteral extends Literal implements INumericLiteralExpression
{
  public static final LockingLazyVar<NumericLiteral> NaN = new LockingLazyVar<NumericLiteral>() {
    protected NumericLiteral init() {
      return new NumericLiteral( Double.NaN + "", Double.NaN, JavaTypes.pDOUBLE() );
    }
  };
  public static final LockingLazyVar<NumericLiteral> INFINITY = new LockingLazyVar<NumericLiteral>() {
    protected NumericLiteral init() {
      return new NumericLiteral( Double.POSITIVE_INFINITY + "", Double.POSITIVE_INFINITY, JavaTypes.pDOUBLE() );
    }
  };
  static {
    TypeSystem.addShutdownListener(new TypeSystemShutdownListener() {
      public void shutdown() {
        INFINITY.clear();
        NaN.clear();
      }
    });
  }

  /**
   * The value of the numberic lister as a Double.
   */
  protected Number _value;
  private String _strValue;
  private boolean _explicitlyTyped;

  public NumericLiteral( String strValue, Number value, IType type )
  {
    _strValue = StringCache.get(strValue);
    _value = value;
    setType( type );
  }

  @SuppressWarnings({"CloneDoesntCallSuperClone"})
  @Override
  public Object clone()
  {
    return new NumericLiteral( _strValue, _value, getType() );
  }

  public NumericLiteral copy()
  {
    return (NumericLiteral)clone();
  }

  public Number getValue()
  {
    return _value;
  }

  public boolean isCompileTimeConstant()
  {
    return getType().isPrimitive();
  }

  public Object evaluate()
  {
    return getValue();
  }

  @Override
  public String toString()
  {
    return String.valueOf( getValue() );
  }

  public String getStrValue()
  {
    return _strValue;
  }

  public void setValue(Number dvalue) {
    _value = dvalue;
  }

  public void setExplicitlyTyped( boolean b )
  {
    _explicitlyTyped = b;
  }

  public boolean isExplicitlyTyped()
  {
    return _explicitlyTyped;
  }
}
