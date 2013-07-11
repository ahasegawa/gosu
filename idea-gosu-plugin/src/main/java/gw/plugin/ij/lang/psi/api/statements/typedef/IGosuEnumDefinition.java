/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.lang.psi.api.statements.typedef;

import com.intellij.psi.StubBasedPsiElement;
import gw.plugin.ij.lang.psi.api.types.IGosuTypeParameterListOwner;
import gw.plugin.ij.lang.psi.stubs.GosuTypeDefinitionStub;

public interface IGosuEnumDefinition extends IGosuTypeDefinition, IGosuTypeParameterListOwner, StubBasedPsiElement<GosuTypeDefinitionStub> {
}
