/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.red.nattable.configs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Condition;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.variables.nattable.TableThemes.TableTheme;
import org.robotframework.red.graphics.ColorsManager;
import org.robotframework.red.nattable.painter.UnderlinedCellBackgroundPainter;

public class RowHeaderConfigurationTest {

    @Test
    public void configurationCheck() {
        final Font fontInUse = Display.getCurrent().getSystemFont();
        final Color bgColorInUse = ColorsManager.getColor(200, 200, 200);
        final Color fgColorInUse = ColorsManager.getColor(100, 100, 100);

        final TableTheme theme = mock(TableTheme.class);
        when(theme.getFont()).thenReturn(fontInUse);
        when(theme.getHeadersBackground()).thenReturn(bgColorInUse);
        when(theme.getHeadersForeground()).thenReturn(fgColorInUse);
        when(theme.getHeadersUnderlineColor()).thenReturn(ColorsManager.getColor(200, 0, 0));

        final RowHeaderConfiguration configuration = new RowHeaderConfiguration(theme);

        assertThat(configuration).has(font(fontInUse));
        assertThat(configuration).has(background(bgColorInUse));
        assertThat(configuration).has(foreground(fgColorInUse));
        assertThat(configuration.cellPainter).isInstanceOf(UnderlinedCellBackgroundPainter.class);
    }

    private Condition<RowHeaderConfiguration> background(final Color bgColorInUse) {
        return new Condition<RowHeaderConfiguration>() {

            @Override
            public boolean matches(final RowHeaderConfiguration config) {
                return config.bgColor.equals(bgColorInUse);
            }
        };
    }

    private Condition<RowHeaderConfiguration> foreground(final Color fgColorInUse) {
        return new Condition<RowHeaderConfiguration>() {

            @Override
            public boolean matches(final RowHeaderConfiguration config) {
                return config.fgColor.equals(fgColorInUse);
            }
        };
    }

    private static Condition<RowHeaderConfiguration> font(final Font font) {
        return new Condition<RowHeaderConfiguration>() {

            @Override
            public boolean matches(final RowHeaderConfiguration config) {
                return config.font.equals(font);
            }
        };
    }
}
