/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.util.cache;

import gw.internal.gosu.parser.StringCache;
import gw.util.DynamicArray;
import gw.util.Predicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FqnCache<T> implements IFqnCache<T> {
  private static Map<String, String[]> PARTS_CACHE = new ConcurrentHashMap<String, String[]>();
  private FqnCacheNode<T> _root = new FqnCacheNode<T>("root", null);

  public FqnCacheNode<T> getRoot() {
    return _root;
  }

  public FqnCacheNode<T> getNode(String fqn) {
    FqnCacheNode<T> n = _root;
    for (String part : getParts(fqn)) {
      n = n.getChild(part);
      if (n == null) {
        break;
      }
    }
    return n;
  }

  @Override
  public final T get( String fqn ) {
    FqnCacheNode<T> n = getNode( fqn );
    return n == null ? null : n.getUserData();
  }

  @Override
  public final boolean contains( String fqn ) {
    return getNode(fqn) != null;
  }

  @Override
  public final void add( String fqn ) {
    add(fqn, null);
  }

  @Override
  public void add( String fqn, T userData ) {
    FqnCacheNode<T> n = _root;
    for (String part : getParts(fqn)) {
      n = n.getOrCreateChild(part);
    }
    n.setUserData(userData);
  }

  @Override
  public final void remove( String[] fqns ) {
    for (String fqn : fqns) {
      remove(fqn);
    }
  }

  @Override
  public boolean remove( String fqn ) {
    FqnCacheNode<T> n = _root;
    for (String part : getParts(fqn)) {
      n = n.getChild(part);
      if( n == null ) {
        return false;
      }
    }
    n.delete();
    return true;
  }

  @Override
  public final void clear() {
    _root.clear();
  }

  @Override
  public Set<String> getFqns() {
    Set<String> names = new HashSet<String>();
    _root.collectNames(names, "");
    return names;
  }

  public void visitDepthFirst( Predicate<T> visitor ) {
    List<FqnCacheNode<T>> copy = new ArrayList<FqnCacheNode<T>>( _root.getChildren() );
    for( FqnCacheNode<T> child: copy ) {
      if( !child.visitDepthFirst( visitor ) ) {
        return;
      }
    }
  }

  public void visitNodeDepthFirst( Predicate<FqnCacheNode> visitor ) {
    List<FqnCacheNode<T>> copy = new ArrayList<FqnCacheNode<T>>( _root.getChildren() );
    for( FqnCacheNode<T> child: copy ) {
      if( !child.visitNodeDepthFirst( visitor ) ) {
        return;
      }
    }
  }

  public void visitBreadthFirst( Predicate<T> visitor ) {
    List<FqnCacheNode<T>> copy = new ArrayList<FqnCacheNode<T>>( _root.getChildren() );
    for( FqnCacheNode<T> child: copy ) {
      child.visitBreadthFirst( visitor );
    }
  }

  private static String[] split( String fqn ) {
    String theRest = fqn;
    DynamicArray<String> parts = new DynamicArray<String>();
    while( theRest != null ) {
      int iParam = theRest.indexOf( '<' );
      int iDot = theRest.indexOf( '.' );
      int iArray = theRest.indexOf( '[' );
      String part;
      if( iParam == 0 ) {
        if( iArray > 0 ) {
          part = theRest.substring( 0, iArray );
          theRest = iArray < theRest.length() ? theRest.substring( iArray ) : null;
        }
        else {
          if( theRest.charAt( theRest.length()-1 ) != '>' ) {
            throw new RuntimeException( "\"" + theRest + "\" does not end with '>'" );
          }
          part = theRest;
          theRest = null;
        }
      }
      else if( iArray == 0 ) {
        part = theRest.substring( 0, 2 );
        theRest = part.length() == theRest.length() ? null : theRest.substring( 2 );
      }
      else if( iParam > 0 ) {
        if( iDot > 0 && iDot < iParam ) {
          part = theRest.substring( 0, iDot );
          theRest = iDot + 1 < theRest.length() ? theRest.substring( iDot + 1 ) : null;
        }
        else {
          part = theRest.substring( 0, iParam );
          theRest = iParam < theRest.length() ? theRest.substring( iParam ) : null;
        }
      }
      else if( iDot > 0 ) {
        part = theRest.substring( 0, iDot );
        theRest = iDot + 1 < theRest.length() ? theRest.substring( iDot + 1 ) : null;
      }
      else {
        part = theRest;
        theRest = null;
      }
      parts.add( StringCache.get( part ) );
    }

    return parts.toArray(new String[parts.size()]);
  }

  public static String[] getParts(String fqn) {
    String[] strings = PARTS_CACHE.get(fqn);
    if (strings == null) {
      strings = split(fqn);
      PARTS_CACHE.put(fqn, strings);
    }
    return strings;
  }

}
