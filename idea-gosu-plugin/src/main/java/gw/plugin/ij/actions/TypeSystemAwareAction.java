/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import gw.internal.gosu.parser.TypeSystemState;
import gw.lang.reflect.TypeSystem;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class TypeSystemAwareAction extends AnAction {

  protected TypeSystemAwareAction() {
  }

  protected TypeSystemAwareAction(@Nullable String text) {
    super(text);
  }

  protected TypeSystemAwareAction(String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Override
  public final void update(AnActionEvent e) {
    if (TypeSystem.getState() == TypeSystemState.STARTED) {
      updateImpl(e);
    } else {
      final Presentation presentation = e.getPresentation();
      presentation.setVisible(false);
      presentation.setEnabled(false);
    }
  }

  protected void updateImpl(AnActionEvent e) {

  }

}
