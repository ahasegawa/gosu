/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.refactor.introduceVariable;

import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.scope.processor.VariablesProcessor;
import com.intellij.psi.scope.util.PsiScopesUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * copy of Intellij ReassignVariableUtil
 */
public class GosuReassignVariableUtil {

  static final Key<SmartPsiElementPointer<PsiDeclarationStatement>> DECLARATION_KEY = Key.create("var.type");
  static final Key<RangeMarker[]> OCCURRENCES_KEY = Key.create("occurrences");

  private GosuReassignVariableUtil() {
  }

  static boolean reassign(final Editor editor) {
    final SmartPsiElementPointer<PsiDeclarationStatement> pointer = editor.getUserData(DECLARATION_KEY);
    final PsiDeclarationStatement declaration = pointer != null ? pointer.getElement() : null;
    final PsiType type = getVariableType(declaration);
    if (type != null) {
      VariablesProcessor proc = findVariablesOfType(declaration, type);
      if (proc.size() > 0) {

        if (proc.size() == 1) {
          replaceWithAssignment(declaration, proc.getResult(0), editor);
          return true;
        }

        final DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < proc.size(); i++) {
          model.addElement(proc.getResult(i));
        }
        final JList list = new JBList(model);
        list.setCellRenderer(new ListCellRendererWrapper(new DefaultListCellRenderer()) {
          @Override
          public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            if (value instanceof PsiVariable) {
              setText(((PsiVariable) value).getName());
              setIcon(((PsiVariable) value).getIcon(0));
            }
          }
        });


        final VisualPosition visualPosition = editor.getCaretModel().getVisualPosition();
        final Point point = editor.visualPositionToXY(new VisualPosition(visualPosition.line + 1, visualPosition.column));
        JBPopupFactory.getInstance().createListPopupBuilder(list)
                .setTitle("Choose variable to reassign")
                .setRequestFocus(true)
                .setItemChoosenCallback(new Runnable() {
                  public void run() {
                    replaceWithAssignment(declaration, (PsiVariable) list.getSelectedValue(), editor);
                  }
                }).createPopup().show(new RelativePoint(editor.getContentComponent(), point));
      }

      return true;
    }
    return false;
  }

  @Nullable
  static PsiType getVariableType(@Nullable PsiDeclarationStatement declaration) {
    if (declaration != null) {
      final PsiElement[] declaredElements = declaration.getDeclaredElements();
      if (declaredElements.length > 0 && declaredElements[0] instanceof PsiVariable) {
        return ((PsiVariable) declaredElements[0]).getType();
      }
    }
    return null;
  }

  static VariablesProcessor findVariablesOfType(final PsiDeclarationStatement declaration, final PsiType type) {
    VariablesProcessor proc = new VariablesProcessor(false) {
      @Override
      protected boolean check(PsiVariable var, ResolveState state) {
        for (PsiElement element : declaration.getDeclaredElements()) {
          if (element == var) {
            return false;
          }
        }
        return TypeConversionUtil.isAssignable(var.getType(), type);
      }
    };
    PsiElement scope = declaration;
    while (scope != null) {
      if (scope instanceof PsiFile || scope instanceof PsiMethod || scope instanceof PsiClassInitializer) {
        break;
      }
      scope = scope.getParent();
    }
    if (scope == null) {
      return proc;
    }
    PsiScopesUtil.treeWalkUp(proc, declaration, scope);
    return proc;
  }

  static void replaceWithAssignment(final PsiDeclarationStatement declaration, final PsiVariable variable, final Editor editor) {
    final PsiVariable var = (PsiVariable) declaration.getDeclaredElements()[0];
    final PsiExpression initializer = var.getInitializer();
    new WriteCommandAction(declaration.getProject()) {
      @Override
      protected void run(Result result) throws Throwable {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(variable.getProject());
        final String chosenVariableName = variable.getName();
        //would generate red code for final variables
        PsiElement newDeclaration = elementFactory.createStatementFromText(chosenVariableName + " = " + initializer.getText() + ";",
                declaration);
        newDeclaration = declaration.replace(newDeclaration);
        final PsiFile containingFile = newDeclaration.getContainingFile();
        final RangeMarker[] occurrenceMarkers = editor.getUserData(OCCURRENCES_KEY);
        if (occurrenceMarkers != null) {
          for (RangeMarker marker : occurrenceMarkers) {
            final PsiElement refVariableElement = containingFile.findElementAt(marker.getStartOffset());
            final PsiExpression expression = PsiTreeUtil.getParentOfType(refVariableElement, PsiReferenceExpression.class);
            if (expression != null) {
              expression.replace(elementFactory.createExpressionFromText(chosenVariableName, newDeclaration));
            }
          }
        }
      }
    }.execute();
    finishTemplate(editor);
  }

  private static void finishTemplate(Editor editor) {
    final TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    final InplaceRefactoring renamer = editor.getUserData(InplaceRefactoring.INPLACE_RENAMER);
    if (templateState != null && renamer != null) {
      templateState.gotoEnd(true);
      editor.putUserData(InplaceRefactoring.INPLACE_RENAMER, null);
    }
  }
}
