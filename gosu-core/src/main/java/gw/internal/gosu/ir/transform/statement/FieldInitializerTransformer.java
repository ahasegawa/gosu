/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.transform.statement;

import gw.internal.gosu.parser.ScopedDynamicSymbol;
import gw.internal.gosu.ir.transform.ExpressionTransformer;
import gw.internal.gosu.ir.transform.TopLevelTransformationContext;
import gw.internal.gosu.ir.transform.util.AccessibilityUtil;
import gw.lang.ir.IRStatement;
import gw.lang.ir.IRExpression;
import gw.lang.ir.expression.IRNotExpression;
import gw.lang.ir.statement.IRNoOpStatement;
import gw.lang.ir.statement.IRIfStatement;
import gw.lang.parser.IExpression;
import gw.lang.parser.ISymbol;
import gw.lang.parser.IAttributeSource;
import gw.lang.parser.expressions.IVarStatement;

/**
 */
public class FieldInitializerTransformer extends AbstractStatementTransformer<IVarStatement>
{
  public static IRStatement compile( TopLevelTransformationContext cc, IVarStatement stmt )
  {
    FieldInitializerTransformer gen = new FieldInitializerTransformer( cc, stmt );
    return gen.compile();
  }

  private FieldInitializerTransformer( TopLevelTransformationContext cc, IVarStatement stmt )
  {
    super( cc, stmt );
  }

  @Override
  protected IRStatement compile_impl()
  {
    IExpression asExp = _stmt().getAsExpression();
    ISymbol symbol = _stmt().getSymbol();
    if( asExp != null )
    {
      if( symbol instanceof ScopedDynamicSymbol )
      {
        ScopedDynamicSymbol dynamicSymbol = (ScopedDynamicSymbol)symbol;
        IRExpression hasAttribute = callMethod( IAttributeSource.class, "hasAttribute", new Class[]{String.class},
                pushAttributeSource( dynamicSymbol ),
                exprList( pushConstant( dynamicSymbol.getAttributeName() ) ) );

        IRExpression value = boxValue( asExp.getType(), ExpressionTransformer.compile( asExp, _cc() ) );
        IRExpression setAttribute = callMethod( IAttributeSource.class, "setAttribute", new Class[]{String.class, Object.class},
                pushAttributeSource( dynamicSymbol ),
                exprList( pushConstant( dynamicSymbol.getAttributeName() ), value) );

        return new IRIfStatement( new IRNotExpression( hasAttribute ), buildMethodCall( setAttribute ), null);
      }
      else
      {
        if( _stmt().isStatic() )
        {
          return setStaticField( getGosuClass(), symbol.getName(), getDescriptor( symbol.getType() ), AccessibilityUtil.forSymbol( symbol ),
                  ExpressionTransformer.compile( asExp, _cc() ) );
        }
        else
        {
          return setInstanceField( getGosuClass(), symbol.getName(), getDescriptor( symbol.getType() ), AccessibilityUtil.forSymbol( symbol ),
                  pushThis(),
                  ExpressionTransformer.compile( asExp, _cc() ) );
        }
      }
    }

    return new IRNoOpStatement();
  }
}