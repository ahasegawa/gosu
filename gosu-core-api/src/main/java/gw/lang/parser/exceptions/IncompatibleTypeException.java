/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.parser.exceptions;

public class IncompatibleTypeException extends RuntimeException
{
  public IncompatibleTypeException()
  {
  }

  public IncompatibleTypeException( String message )
  {
    super( message );
  }

  public IncompatibleTypeException( Throwable cause )
  {
    super( cause );
  }

  public IncompatibleTypeException( String message, Throwable cause )
  {
    super( message, cause );
  }
}
