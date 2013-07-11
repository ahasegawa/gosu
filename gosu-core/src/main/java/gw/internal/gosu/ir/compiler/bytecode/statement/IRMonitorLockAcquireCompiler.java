/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.ir.compiler.bytecode.statement;

import gw.internal.gosu.ir.compiler.bytecode.AbstractBytecodeCompiler;
import gw.internal.gosu.ir.compiler.bytecode.IRBytecodeContext;
import gw.internal.gosu.ir.compiler.bytecode.IRBytecodeCompiler;
import gw.lang.ir.statement.IRMonitorLockAcquireStatement;
import gw.internal.ext.org.objectweb.asm.Opcodes;

public class IRMonitorLockAcquireCompiler extends AbstractBytecodeCompiler
{
  public static void compile( IRMonitorLockAcquireStatement statement, IRBytecodeContext context )
  {
    IRBytecodeCompiler.compileIRExpression( statement.getMonitoredObject(), context );
    context.getMv().visitInsn( Opcodes.MONITORENTER );
  }
}
