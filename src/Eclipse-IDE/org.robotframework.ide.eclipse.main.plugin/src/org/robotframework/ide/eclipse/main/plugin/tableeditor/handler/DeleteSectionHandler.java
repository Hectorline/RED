package org.robotframework.ide.eclipse.main.plugin.tableeditor.handler;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.robotframework.ide.eclipse.main.plugin.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.RobotSuiteFileSection;
import org.robotframework.ide.eclipse.main.plugin.cmd.DeleteSectionCommand;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotEditorCommandsStack;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.SectionEditorPage;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.handler.DeleteSectionHandler.E4DeleteSection;

import com.google.common.base.Optional;

public class DeleteSectionHandler extends DIHandler<E4DeleteSection> {

    public DeleteSectionHandler() {
        super(E4DeleteSection.class);
    }

    public static class E4DeleteSection {

        @Execute
        public Object deleteVariables(@Named(ISources.ACTIVE_EDITOR_NAME) final RobotFormEditor editor,
                final RobotEditorCommandsStack stack) {
            final IEditorPart activeEditor = editor.getActiveEditor();

            if (activeEditor instanceof SectionEditorPage) {
                final SectionEditorPage page = (SectionEditorPage) activeEditor;
                final Optional<RobotElement> section = page.provideSection(editor.provideSuiteModel());
                if (section.isPresent()) {
                    final List<RobotSuiteFileSection> sectionsToRemove = Arrays.asList((RobotSuiteFileSection)section.get());
                    stack.execute(new DeleteSectionCommand(sectionsToRemove));
                }
            }
            return null;
        }
    }
}
