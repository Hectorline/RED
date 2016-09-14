/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.handler;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ModelType;
import org.robotframework.ide.eclipse.main.plugin.documentation.DocumentationViewPartListener;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotDefinitionSetting;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotEditorSources;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.handler.ShowInDocViewHandler.E4ShowInDocViewHandler;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourceEditor;
import org.robotframework.ide.eclipse.main.plugin.views.DocumentationView;
import org.robotframework.red.commands.DIParameterizedHandler;
import org.robotframework.red.viewers.Selections;

/**
 * @author mmarzec
 *
 */
public class ShowInDocViewHandler extends DIParameterizedHandler<E4ShowInDocViewHandler> {

    public ShowInDocViewHandler() {
        super(E4ShowInDocViewHandler.class);
    }

    public static class E4ShowInDocViewHandler {

        @Execute
        public void showInDocView(@Optional @Named(Selections.SELECTION) final IStructuredSelection selection,
                @Named(ISources.ACTIVE_EDITOR_NAME) final RobotFormEditor editor,
                @Named(RobotEditorSources.SUITE_FILE_MODEL) final RobotSuiteFile suiteModel) {

            initDocumentationView();

            final DocumentationViewPartListener documentationViewPartListener = editor
                    .getDocumentationViewPartListener();
            if (documentationViewPartListener == null) {
                return;
            }
            final DocumentationView view = documentationViewPartListener.getView();
            if (view == null) {
                return;
            }

            if (selection != null) {
                final com.google.common.base.Optional<RobotFileInternalElement> selectedElement = Selections
                        .getOptionalFirstElement(selection, RobotFileInternalElement.class);
                if (selectedElement.isPresent()) {
                    showDoc(view, selectedElement.get());
                }
            } else if (editor.getActiveEditor() instanceof SuiteSourceEditor) {
                final SuiteSourceEditor sourceEditor = (SuiteSourceEditor) editor.getActiveEditor();
                final int offset = sourceEditor.getViewer().getTextWidget().getCaretOffset();
                final com.google.common.base.Optional<? extends RobotElement> element = suiteModel.findElement(offset);
                if (element.isPresent()) {
                    showDoc(view, (RobotFileInternalElement) element.get());
                }
            }

        }

        private void showDoc(final DocumentationView view, final RobotFileInternalElement robotFileInternalElement) {
            final Object linkedElement = robotFileInternalElement.getLinkedElement();
            if (linkedElement != null && linkedElement instanceof AModelElement<?>) {
                final ModelType modelType = ((AModelElement<?>) linkedElement).getModelType();

                if (modelType == ModelType.USER_KEYWORD_EXECUTABLE_ROW
                        || modelType == ModelType.TEST_CASE_EXECUTABLE_ROW) {
                    view.setShowLibdocEnabled();
                    view.showLibdoc(robotFileInternalElement);
                } else {
                    RobotCodeHoldingElement<?> parent = null;
                    if (modelType == ModelType.USER_KEYWORD || modelType == ModelType.TEST_CASE) {
                        parent = (RobotCodeHoldingElement<?>) robotFileInternalElement;
                    } else {
                        final RobotElement robotElement = robotFileInternalElement.getParent();
                        if (robotElement instanceof RobotCodeHoldingElement<?>) {
                            parent = (RobotCodeHoldingElement<?>) robotFileInternalElement.getParent();
                        }
                    }
                    if (parent != null) {
                        final RobotDefinitionSetting docSettingFromParent = parent
                                .findSetting(ModelType.TEST_CASE_DOCUMENTATION, ModelType.USER_KEYWORD_DOCUMENTATION);
                        view.showDocumentation(docSettingFromParent);
                    }
                }
            }
        }

        private void initDocumentationView() {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            workbench.getDisplay().syncExec(new Runnable() {

                @Override
                public void run() {
                    final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
                    if (activeWorkbenchWindow != null) {
                        final IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
                        if (page != null) {
                            final IViewPart docViewPart = page.findView(DocumentationView.ID);
                            if (docViewPart == null || !page.isPartVisible(docViewPart)) {
                                try {
                                    page.showView(DocumentationView.ID);
                                    page.activate(page.getActiveEditor()); // activate current RobotFormEditor to reset DocViewPartListener instance, partActivated method will be invoked 
                                } catch (final PartInitException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}