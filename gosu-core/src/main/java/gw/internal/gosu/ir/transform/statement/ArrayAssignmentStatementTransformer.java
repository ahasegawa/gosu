/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.transform.statement;

import gw.config.CommonServices;
import gw.internal.gosu.parser.TypeLoaderAccess;
import gw.internal.gosu.parser.expressions.ArrayAccess;
import gw.internal.gosu.parser.statements.ArrayAssignmentStatement;
import gw.internal.gosu.ir.transform.ExpressionTransformer;
import gw.internal.gosu.ir.transform.TopLevelTransformationContext;
import gw.lang.ir.IRStatement;
import gw.lang.ir.IRExpression;
import gw.lang.ir.IRSymbol;
import gw.lang.ir.statement.IRStatementList;
import gw.lang.parser.EvaluationException;
import gw.lang.reflect.IType;
import gw.lang.reflect.java.IJavaType;
import gw.lang.reflect.java.JavaTypes;
import gw.lang.reflect.java.JavaTypes;

import java.util.List;

/**
 */
public class ArrayAssignmentStatementTransformer extends AbstractStatementTransformer<ArrayAssignmentStatement>
{
  public static IRStatement compile( TopLevelTransformationContext cc, ArrayAssignmentStatement stmt )
  {
    ArrayAssignmentStatementTransformer gen = new ArrayAssignmentStatementTransformer( cc, stmt );
    return gen.compile();
  }

  private ArrayAssignmentStatementTransformer( TopLevelTransformationContext cc, ArrayAssignmentStatement stmt )
  {
    super( cc, stmt );
  }

  @Override
  protected IRStatement compile_impl()
  {
    ArrayAccess arrayAccess = _stmt().getArrayAccessExpression();

    IRExpression originalRoot = ExpressionTransformer.compile( arrayAccess.getRootExpression(), _cc() );

    IRSymbol tempRoot = null;
    IRExpression root;
    boolean needsAutoinsert = needsAutoinsert( arrayAccess );
    if( needsAutoinsert)
    {
      tempRoot = _cc().makeAndIndexTempSymbol( originalRoot.getType() );
      root = identifier( tempRoot );
    } else {
      root = originalRoot;
    }

    IRExpression originalIndex = ExpressionTransformer.compile( arrayAccess.getMemberExpression(), _cc() );
    IRSymbol tempIndex = null;
    IRExpression index;
    if( needsAutoinsert )
    {
      tempIndex = _cc().makeAndIndexTempSymbol( originalIndex.getType() );
      index = identifier( tempIndex );
    }
    else
    {
      index = originalIndex;
    }

    IRExpression value = ExpressionTransformer.compile( _stmt().getExpression(), _cc() );

    IType rootType = arrayAccess.getRootExpression().getType();
    if( rootType.isArray() && isBytecodeType( rootType ) )
    {
      // Normal array access
      return buildArrayStore(root, index, value, getDescriptor( rootType.getComponentType() ));
    }
    else
    {
      if( needsAutoinsert )
      {
        return new IRStatementList( false, buildAssignment( tempRoot, originalRoot ),
                                    buildAssignment( tempIndex, originalIndex ),
                                    buildMethodCall( callStaticMethod( ArrayAssignmentStatementTransformer.class, "setOrAddElement", new Class[]{Object.class, int.class, Object.class},
                                                                       exprList( root, index, value ) ) ) );
      }
      else
      {
        return buildMethodCall(
          callStaticMethod( ArrayAssignmentStatementTransformer.class, "setArrayElement", new Class[]{Object.class, int.class, Object.class},
                            exprList( root, index, value ) ) );
      }
    }
  }

  private static boolean needsAutoinsert( ArrayAccess arrayAccess ) {
    if (ArrayAccess.needsAutoinsert( arrayAccess ) ) {
      return true;
    } else if ( arrayAccess.getRootExpression() instanceof ArrayAccess ) {
      return ArrayAccess.needsAutoinsert((ArrayAccess) arrayAccess.getRootExpression());
    } else {
      return false;
    }
  }

  public static void setArrayElement( Object obj, int iIndex, Object value )
  {
    if( obj instanceof List )
    {
      //noinspection unchecked
      ((List)obj).set( iIndex, value );
      return;
    }

    IType classObj = TypeLoaderAccess.instance().getIntrinsicTypeFromObject( obj );
    if( classObj.isArray() )
    {
      value = CommonServices.getCoercionManager().convertValue( value, classObj.getComponentType() );
      classObj.setArrayComponent( obj, iIndex, value );
      return;
    }

    if( obj instanceof StringBuffer )
    {
      ((StringBuffer)obj).setCharAt( iIndex, ((Character)CommonServices.getCoercionManager().convertValue( value, JavaTypes.CHARACTER() )).charValue() );
    }

    throw new EvaluationException( "The type, " + classObj.getName() + ", is not coercible to an indexed-writable array." );
  }

  public static void setOrAddElement( Object obj, int iIndex, Object value )
  {
    List l = (List)obj;
    if( l.size() == iIndex )
    {
      l.add( value );
    }
    else
    {
      l.set( iIndex, value );
    }
  }
}