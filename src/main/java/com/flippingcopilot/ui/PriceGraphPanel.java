package com.flippingcopilot.ui;

import com.flippingcopilot.controller.PriceHistoryService;
import com.flippingcopilot.model.PriceDataPoint;
import net.runelite.client.ui.ColorScheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class PriceGraphPanel extends JPanel {
    private static JDialog currentDialog = null;
    private final ChartPanel chartPanel;
    private final JLabel titleLabel;
    private final int itemId;
    private final String itemName;
    private final TimeSeriesCollection dataset;
    private final PriceHistoryService priceHistoryService;

    @Inject
    public PriceGraphPanel(int itemId, String itemName, PriceHistoryService priceHistoryService) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.priceHistoryService = priceHistoryService;
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title panel with close button
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        titleLabel = new JLabel(itemName);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        add(titlePanel, BorderLayout.NORTH);

        // Create dataset and chart
        dataset = new TimeSeriesCollection();
        JFreeChart chart = createChart();
        
        // Create and configure chart panel
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 400));
        chartPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        chartPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        
        add(chartPanel, BorderLayout.CENTER);

        // Load real price data
        updateChartData();
    }

    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            null,           // No title (we have it in the panel)
            "Time",        // X-axis label
            "Price",       // Y-axis label
            dataset,       // Dataset
            true,          // Show legend
            true,          // Tooltips
            false          // No URLs
        );

        // Customize the chart appearance
        chart.setBackgroundPaint(ColorScheme.DARKER_GRAY_COLOR);
        chart.getLegend().setBackgroundPaint(ColorScheme.DARKER_GRAY_COLOR);
        chart.getLegend().setItemPaint(Color.WHITE);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(ColorScheme.DARK_GRAY_COLOR);
        plot.setDomainGridlinePaint(ColorScheme.LIGHT_GRAY_COLOR);
        plot.setRangeGridlinePaint(ColorScheme.LIGHT_GRAY_COLOR);
        plot.setOutlinePaint(ColorScheme.LIGHT_GRAY_COLOR);

        // Customize the axes
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        // Set the time range to show exactly 18 hours
        long now = System.currentTimeMillis();
        dateAxis.setRange(now - (18 * 60 * 60 * 1000), now);
        dateAxis.setLabelPaint(Color.WHITE);
        dateAxis.setTickLabelPaint(Color.WHITE);

        plot.getRangeAxis().setLabelPaint(Color.WHITE);
        plot.getRangeAxis().setTickLabelPaint(Color.WHITE);

        // Customize the line renderer
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.GREEN);  // High price
        renderer.setSeriesPaint(1, Color.RED);    // Low price
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setDefaultShapesVisible(false);  // No data point markers
        renderer.setDrawOutlines(false);
        renderer.setUseFillPaint(false);

        return chart;
    }

    private void updateChartData() {
        try {
            log.debug("Updating chart data for item {}", itemId);
            List<PriceDataPoint> priceHistory = priceHistoryService.getPriceHistory(itemId);
            log.debug("Received {} price points for item {}", priceHistory.size(), itemId);
            
            TimeSeries highPriceSeries = new TimeSeries("High Price");
            TimeSeries lowPriceSeries = new TimeSeries("Low Price");

            for (PriceDataPoint point : priceHistory) {
                Second second = new Second(Date.from(point.getTimestamp()));
                highPriceSeries.add(second, point.getAvgHighPrice());
                lowPriceSeries.add(second, point.getAvgLowPrice());
            }

            dataset.removeAllSeries();
            dataset.addSeries(highPriceSeries);
            dataset.addSeries(lowPriceSeries);
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
                Point parentLocation = parentFrame.getLocationOnScreen();
                Dimension parentSize = parentFrame.getSize();
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
                // Fall back to center of parent frame
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