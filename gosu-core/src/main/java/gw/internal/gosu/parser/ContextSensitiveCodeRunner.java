/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.internal.gosu.ir.transform.expression.EvalExpressionTransformer;
import gw.lang.parser.ExternalSymbolMapForMap;
import gw.lang.parser.IDynamicFunctionSymbol;
import gw.lang.parser.IParseIssue;
import gw.lang.parser.IParseTree;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.IParsedElementWithAtLeastOneDeclaration;
import gw.lang.parser.IProgramClassFunctionSymbol;
import gw.lang.parser.IStatement;
import gw.lang.parser.ISymbol;
import gw.lang.parser.ISymbolTable;
import gw.lang.parser.StandardSymbolTable;
import gw.lang.parser.exceptions.ParseResultsException;
import gw.lang.parser.expressions.ILocalVarDeclaration;
import gw.lang.parser.expressions.IParameterDeclaration;
import gw.lang.parser.expressions.IVarStatement;
import gw.lang.parser.statements.IFunctionStatement;
import gw.lang.parser.statements.IStatementList;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IExternalSymbolMap;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.java.JavaTypes;
import gw.util.GosuExceptionUtil;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 */
public class ContextSensitiveCodeRunner {

  //!! Needed to ensure this class is loaded so a debugger can call into it remotely
  static void ensureLoadedForDebuggerEval() {
    System.out.println( "~~~~~LOADED");
  }

  //!! Do not remove! This is called from the debugger via jdwp.
  /**
   * Intended for use with a debugger to evaluate arbitrary expressions/programs
   * in the context of a source position being debugged, usually at a breakpoint.
   *
   * @param enclosingInstance The instance of the object immediately enclosing the source position.
   * @param extSyms An array of adjacent name/value pairs corresponding with the names and values of local symbols in scope.
   * @param strText The text of the expression/program.
   * @param strClassContext The name of the top-level class enclosing the the source position.
   * @param strContextElementClass The name of the class immediately enclosing the source position (can be same as strClassContext).
   * @param iSourcePosition  The index of the source position within the containing file.
   * @return The result of the expression or, in the case of a program, the return value of the program.
   */
  public static Object runMeSomeCode( Object enclosingInstance, ClassLoader cl, Object[] extSyms, String strText, final String strClassContext, String strContextElementClass, int iSourcePosition )
  {
    // Must execute in caller's classloader
    try {
      Class<?> cls = Class.forName( ContextSensitiveCodeRunner.class.getName(), false, cl );
      Method m = cls.getDeclaredMethod( "_runMeSomeCode", Object.class, Object[].class, String.class, String.class, String.class, int.class );
      m.setAccessible( true );
      return m.invoke( null, enclosingInstance, extSyms, strText, strClassContext, strContextElementClass, iSourcePosition );
    }
    catch( Exception e ) {
      Throwable cause = GosuExceptionUtil.findExceptionCause( e );
      if( cause instanceof ParseResultsException ) {
        List<IParseIssue> parseExceptions = ((ParseResultsException)cause).getParseExceptions();
        if( parseExceptions != null && parseExceptions.size() >= 0 ) {
          throw GosuExceptionUtil.forceThrow( (Throwable)parseExceptions.get( 0 ) );
        }
      }
      throw GosuExceptionUtil.forceThrow( cause );
    }
  }
  private static Object _runMeSomeCode( Object enclosingInstance, Object[] extSyms, String strText, final String strClassContext, String strContextElementClass, int iSourcePosition )
  {
    IType type = TypeSystem.getByFullName( strClassContext, TypeSystem.getGlobalModule() );
    if( !(type instanceof IGosuClassInternal) ) {
      System.out.println( strClassContext + " is not a Gosu class" );
      return null;
    }
    IGosuClassInternal gsClass = (IGosuClassInternal)type;
    gsClass.isValid();
    IParsedElement ctxElem = findElemAt( gsClass, iSourcePosition );
    ISymbolTable compileTimeLocalContextSymbols = findCompileTimeSymbols( gsClass, iSourcePosition );
    IExternalSymbolMap runtimeLocalSymbolValues = makeRuntimeNamesAndValues( extSyms );
    IGosuClassInternal gsImmediateClass = (IGosuClassInternal)TypeSystem.getByFullName( strContextElementClass );
    return EvalExpressionTransformer.compileAndRunEvalSource( strText, enclosingInstance, null, null, gsImmediateClass, ctxElem, compileTimeLocalContextSymbols, runtimeLocalSymbolValues );
  }

