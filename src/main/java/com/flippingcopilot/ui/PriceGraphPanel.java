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

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
        chartPanel.setPreferredSize(new Dimension(300, 200));
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
        dateAxis.setDateFormatOverride(new SimpleDateFormat("MMM dd HH:mm"));
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
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);

        return chart;
    }

    private void updateChartData() {
        List<PriceDataPoint> priceHistory = priceHistoryService.getPriceHistory(itemId);
        
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
    }

    public static void showPanel(Component parent, PriceGraphPanel graphPanel) {
        // If there's already a dialog showing, dispose it
        if (currentDialog != null) {
            currentDialog.dispose();
        }

        // Create new dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent));
        dialog.setUndecorated(true); // Remove window decorations
        dialog.setBackground(new Color(0, 0, 0, 0)); // Make dialog background transparent

        // Use the provided graph panel
        
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