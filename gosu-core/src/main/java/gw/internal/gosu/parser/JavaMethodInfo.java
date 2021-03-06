/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser;

import gw.lang.Deprecated;
import gw.lang.GosuShop;
import gw.lang.PublishedName;
import gw.lang.javadoc.IClassDocNode;
import gw.lang.javadoc.IDocRef;
import gw.lang.javadoc.IExceptionNode;
import gw.lang.javadoc.IMethodNode;
import gw.lang.javadoc.IParamNode;
import gw.lang.parser.GosuParserTypes;
import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IExceptionInfo;
import gw.lang.reflect.IFeatureInfo;
import gw.lang.reflect.IMethodCallHandler;
import gw.lang.reflect.IParameterInfo;
import gw.lang.reflect.IScriptabilityModifier;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeVariableType;
import gw.lang.reflect.ModifiedParameterInfo;
import gw.lang.reflect.SimpleParameterInfo;
import gw.lang.reflect.TypeInfoUtil;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGenericTypeVariable;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.java.ClassInfoUtil;
import gw.lang.reflect.java.IJavaAnnotatedElement;
import gw.lang.reflect.java.IJavaClassGenericArrayType;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.java.IJavaClassMethod;
import gw.lang.reflect.java.IJavaClassParameterizedType;
import gw.lang.reflect.java.IJavaClassType;
import gw.lang.reflect.java.IJavaClassTypeVariable;
import gw.lang.reflect.java.IJavaClassWildcardType;
import gw.lang.reflect.java.IJavaMethodDescriptor;
import gw.lang.reflect.java.IJavaMethodInfo;
import gw.lang.reflect.java.IJavaParameterDescriptor;
import gw.lang.reflect.java.JavaTypes;
import gw.lang.reflect.java.JavaExceptionInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class JavaMethodInfo extends JavaBaseFeatureInfo implements IJavaMethodInfo
{
  private static final int UNINITED = 0;
  private static final int TRUE_ENC = 1;
  private static final int FALSE_ENC = 2;

  private IJavaMethodDescriptor _md;
  private boolean _forceHidden;
  private IParameterInfo[] _params;
  private IParameterInfo[] _typedParams;
  private IType _retType;
  private IMethodCallHandler _callHandler;
  private IGenericTypeVariable[] _typeVars;
  private int _staticCache = UNINITED;
  private List<IExceptionInfo> _exceptions;
  private IDocRef<IMethodNode> _methodDocs = new IDocRef<IMethodNode>() {
    @Override
    public IMethodNode get() {
      if (getContainer() instanceof JavaTypeInfo) {
        IClassDocNode classDocs = ((JavaTypeInfo) getContainer()).getDocNode().get();
        return classDocs == null ? null : classDocs.getMethod(_md);
      } else {
        return null;
      }
    }
  };
  private String _name;
  private String _signature;

  /**
   * @param container Typically this will be the containing ITypeInfo
   * @param md        The method descriptor (from BeanInfo)
   */
  public JavaMethodInfo(IFeatureInfo container, IJavaMethodDescriptor md, boolean forceHidden) {
    super(container);
    _md = md;
    _forceHidden = forceHidden;
    _name = _md.getName();
    if (_md.getMethod().isAnnotationPresent( PublishedName.class)) {
      _name = (String) _md.getMethod().getAnnotation( PublishedName.class).getFieldValue("value");
    }
    _signature = makeSignature();
  }

  @Override
  public IParameterInfo[] getGenericParameters()
  {
    return getParameters( true );
  }

  @Override
  public IParameterInfo[] getParameters()
  {
    IType ownerType = getOwnersType();
    return getParameters( !ownerType.isGenericType() || ownerType.isParameterizedType() );
  }

  private IParameterInfo[] getParameters( boolean bKeepTypeVars )
  {
    if (bKeepTypeVars) {
      if (_typedParams == null) {
        _typedParams = convertParameterDescriptors( bKeepTypeVars );
      }
      return _typedParams;
    } else {
      if (_params == null) {
        _params = convertParameterDescriptors( bKeepTypeVars );
      }
      return _params;
    }
  }

  @Override
  public IType getGenericReturnType()
  {
    return getReturnType( true );
  }

  @Override
  public IType getReturnType()
  {
    IType ownerType = getOwnersType();
    return getReturnType( !ownerType.isGenericType() || ownerType.isParameterizedType() );
  }

  private IType getReturnType( boolean bKeepTypeVars )
  {
    if( _retType != null && !bKeepTypeVars )
    {
      return _retType;
    }

    IType declaringClass = _md.getMethod().getEnclosingClass().getJavaType();
    TypeVarToTypeMap actualParamByVarName = TypeLord.mapTypeByVarName( getOwnersType(), declaringClass, bKeepTypeVars );
    actualParamByVarName = addEnclosingTypeParams( declaringClass, actualParamByVarName );

    for( IGenericTypeVariable tv : getTypeVariables() )
    {
      if( actualParamByVarName.isEmpty() )
      {
        actualParamByVarName = new TypeVarToTypeMap();
      }
      if( bKeepTypeVars )
      {
        actualParamByVarName.put( tv.getTypeVariableDefinition().getType(),
                                  tv.getTypeVariableDefinition() != null
                                  ? tv.getTypeVariableDefinition().getType()
                                  : new TypeVariableType( getOwnersType(), tv ) );
      }
      else
      {
        actualParamByVarName.put( tv.getTypeVariableDefinition().getType(), tv.getBoundingType() );
      }
    }

    IType retType = ClassInfoUtil.getActualReturnType(_md.getMethod().getGenericReturnType(), actualParamByVarName, bKeepTypeVars);
    if (TypeSystem.isDeleted(retType)) {
      return null;
    }
    if( retType.isGenericType() && !retType.isParameterizedType() )
    {
      retType = TypeLord.getDefaultParameterizedType( retType );
    }

    retType = ClassInfoUtil.getPublishedType(retType, _md.getMethod().getEnclosingClass());

    if( !bKeepTypeVars )
    {
      _retType = retType;
    }

    return retType;
  }

  public static TypeVarToTypeMap addEnclosingTypeParams( IType declaringClass, TypeVarToTypeMap actualParamByVarName )
  {
    while( declaringClass.getEnclosingType() != null && !Modifier.isStatic( declaringClass.getModifiers() ) )
    {
      declaringClass = declaringClass.getEnclosingType();
      IGenericTypeVariable[] typeVariables = declaringClass.getGenericTypeVariables();
      if( typeVariables != null )
      {
        for( IGenericTypeVariable typeVariable : typeVariables )
        {
          if( actualParamByVarName.isEmpty() )
          {
            actualParamByVarName = new TypeVarToTypeMap();
          }
          actualParamByVarName.put( typeVariable.getTypeVariableDefinition().getType(),
                                    typeVariable.getTypeVariableDefinition() != null
                                    ? typeVariable.getTypeVariableDefinition().getType()
                                    : new TypeVariableType( declaringClass, typeVariable ) );
        }
      }
    }
    return actualParamByVarName;
  }

  @Override
  public List<IAnnotationInfo> getDeclaredAnnotations() {
    List<IAnnotationInfo> annotations = super.getDeclaredAnnotations();
    if (getMethodDocs().get() != null && getMethodDocs().get().isDeprecated()) {
      annotations.add(GosuShop.getAnnotationInfoFactory().createJavaAnnotation(makeDeprecated(getMethodDocs().get().getDeprecated()), this));
    }
    return annotations;
  }

  @Override
  public IGenericTypeVariable[] getTypeVariables()
  {
    if( _typeVars == null )
    {
      _typeVars = _md.getMethod().getTypeVariables(this);
    }
    return _typeVars;
  }


  @Override
  public IType getParameterizedReturnType( IType... typeParams )
  {
    TypeVarToTypeMap actualParamByVarName =
      TypeLord.mapTypeByVarName( getOwnersType(), _md.getMethod().getEnclosingClass().getJavaType(), true );
    int i = 0;
    for( IGenericTypeVariable tv : getTypeVariables() )
    {
      if( actualParamByVarName.isEmpty() )
      {
        actualParamByVarName = new TypeVarToTypeMap();
      }
      actualParamByVarName.putByString( tv.getName(), typeParams[i++] );
    }
    return _md.getMethod().getGenericReturnType().getActualType(actualParamByVarName, true);
  }

  public IType[] getParameterizedParameterTypes( IType... typeParams )
  {
    return getParameterizedParameterTypes2( null, typeParams );
  }
  public IType[] getParameterizedParameterTypes2( IGosuClass ownersType, IType... typeParams )
  {
    IType ot = ownersType == null ? getOwnersType() : ownersType;
    TypeVarToTypeMap actualParamByVarName =
      TypeLord.mapTypeByVarName( ot, _md.getMethod().getEnclosingClass().getJavaType(), true );
    int i = 0;
    for( IGenericTypeVariable tv : getTypeVariables() )
    {
      if( actualParamByVarName.isEmpty() )
      {
        actualParamByVarName = new TypeVarToTypeMap();
      }
      actualParamByVarName.putByString( tv.getName(), typeParams[i++] );
    }

    return ClassInfoUtil.getActualTypes(_md.getMethod().getGenericParameterTypes(), actualParamByVarName, true);
  }

  //  <T extends CharSequence> T[] foo( ArrayList<? extends T>[] s ) { return null; }
  @Override
  public TypeVarToTypeMap inferTypeParametersFromArgumentTypes( IType... argTypes )
  {
    return inferTypeParametersFromArgumentTypes2( null, argTypes );
  }
  @Override
  public TypeVarToTypeMap inferTypeParametersFromArgumentTypes2( IGosuClass ownersType, IType... argTypes )
  {
    IJavaClassType[] genParamTypes = _md.getMethod().getGenericParameterTypes();
    IType ot = ownersType == null ? getOwnersType() : ownersType;
    TypeVarToTypeMap actualParamByVarName = TypeLord.mapTypeByVarName( ot, _md.getMethod().getEnclosingClass().getJavaType(), true );
    IGenericTypeVariable[] typeVars = getTypeVariables();
    for( IGenericTypeVariable tv : typeVars )
    {
      if( actualParamByVarName.isEmpty() )
      {
        actualParamByVarName = new TypeVarToTypeMap();
      }
      actualParamByVarName.put( tv.getTypeVariableDefinition().getType(), tv.getBoundingType() );
    }

    TypeVarToTypeMap map = new TypeVarToTypeMap();
    for( int i = 0; i < argTypes.length; i++ )
    {
      if( genParamTypes.length > i )
      {
        IType argType = argTypes[i];
        IJavaClassType genParamType = genParamTypes[i];
        inferTypeVariableTypesFromGenParamTypeAndConcreteType( genParamType, argType, map );
        ensureInferredTypeAssignableToBoundingType( actualParamByVarName, map );
      }
    }
    return map;
  }

  private void ensureInferredTypeAssignableToBoundingType( TypeVarToTypeMap actualParamByVarName, TypeVarToTypeMap map )
  {
    for( Object s : map.keySet() )
    {
      IType inferredType = map.getRaw( s );
      IType boundingType = actualParamByVarName.getRaw( s );
      if( !boundingType.isAssignableFrom( inferredType ) )
      {
        map.putRaw( s, boundingType );
      }
    }
  }

  private void inferTypeVariableTypesFromGenParamTypeAndConcreteType( IJavaClassType genParamType, IType argType, TypeVarToTypeMap map )
  {
    if( argType == GosuParserTypes.NULL_TYPE() )
    {
      return;
    }

    if( genParamType instanceof IJavaClassGenericArrayType)
    {
      //## todo: DON'T allow a null component type here; we do it now as a hack that enables gosu arrays to be compatible with java arrays
      //## todo: same as TypeLord.inferTypeVariableTypesFromGenParamTypeAndConcreteType()
      if( argType.getComponentType() == null || !argType.getComponentType().isPrimitive() )
      {
        inferTypeVariableTypesFromGenParamTypeAndConcreteType(
          ((IJavaClassGenericArrayType)genParamType).getGenericComponentType(), argType.getComponentType(), map );
      }
    }
    else if( genParamType instanceof IJavaClassParameterizedType)
    {
      IJavaClassParameterizedType parameterizedType = (IJavaClassParameterizedType)genParamType;
      IType argTypeInTermsOfParamType = TypeLord.findParameterizedType( argType, genParamType.getActualType( TypeVarToTypeMap.EMPTY_MAP ) );
      if( argTypeInTermsOfParamType == null )
      {
        return;
      }
      IType[] concreteTypeParams = argTypeInTermsOfParamType.getTypeParameters();
      if( concreteTypeParams != null && concreteTypeParams.length > 0 )
      {
        int i = 0;
        for( IJavaClassType typeArg : parameterizedType.getActualTypeArguments() )
        {
          inferTypeVariableTypesFromGenParamTypeAndConcreteType( typeArg, concreteTypeParams[i++], map );
        }
      }
    }
    else if( genParamType instanceof IJavaClassTypeVariable)
    {
      String strTypeVarName = genParamType.getName();
      IType type = map.getByString( strTypeVarName );
      if( type == null || type instanceof TypeVariableType )
      {
        // Infer the type
        map.putByString( strTypeVarName, argType );
      }
    }
    else if( genParamType instanceof IJavaClassWildcardType)
    {
      IJavaClassWildcardType wildcardType = (IJavaClassWildcardType)genParamType;
      inferTypeVariableTypesFromGenParamTypeAndConcreteType(
        wildcardType.getUpperBound(), argType, map );
    }
  }

  @Override
  public IMethodCallHandler getCallHandler()
  {
    if( _callHandler != null )
    {
      return _callHandler;
    }
    IJavaClassMethod method = this._md.getMethod();
    if (!(method instanceof MethodJavaClassMethod)) {
      return null;
    }
    return _callHandler = new MethodCallAdapter( ((MethodJavaClassMethod)method).getJavaMethod() );
  }

  @Override
  public String getReturnDescription()
  {
    return getMethodDocs().get() == null ? "" : getMethodDocs().get().getReturnDescription();
  }

  @Override
  public List<IExceptionInfo> getExceptions()
  {
    if( _exceptions == null )
    {
      IJavaClassMethod method = _md.getMethod();
      IJavaClassInfo[] classes = method.getExceptionTypes();
      _exceptions = new ArrayList<IExceptionInfo>();
      for (int i = 0; i < classes.length; i++) {
        final IJavaClassInfo exceptionClass = classes[i];
        _exceptions.add(new JavaExceptionInfo(this, exceptionClass, new IDocRef<IExceptionNode>() {
          @Override
          public IExceptionNode get() {
            return getMethodDocs().get() == null ? null : getMethodDocs().get().getException(exceptionClass);
          }
        }));
      }
    }

    // merge in methods exceptions with the annotations
    return _exceptions;
  }

  @Override
  public String getName() {
    return _signature;
  }

  private String makeSignature() {
    String name = getDisplayName();
    name += TypeInfoUtil.getTypeVarList( this, true );
    name += "(";
    IParameterInfo[] parameterInfos = getGenericParameters();
    if (parameterInfos.length > 0) {
      name += " ";
      for (int i = 0; i < parameterInfos.length; i++) {
        IParameterInfo iParameterInfo = parameterInfos[i];
        if (i != 0) {
          name += ", ";
        }
        name += iParameterInfo.getFeatureType().getName();
      }
      name += " ";
    }
    name += ")";
    return name;
  }

  @Override
  public String getDisplayName()
  {
    return _name;
  }

  @Override
  public String getShortDescription()
  {
    return getMethodDocs().get() == null ? null : getMethodDocs().get().getDescription();
  }

  @Override
  public String getDescription()
  {
    return getMethodDocs().get() == null ? null : getMethodDocs().get().getDescription();
  }

  @Override
  public boolean isHidden() {
    return _forceHidden || super.isHidden();
  }

  @Override
  protected boolean isDefaultEnumFeature()
  {
    if( getOwnersType().isEnum() )
    {
      String name = getName();
      return isStatic() && (name.equals( "values()" ) || name.equals( "valueOf( java.lang.String )" ));
    }
    else
    {
      return false;
    }
  }

  @Override
  public boolean isVisible(IScriptabilityModifier constraint) {
    return !_forceHidden && super.isVisible(constraint);
  }

  @Override
  public boolean isStatic()
  {
    if( _staticCache == UNINITED )
    {
      synchronized( this )
      {
        if( _staticCache == UNINITED )
        {
          if( Modifier.isStatic( _md.getMethod().getModifiers() ) )
          {
            _staticCache = TRUE_ENC;
          }
          else
          {
            _staticCache = FALSE_ENC;
          }
        }
      }
    }
    return _staticCache == TRUE_ENC;
  }

  @Override
  public boolean isPrivate()
  {
    return Modifier.isPrivate( _md.getMethod().getModifiers() );
  }

  @Override
  public boolean isInternal()
  {
    return !isPrivate() && !isPublic() && !isProtected();
  }

  @Override
  public boolean isProtected()
  {
    return Modifier.isProtected( _md.getMethod().getModifiers() );
  }

  @Override
  public boolean isPublic()
  {
    return Modifier.isPublic( _md.getMethod().getModifiers() );
  }

  @Override
  public boolean isAbstract()
  {
    return Modifier.isAbstract( _md.getMethod().getModifiers() );
  }

  @Override
  public boolean isFinal()
  {
    return Modifier.isFinal( _md.getMethod().getModifiers() );
  }

  @Override
  public boolean isDeprecated()
  {
    return super.isDeprecated() || getMethod().isAnnotationPresent( Deprecated.class );
  }

  @Override
  public String getDeprecatedReason() {
    String deprecated = super.getDeprecatedReason();
    if (isDeprecated() && deprecated == null) {
      return (String) getMethod().getAnnotation( Deprecated.class ).getFieldValue("value");
    }
    return deprecated;
  }

  private IParameterInfo[] convertParameterDescriptors( boolean bKeepTypeVars )
  {
    IType declaringClass = _md.getMethod().getEnclosingClass().getJavaType();
    TypeVarToTypeMap actualParamByVarName =
      TypeLord.mapTypeByVarName( getOwnersType(), declaringClass, bKeepTypeVars );
    actualParamByVarName = addEnclosingTypeParams( declaringClass, actualParamByVarName );

    for( IGenericTypeVariable tv : getTypeVariables() )
    {
      if( actualParamByVarName.isEmpty() )
      {
        actualParamByVarName = new TypeVarToTypeMap();
      }
      if( bKeepTypeVars )
      {
        actualParamByVarName.put( tv.getTypeVariableDefinition().getType(),
                                  tv.getTypeVariableDefinition() != null
                                  ? tv.getTypeVariableDefinition().getType()
                                  : new TypeVariableType( getOwnersType(), tv ) );
      }
      else
      {
        actualParamByVarName.put( tv.getTypeVariableDefinition().getType(), tv.getBoundingType() );
      }
    }
    IJavaClassType[] paramTypes = _md.getMethod().getGenericParameterTypes();
    return convertGenericParameterTypes( this, actualParamByVarName, paramTypes, bKeepTypeVars, _md.getMethod().getEnclosingClass());
  }

  static IParameterInfo[] convertGenericParameterTypes( IFeatureInfo container,
                                                        TypeVarToTypeMap actualParamByVarName,
                                                        IJavaClassType[] paramTypes,
                                                        boolean bKeepTypeVars,
                                                        IJavaClassInfo declaringClass )
  {
    if( paramTypes == null )
    {
      return null;
    }

// Disabling this "hack" with modifying method signatures, all prevously written source code should work without problems.
// TODO: remove completely in future
//      IParameterInfo[] fixedParamInfo = maybeFixJavaCovarianceHandicappedMethods( container, paramTypes, bKeepTypeVars );
//      if( fixedParamInfo != null )
//      {
//        return fixedParamInfo;
//      }
    IParameterInfo[] pi = new IParameterInfo[paramTypes.length];
    for( int i = 0; i < paramTypes.length; i++ )
    {
      IType parameterType = null;

      if(paramTypes[i] != null) {
        parameterType = paramTypes[i].getActualType( actualParamByVarName, bKeepTypeVars );
      }

      if (parameterType == null) {
        parameterType = TypeSystem.getErrorType();
      }

      parameterType = ClassInfoUtil.getPublishedType(parameterType, declaringClass);

      pi[i] = new SimpleParameterInfo( container, parameterType, i );
    }
    return pi;

  }

  /**
   * Fixes the argument type of some common methods that make no assumption about the type of the argument internally
   * but which are handicapped by java's covariance rules with wildcard types.  For example, Collection<T>.contains(Object)
   * takes an argument of type Object rather than T so that if you have a collection parameterized on a wildcard type
   * (e.g. Collection<? extends CharSequence>) you can still invoke the contains method.
   *
   * @param container
   * @param paramTypes
   *@param bKeepTypeVars @return
   */
  private static IParameterInfo[] maybeFixJavaCovarianceHandicappedMethods( IFeatureInfo container, IJavaClassType[] paramTypes, boolean bKeepTypeVars )
  {
    IType intrinsicType = container.getOwnersType();
    String displayName = container.getDisplayName();
    if( (displayName.equals("contains") ||
         displayName.equals("remove")) &&
                                                        paramTypes.length == 1 &&
                                                        paramTypes[0] instanceof IJavaClassInfo &&
                                                        ((IJavaClassInfo)paramTypes[0]).getJavaType() == JavaTypes.OBJECT() &&
                                                        JavaTypes.COLLECTION().isAssignableFrom(intrinsicType.getGenericType()) )
    {
      TypeVarToTypeMap map = resolveTypes( intrinsicType, JavaTypes.COLLECTION(), bKeepTypeVars );
      return new IParameterInfo[]{new ModifiedParameterInfo( container, map.getByString( "E" ), JavaTypes.OBJECT(), 0 )};
    }

    if( (displayName.equals("indexOf") ||
         displayName.equals("lastIndexOf")) &&
                                                        paramTypes.length == 1 &&
                                                        paramTypes[0] instanceof IJavaClassInfo &&
                                                        ((IJavaClassInfo)paramTypes[0]).getJavaType() == JavaTypes.OBJECT() &&
                                                        JavaTypes.LIST().isAssignableFrom(intrinsicType.getGenericType()) )
    {
      TypeVarToTypeMap map = resolveTypes( intrinsicType, JavaTypes.LIST(), bKeepTypeVars );
      return new IParameterInfo[]{new ModifiedParameterInfo( container, map.getByString( "E" ), JavaTypes.OBJECT(), 0 )};
    }

    if( (displayName.equals("retainAll") ||
         displayName.equals("containsAll") ||
         displayName.equals("removeAll")) )
    {
      if( paramTypes.length == 1 &&
          paramTypes[0] instanceof IJavaClassParameterizedType &&
          paramTypes[0].getConcreteType() instanceof IJavaClassInfo &&
          ((IJavaClassInfo) paramTypes[0].getConcreteType()).getJavaType() == JavaTypes.COLLECTION() &&
          JavaTypes.COLLECTION().isAssignableFrom(intrinsicType.getGenericType()) )
      {
        TypeVarToTypeMap map = resolveTypes( intrinsicType, JavaTypes.COLLECTION(), bKeepTypeVars );
        return new IParameterInfo[]{new ModifiedParameterInfo( container,
            JavaTypes.COLLECTION().getParameterizedType(map.getByString("E")),
            JavaTypes.COLLECTION().getParameterizedType(JavaTypes.OBJECT()), 0 )};
      }
    }

    if( (displayName.equals("get") ||
         displayName.equals("containsKey") ||
         displayName.equals("remove") ||
         displayName.equals("containsValue")) &&
                                                               paramTypes.length == 1 &&
                                                               paramTypes[0] instanceof IJavaClassInfo &&
                                                               ((IJavaClassInfo)paramTypes[0]).getJavaType() == JavaTypes.OBJECT() &&
                                                               JavaTypes.MAP().isAssignableFrom(intrinsicType.getGenericType()) )
    {
      TypeVarToTypeMap map = resolveTypes( intrinsicType, JavaTypes.MAP(), bKeepTypeVars );
      IType type = displayName.equals("containsValue") ? map.getByString( "V" ) : map.getByString( "K" );
      return new IParameterInfo[]{new ModifiedParameterInfo( container, type, JavaTypes.OBJECT(), 0 )};
    }
    return null;
  }

  private static TypeVarToTypeMap resolveTypes( IType type, IType declaringType, boolean bKeepTypeVars )
  {
    TypeVarToTypeMap paramsByName = TypeLord.mapTypeByVarName( type, declaringType, bKeepTypeVars );
    for( Map.Entry<Object, IType> entry : paramsByName.entrySet() )
    {
      if( !bKeepTypeVars && entry.getValue() instanceof ITypeVariableType )
      {
        entry.setValue( ((ITypeVariableType)entry.getValue()).getBoundingType() );
      }
    }
    return paramsByName;
  }

  @Override
  public IJavaClassMethod getMethod()
  {
    return _md.getMethod();
  }

  @Override
  public String toString()
  {
    return getName();
  }

  @Override
  protected IJavaAnnotatedElement getAnnotatedElement()
  {
    return _md.getMethod();
  }

  @Override
  protected boolean isVisibleViaFeatureDescriptor(IScriptabilityModifier constraint) {
    return _md.isVisibleViaFeatureDescriptor(constraint);
  }

  @Override
  protected boolean isHiddenViaFeatureDescriptor() {
    return _md.isHiddenViaFeatureDescriptor();
  }

  @Override
  public IDocRef<IParamNode> getDocsForParam(final int paramIndex) {
    return new IDocRef<IParamNode>() {
      @Override
      public IParamNode get() {
        if (getMethodDocs().get() != null) {
          List<? extends IParamNode> list = getMethodDocs().get().getParams();
          if (list.size() > paramIndex) {
            return list.get(paramIndex);
          }
        }
        return null;
      }
    };
  }

  @Override
  public IDocRef<IMethodNode> getMethodDocs() {
    return _methodDocs;
  }

  @Override
  public Method getRawMethod() {
    return ((MethodJavaClassMethod)_md.getMethod()).getJavaMethod();
  }

  @Override
  public int getModifiers() {
    return _md.getMethod().getModifiers();
  }
}
