/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.transform;

import gw.internal.gosu.ir.nodes.GosuClassIRType;
import gw.internal.gosu.ir.transform.util.RequiresReflectionDeterminer;
import gw.internal.gosu.parser.IGosuClassInternal;
import gw.lang.ir.IRExpression;
import gw.lang.ir.IRStatement;
import gw.lang.ir.IRType;
import gw.lang.parser.IBlockClass;
import gw.lang.parser.Keyword;
import gw.lang.reflect.IRelativeTypeInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.gs.ISourceFileHandle;

import java.util.ArrayList;
import java.util.List;

public class GosuClassTransformationContext extends TransformationContextBase {

  private GosuClassTransformer _classTransformer;
  private IGosuClassInternal _gsClass;
  private List<String> _sourceLines;
  private static final boolean INCLUDE_SOURCE_COMMENTS = false;

  public GosuClassTransformationContext( GosuClassTransformer classTransformer, IGosuClassInternal gsClass ) {
    super(gsClass );
    _classTransformer = classTransformer;
    _gsClass = gsClass;
    maybeIndexSourceCode();
  }

  @Override
  public void maybeAssignOuterRef(List<IRStatement> statements) {
    _classTransformer.maybeAssignOuterRef( statements );
  }

  @Override
  public void maybePushSupersEnclosingThisRef(List<IRExpression> arguments) {
    _classTransformer.maybePushSupersEnclosingThisRef( arguments );
  }

  @Override
  public void pushEnumNameAndOrdinal(IType type, List<IRExpression> args) {
    _classTransformer.pushEnumNameAndOrdinal( type, args );
  }

  @Override
  public void initCapturedSymbolFields(List<IRStatement> statements) {
    _classTransformer.initCapturedSymbolFields( statements );
  }

  @Override
  public void initTypeVarFields(List<IRStatement> statements) {
    _classTransformer.initTypeVarFields( statements );
  }

  @Override
  public void initializeInstanceFields(List<IRStatement> statements) {
    _classTransformer.initializeInstanceFields( statements );
  }

  public void addAssertionsStaticField() {
    _classTransformer.setHasAsserts();
  }

  // --------------- Methods implemented directly here

  @Override
  public String getOuterThisParamName() {
    return getOuterThisFieldName() + "$$arg";
  }

  @Override
  public String getSourceFileRef() {
    // Try to find the outermost class that's a Gosu class, so that inner classes get the
    // source file name of the outermost class
    IGosuClassInternal outermostClass = _gsClass;
    while (outermostClass.getEnclosingType() != null && outermostClass.getEnclosingType() instanceof IGosuClassInternal) {
      outermostClass = (IGosuClassInternal) outermostClass.getEnclosingType();
    }
    
    ISourceFileHandle sfh = outermostClass.getSourceFileHandle();
    return sfh == null ? null : sfh.getFileName();
  }

  @Override
  public String getSourceLine(int lineNumber) {
    if (_sourceLines != null) {
      return _sourceLines.get(lineNumber - 1);
    } else {
      return null;
    }
  }
  
  @Override
  public String getOuterThisFieldName() {
    return Keyword.KW_this + "$" + (_gsClass.getDepth()-1);
  }

  @Override
  public IRType getIRTypeForCurrentClass() {
    return GosuClassIRType.get( _gsClass );
  }

  @Override
  public boolean isExternalSymbol(String name) {
    return _gsClass.getExternalSymbol( name ) != null;
  }

  @Override
  public boolean isFragmentEvaluation() {
    return false;
  }

  // ---------------------------------- Private helper methods

  private void maybeIndexSourceCode() {
    if (INCLUDE_SOURCE_COMMENTS) {
      String source = _gsClass.getSource();
      _sourceLines = split(source);
    }
  }

  private List<String> split(String source) {
    List<String> results = new ArrayList<String>();
    int start = 0;
    int lineIndex = source.indexOf('\n');
    while (lineIndex != -1) {
      results.add(source.substring(start, lineIndex));
      start = lineIndex + 1;
      lineIndex = source.indexOf('\n', start);
    }
    results.add(source.substring(start));
    return results;
  }

  public boolean shouldUseReflection( IType declaringClass, IRelativeTypeInfo.Accessibility accessibility )
  {
    return RequiresReflectionDeterminer.shouldUseReflection( declaringClass, _gsClass, accessibility );
  }

  @Override
  public boolean currentlyCompilingBlock()
  {
    return getGosuClass() instanceof IBlockClass;
  }
}
