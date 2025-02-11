package com.flippingcopilot.ui;

import com.flippingcopilot.controller.PriceHistoryService;
import com.flippingcopilot.model.PriceDataPoint;
import net.runelite.client.ui.ColorScheme;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

@Slf4j
public class PriceGraphPanel extends JPanel {
    private static JDialog currentDialog = null;
    private final PriceTimeChart chartPanel;
    private final int itemId;
    private final String itemName;
    private final PriceHistoryService priceHistoryService;

    @Inject
    public PriceGraphPanel(int itemId, String itemName, PriceHistoryService priceHistoryService) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.priceHistoryService = priceHistoryService;
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create and configure chart panel
        chartPanel = new PriceTimeChart(itemName);
        chartPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        chartPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        
        add(chartPanel, BorderLayout.CENTER);

        // Load real price data
        updateChartData();
    }

    private void updateChartData() {
        try {
            log.debug("Updating chart data for item {}", itemId);
            List<PriceDataPoint> priceHistory = priceHistoryService.getPriceHistory(itemId);
            log.debug("Received {} price points for item {}", priceHistory.size(), itemId);
            
            chartPanel.updateData(priceHistory);
            log.debug("Chart data updated successfully");
        } catch (Exception e) {
            log.error("Error updating chart data for item " + itemId, e);
        }
    }

    public static void showPanel(Component parent, PriceGraphPanel graphPanel) {
        try {
            log.debug("Showing price graph panel for item: {}", graphPanel.itemName);
            
            // If there's already a dialog showing, dispose it
            if (currentDialog != null) {
                log.debug("Disposing existing dialog");
                currentDialog.dispose();
            }

            // Create new dialog
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(parent);
            if (parentFrame == null) {
                log.error("Could not find parent frame");
                return;
            }

            JDialog dialog = new JDialog(parentFrame);
            dialog.setUndecorated(true); // Remove window decorations
            dialog.setBackground(new Color(0, 0, 0, 0)); // Make dialog background transparent

            // Add a border to make it look like a floating panel
            graphPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 1),
                graphPanel.getBorder()
            ));

            dialog.add(graphPanel);
            dialog.pack();

            // Position the dialog within the RuneLite window
            try {
                Frame frame = (Frame) SwingUtilities.getWindowAncestor(parent);
                Point parentLocation = frame.getLocationOnScreen();
                Dimension parentSize = frame.getSize();
                Dimension dialogSize = dialog.getSize();
                Point buttonLocation = parent.getLocationOnScreen();

                // Calculate initial position (centered above the button)
                int x = buttonLocation.x - (dialogSize.width - parent.getWidth()) / 2;
                int y = buttonLocation.y - dialogSize.height - 5; // 5px gap

                // Ensure the dialog stays within the RuneLite window bounds
                x = Math.max(parentLocation.x, Math.min(x, parentLocation.x + parentSize.width - dialogSize.width));
                y = Math.max(parentLocation.y, Math.min(y, parentLocation.y + parentSize.height - dialogSize.height));

                dialog.setLocation(x, y);
                log.debug("Positioned dialog at ({}, {})", x, y);
            } catch (IllegalComponentStateException e) {
                log.error("Error calculating dialog position", e);
                dialog.setLocationRelativeTo(parent);
            }

            // Add window listener to handle dialog disposal
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDeactivated(WindowEvent e) {
                    dialog.dispose();
                    currentDialog = null;
                }
            });

            currentDialog = dialog;
            dialog.setVisible(true);
            log.debug("Price graph panel shown successfully");
        } catch (Exception e) {
            log.error("Error showing price graph panel", e);
        }
    }
}