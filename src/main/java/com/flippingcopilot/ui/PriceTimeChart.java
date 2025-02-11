package com.flippingcopilot.ui;

import com.flippingcopilot.model.PriceDataPoint;
import net.runelite.client.ui.ColorScheme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PriceTimeChart extends JPanel {
    private static final int PADDING_TOP = 20;
    private static final int PADDING_BOTTOM = 40;
    private static final int PADDING_LEFT = 60;
    private static final int PADDING_RIGHT = 20;
    private static final int Y_AXIS_TICKS = 5;
    private static final int X_AXIS_TICKS = 6;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    private static final Color HIGH_PRICE_COLOR = new Color(0, 180, 0);
    private static final Color LOW_PRICE_COLOR = new Color(180, 0, 0);
    private static final Stroke PRICE_LINE_STROKE = new BasicStroke(2f);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 10);

    private List<PriceDataPoint> priceData = new ArrayList<>();
    private long minTime;
    private long maxTime;
    private long minPrice;
    private long maxPrice;
    private final String itemName;

    public PriceTimeChart(String itemName) {
        this.itemName = itemName;
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setPreferredSize(new Dimension(600, 400));
    }

    public void updateData(List<PriceDataPoint> newData) {
        if (newData.isEmpty()) {
            return;
        }

        this.priceData = new ArrayList<>(newData);
        
        // Calculate data ranges
        minTime = priceData.get(0).getTimestamp().getEpochSecond();
        maxTime = minTime;
        minPrice = priceData.get(0).getAvgLowPrice();
        maxPrice = priceData.get(0).getAvgHighPrice();

        for (PriceDataPoint point : priceData) {
            long time = point.getTimestamp().getEpochSecond();
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
            minPrice = Math.min(minPrice, point.getAvgLowPrice());
            minPrice = Math.min(minPrice, point.getAvgHighPrice());
            maxPrice = Math.max(maxPrice, point.getAvgLowPrice());
            maxPrice = Math.max(maxPrice, point.getAvgHighPrice());
        }

        // Add some padding to price range
        long priceRange = maxPrice - minPrice;
        minPrice -= priceRange * 0.05;
        maxPrice += priceRange * 0.05;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int chartWidth = width - PADDING_LEFT - PADDING_RIGHT;
        int chartHeight = height - PADDING_TOP - PADDING_BOTTOM;

        // Draw title
        g2.setColor(Color.WHITE);
        g2.setFont(LABEL_FONT.deriveFont(Font.BOLD, 12f));
        String title = itemName + " Price History";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, PADDING_TOP - 5);

        if (priceData.isEmpty()) {
            return;
        }

        // Draw axes
        g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g2.drawLine(PADDING_LEFT, height - PADDING_BOTTOM, width - PADDING_RIGHT, height - PADDING_BOTTOM); // X axis
        g2.drawLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, height - PADDING_BOTTOM); // Y axis

        // Draw Y axis labels and grid lines
        g2.setFont(LABEL_FONT);
        fm = g2.getFontMetrics();
        for (int i = 0; i <= Y_AXIS_TICKS; i++) {
            int y = height - PADDING_BOTTOM - (i * chartHeight / Y_AXIS_TICKS);
            long price = minPrice + (i * (maxPrice - minPrice) / Y_AXIS_TICKS);
            String label = String.valueOf(price);
            g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
            g2.drawString(label, PADDING_LEFT - fm.stringWidth(label) - 5, y + fm.getAscent() / 2);
            
            // Grid line
            g2.setColor(ColorScheme.DARK_GRAY_COLOR.darker());
            g2.drawLine(PADDING_LEFT, y, width - PADDING_RIGHT, y);
        }

        // Draw X axis labels and grid lines
        for (int i = 0; i <= X_AXIS_TICKS; i++) {
            int x = PADDING_LEFT + (i * chartWidth / X_AXIS_TICKS);
            long time = minTime + (i * (maxTime - minTime) / X_AXIS_TICKS);
            String label = TIME_FORMAT.format(Date.from(Instant.ofEpochSecond(time)));
            g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
            g2.drawString(label, x - fm.stringWidth(label) / 2, height - PADDING_BOTTOM + fm.getHeight() + 5);
            
            // Grid line
            g2.setColor(ColorScheme.DARK_GRAY_COLOR.darker());
            g2.drawLine(x, PADDING_TOP, x, height - PADDING_BOTTOM);
        }

        // Draw price lines
        g2.setStroke(PRICE_LINE_STROKE);
        drawPriceLine(g2, chartWidth, chartHeight, true); // High prices
        drawPriceLine(g2, chartWidth, chartHeight, false); // Low prices
    }

    private void drawPriceLine(Graphics2D g2, int chartWidth, int chartHeight, boolean isHighPrice) {
        Path2D path = new Path2D.Float();
        boolean first = true;

        for (PriceDataPoint point : priceData) {
            double x = PADDING_LEFT + ((point.getTimestamp().getEpochSecond() - minTime) * chartWidth / (maxTime - minTime));
            double price = isHighPrice ? point.getAvgHighPrice() : point.getAvgLowPrice();
            double y = getHeight() - PADDING_BOTTOM - ((price - minPrice) * chartHeight / (maxPrice - minPrice));

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        g2.setColor(isHighPrice ? HIGH_PRICE_COLOR : LOW_PRICE_COLOR);
        g2.draw(path);
    }
}