package com.flippingcopilot.ui;

import net.runelite.client.ui.ColorScheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

public class PriceGraphPanel extends JPanel {
    private static JDialog currentDialog = null;
    private final ChartPanel chartPanel;
    private final JLabel titleLabel;
    private final int itemId;
    private final String itemName;
    private final TimeSeriesCollection dataset;

    public PriceGraphPanel(int itemId, String itemName) {
        this.itemId = itemId;
        this.itemName = itemName;
        
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
        chartPanel.setPreferredSize(new Dimension(300, 200));
        chartPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        chartPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        
        add(chartPanel, BorderLayout.CENTER);

        // Add some dummy data for testing
        addDummyData();
    }

    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            null,           // No title (we have it in the panel)
            "Time",        // X-axis label
            "Price",       // Y-axis label
            dataset,       // Dataset
            false,         // No legend
            true,          // Tooltips
            false          // No URLs
        );

        // Customize the chart appearance
        chart.setBackgroundPaint(ColorScheme.DARKER_GRAY_COLOR);
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(ColorScheme.DARK_GRAY_COLOR);
        plot.setDomainGridlinePaint(ColorScheme.LIGHT_GRAY_COLOR);
        plot.setRangeGridlinePaint(ColorScheme.LIGHT_GRAY_COLOR);
        plot.setOutlinePaint(ColorScheme.LIGHT_GRAY_COLOR);

        // Customize the line renderer
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        return chart;
    }

    private void addDummyData() {
        // This is temporary for testing - will be replaced with real data later
        TimeSeries series = new TimeSeries("Price");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            series.add(new Second(new Date(now - i * 3600000)), 1000 + Math.random() * 100);
        }
        dataset.addSeries(series);
    }

    public static void showPanel(Component parent, int itemId, String itemName) {
        // If there's already a dialog showing, dispose it
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        // Create new dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent));
        dialog.setUndecorated(true); // Remove window decorations
        dialog.setBackground(new Color(0, 0, 0, 0)); // Make dialog background transparent

        // Create the graph panel
        PriceGraphPanel graphPanel = new PriceGraphPanel(itemId, itemName);
        
        // Add a border to make it look like a floating panel
        graphPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 1),
            graphPanel.getBorder()
        ));

        dialog.add(graphPanel);
        dialog.pack();

        // Position the dialog near the parent component
        Point location = parent.getLocationOnScreen();
        dialog.setLocation(
            location.x - dialog.getWidth() / 2,
            location.y - dialog.getHeight()
        );

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
    }
}