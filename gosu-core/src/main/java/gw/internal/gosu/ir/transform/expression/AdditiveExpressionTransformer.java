/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.transform.expression;

import gw.config.CommonServices;
import gw.internal.gosu.parser.BeanAccess;
import gw.internal.gosu.parser.expressions.AdditiveExpression;
import gw.internal.gosu.parser.expressions.Identifier;
import gw.lang.ir.IRExpression;
import gw.internal.gosu.ir.nodes.IRMethodFactory;
import gw.lang.ir.expression.IRArithmeticExpression;
import gw.internal.gosu.ir.transform.ExpressionTransformer;
import gw.internal.gosu.ir.transform.TopLevelTransformationContext;
import gw.lang.ir.expression.IRStringLiteralExpression;
import gw.lang.parser.IParseTree;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.StandardSymbolTable;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.java.JavaTypes;

/**
 */
public class AdditiveExpressionTransformer extends AbstractExpressionTransformer<AdditiveExpression>
{
  public static IRExpression compile( TopLevelTransformationContext cc, AdditiveExpression expr )
  {
    AdditiveExpressionTransformer gen = new AdditiveExpressionTransformer( cc, expr );
    return gen.compile();
  }

  private AdditiveExpressionTransformer( TopLevelTransformationContext cc, AdditiveExpression expr )
  {
    super( cc, expr );
  }

  protected IRExpression compile_impl()
  {
    boolean bNumeric = BeanAccess.isNumericType( _expr().getType() );
    IMethodInfo operatorOverload = _expr().getOverride();
    if( bNumeric && operatorOverload != null )
    {
      // The operator is overloaded, call into it...
      IRExpression lhs = ExpressionTransformer.compile( _expr().getLHS(), _cc() );
      IRExpression rhs = ExpressionTransformer.compile( _expr().getRHS(), _cc() );
      return callMethod( IRMethodFactory.createIRMethod( operatorOverload, null ), lhs, exprList( rhs ) );
    }
    else
    {
      if( isSimpleAddition() )
      {
        return simpleAddition( );
      }
      else
      {
        if( isCompileTimeConstantConcatenation() )
        {
          return concatenate();
        }
//## todo: handle other cases efficiently
//   (ex var a = "1" var b = "2" var c = a + b  -> we should generate code using a StringBuilder)
//        else
//        {
//
//        }
        return complexAddition( bNumeric );
      }
    }
  }

  private IRStringLiteralExpression concatenate() {
    return (IRStringLiteralExpression) pushConstant( StandardSymbolTable.toString( _expr().getLHS().evaluate() ) +
                                                     StandardSymbolTable.toString( _expr().getRHS().evaluate() ) );
  }

  private IRExpression simpleAddition( )
  {
    IType type = _expr().getType();

    // Push LHS
    IRExpression lhs = ExpressionTransformer.compile( _expr().getLHS(), _cc() );
    // Maybe convert to expression type
    lhs = numberConvert( _expr().getLHS().getType(), type, lhs );

    // Push RHS
    IRExpression rhs = ExpressionTransformer.compile( _expr().getRHS(), _cc() );
    // Maybe convert to expression type
    rhs = numberConvert( _expr().getRHS().getType(), type, rhs );

    return new IRArithmeticExpression(getDescriptor( type ), lhs, rhs, _expr().isAdditive() ? IRArithmeticExpression.Operation.Addition : IRArithmeticExpression.Operation.Subtraction );
  }

  private IRExpression complexAddition( boolean bNumeric )
  {

    // Call into Gosu's runtime for the answer.  The arguments are the result type, the boxed LHS, the boxed RHS,
    // the LHS type, the RHS type, whether it's addition, and whether it's numeric
    IRExpression evaluateCall = callStaticMethod( AdditiveExpression.class, "evaluate",
                                                  new Class[]{IType.class, Object.class, Object.class, IType.class, IType.class, boolean.class, boolean.class, boolean.class},
                                                  exprList( pushType( _expr().getType() ),
                                                            boxValue( _expr().getLHS().getType(), ExpressionTransformer.compile( _expr().getLHS(), _cc() ) ),
                                                            boxValue( _expr().getRHS().getType(), ExpressionTransformer.compile( _expr().getRHS(), _cc() ) ),
                                                            pushType( _expr().getLHS().getType() ),
                                                            pushType( _expr().getRHS().getType() ),
                                                            pushConstant( _expr().isAdditive() ),
                                                            pushConstant( _expr().isNullSafe() ),
                                                            pushConstant( bNumeric ) ) );

    // Ensure value is unboxed if type is primitive
    return unboxValueToType( _expr().getType(), evaluateCall );
  }

  public boolean isSimpleAddition()
  {
    return isPrimitiveNumberType( _expr().getType() ) &&
           isPrimitiveNumberType( _expr().getLHS().getType() ) &&
           isPrimitiveNumberType( _expr().getRHS().getType() );
  }

  private boolean isCompileTimeConstantConcatenation()
  {
    return  _expr().getType() == JavaTypes.STRING() &&
            _expr().isCompileTimeConstant() &&
            !containsIdentifier( _expr().isAssignment() ? _expr().getParent() : _expr() );
  }

  private boolean containsIdentifier( IParsedElement expr )
  {
    if ( expr instanceof Identifier &&
         !((Identifier) expr).isStaticFinalInitializedCompileTimeConstant() )
    {
      return true;
    }
    IParseTree location = expr.getLocation();
    if( location == null )
    {
      return true;
    }
    for( IParseTree child: location.getChildren() )
    {
      if( containsIdentifier( child.getParsedElement() ) )
      {
        return true;
      }
    }
    return false;
  }
}