  private static IExternalSymbolMap makeRuntimeNamesAndValues( Object[] extSyms ) {
    HashMap<String, ISymbol> map = new HashMap();
    for( int i = 0; i < extSyms.length; i++ ) {
      String name = (String)extSyms[i];
      Object value = extSyms[++i];
      map.put( (String)name, new Symbol( name, JavaTypes.OBJECT(), value ) );
    }

    return new ExternalSymbolMapForMap( map );
  }

  private static IParsedElement findElemAt( IGosuClassInternal gsClass, int iContextLocation ) {
    IParseTree elem = ((IGosuClass)TypeLord.getOuterMostEnclosingClass( gsClass )).getClassStatement().getClassFileStatement().getLocation().getDeepestLocation( iContextLocation, false );
    return elem == null ? gsClass.getClassStatement().getClassFileStatement() : elem.getParsedElement();
  }

  private static ISymbolTable findCompileTimeSymbols( IGosuClassInternal enclosingClass, int iLocation ) {
    ISymbolTable symTable = new StandardSymbolTable( false );
    IParseTree deepestLocation = enclosingClass.getClassStatement().getClassFileStatement().getLocation().getDeepestLocation( iLocation, false );
    collectLocalSymbols( enclosingClass, symTable,
                         deepestLocation.getParsedElement(),
                         iLocation );
    return symTable;
  }

  public static void collectLocalSymbols( IType enclosingType, ISymbolTable symTable, IParsedElement parsedElement, int iOffset ) {
    if( parsedElement == null ) {
      return;
    }

    if( parsedElement instanceof IFunctionStatement ) {
      IFunctionStatement declStmt = (IFunctionStatement)parsedElement;
      if( !declStmt.getDynamicFunctionSymbol().isStatic() ) {
        addThisSymbolForEnhancement( enclosingType, symTable );
      }
      for( IParameterDeclaration localVar : declStmt.getParameters() ) {
        if( localVar != null && localVar.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = localVar.getSymbol();
          symTable.putSymbol( symbol );
        }
      }
    }
    else if( parsedElement instanceof IParsedElementWithAtLeastOneDeclaration ) {
      IParsedElementWithAtLeastOneDeclaration declStmt = (IParsedElementWithAtLeastOneDeclaration)parsedElement;
      for( String strVar : declStmt.getDeclarations() ) {
        ILocalVarDeclaration localVar = findLocalVarSymbol( strVar, declStmt );
        if( localVar != null && localVar.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = localVar.getSymbol();
          symTable.putSymbol( symbol );
        }
      }
    }
    else if( parsedElement instanceof IStatementList ) {
      IStatementList stmtList = (IStatementList)parsedElement;
      for( IStatement stmt : stmtList.getStatements() ) {
        if( stmt instanceof IVarStatement && !((IVarStatement)stmt).isFieldDeclaration() && stmt.getLocation().getOffset() < iOffset ) {
          ISymbol symbol = ((IVarStatement)stmt).getSymbol();
          if( isProgramFieldVar( stmt ) ) {
            continue;
          }
          symTable.putSymbol( symbol );
        }
      }
    }
    IParsedElement parent = parsedElement.getParent();
    if( parent != parsedElement ) {
      collectLocalSymbols( enclosingType, symTable, parent, iOffset );
    }
  }

  private static void addThisSymbolForEnhancement( IType enclosingType, ISymbolTable symTable ) {
    if( enclosingType instanceof IGosuEnhancementInternal ) {
      IType thisType = ((IGosuEnhancementInternal)enclosingType).getEnhancedType();
      if( thisType != null ) {
        thisType = TypeLord.getConcreteType( thisType );
        symTable.putSymbol( new ThisSymbol( thisType, symTable ) );
      }
    }
  }

  private static boolean isProgramFieldVar( IStatement stmt ) {
    if( stmt.getParent() != null ) {
      IParsedElement parent = stmt.getParent().getParent();
      if( parent instanceof IFunctionStatement ) {
        IDynamicFunctionSymbol dfs = ((IFunctionStatement)parent).getDynamicFunctionSymbol();
        if( dfs instanceof IProgramClassFunctionSymbol ) {
          return true;
        }
      }
    }
    return false;
  }

  private static ILocalVarDeclaration findLocalVarSymbol( String strVar, IParsedElement pe ) {
    if( pe instanceof ILocalVarDeclaration ) {
      ISymbol symbol = ((ILocalVarDeclaration)pe).getSymbol();
      if( symbol != null && symbol.getName().equals( strVar ) ) {
        return (ILocalVarDeclaration)pe;
      }
      return null;
    }
    if( pe == null ) {
      return null;
    }
    for( IParseTree child : pe.getLocation().getChildren() ) {
      ILocalVarDeclaration localVar = findLocalVarSymbol( strVar, child.getParsedElement() );
      if( localVar != null ) {
        return localVar;
      }
    }
    return null;
  }
}
