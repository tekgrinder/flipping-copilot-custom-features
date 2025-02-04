package com.flippingcopilot.ui;


import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;

@Singleton
public class CopilotPanel extends JPanel {

    public final SuggestionPanel suggestionPanel;
    public final StatsPanelV2 statsPanel;

    @Inject
    public CopilotPanel(SuggestionPanel suggestionPanel,
                        StatsPanelV2 statsPanel) {
        this.statsPanel = statsPanel;
        this.suggestionPanel = suggestionPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        suggestionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        suggestionPanel.setMinimumSize(new Dimension(Integer.MIN_VALUE, 300));
        add(suggestionPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(Box.createVerticalGlue());
        add(statsPanel);
    }

    public void refresh() {
        if(!SwingUtilities.isEventDispatchThread()) {
            // we always execute this in the Swing EDT thread
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        suggestionPanel.refresh();
    }
}
