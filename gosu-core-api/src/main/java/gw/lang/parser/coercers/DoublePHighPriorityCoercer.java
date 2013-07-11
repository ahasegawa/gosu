/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.lang.parser.coercers;

public class DoublePHighPriorityCoercer extends BasePHighPriorityCoercer
{
  private static final DoublePHighPriorityCoercer INSTANCE = new DoublePHighPriorityCoercer();

  public DoublePHighPriorityCoercer()
  {
    super( BasePrimitiveCoercer.DoublePCoercer, MAX_PRIORITY );
  }

  public static DoublePHighPriorityCoercer instance()
  {
    return INSTANCE;
  }
}