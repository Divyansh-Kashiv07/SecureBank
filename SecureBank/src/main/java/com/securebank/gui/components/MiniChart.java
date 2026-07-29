package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * MiniChart — a Java2D bar chart component for the dashboard (dark theme).
 *
 * Draws a bar chart showing transaction amounts with HSBC Red gradient bars.
 * No external charting library required — uses Graphics2D directly.
 *
 * RUBRIC: Unit 5 — Swing GUI component with custom painting.
 */
public class MiniChart extends JPanel {

    /** The data values to display as bars */
    private double[] values;

    /** Labels for the X-axis (one per bar) */
    private String[] labels;

    /** Chart title */
    private String title;

    /** Bar color — Theme primary accent */
    private Color barColor = ThemeManager.getPrimaryAccentColor();

    /** Background color for chart area */
    private Color chartBg = ThemeManager.getBackgroundColor();

    /**
     * Creates a mini chart with the given title.
     */
    public MiniChart(String title) {
        this.title = title;
        this.values = new double[]{0};
        this.labels = new String[]{""};
        setOpaque(false);
        setPreferredSize(new Dimension(300, 180));
    }

    /**
     * Sets the chart data.
     */
    public void setData(double[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        repaint();
    }

    /**
     * Sets the bar color.
     */
    public void setBarColor(Color color) {
        this.barColor = color;
        repaint();
    }

    /**
     * Custom painting — draws the bar chart using Java2D with dark theme.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chart area margins
        int leftMargin = 50;
        int rightMargin = 15;
        int topMargin = 30;
        int bottomMargin = 35;

        int chartW = w - leftMargin - rightMargin;
        int chartH = h - topMargin - bottomMargin;

        // Draw title
        g2d.setFont(ThemeManager.getBoldFont(13));
        g2d.setColor(ThemeManager.getTextLightColor());
        g2d.drawString(title, leftMargin, 20);

        // Draw chart background — dark
        g2d.setColor(chartBg);
        g2d.fillRoundRect(leftMargin, topMargin, chartW, chartH, 6, 6);

        // Draw border
        g2d.setColor(ThemeManager.getBorderColor());
        g2d.drawRoundRect(leftMargin, topMargin, chartW, chartH, 6, 6);

        if (values == null || values.length == 0) {
            g2d.setColor(ThemeManager.getTextMutedColor());
            g2d.setFont(ThemeManager.getItalicFont(12));
            g2d.drawString("No data available", leftMargin + 20, topMargin + chartH / 2);
            g2d.dispose();
            return;
        }

        // Find max value for scaling
        double maxVal = 1;
        for (double v : values) {
            if (v > maxVal) maxVal = v;
        }

        // Draw bars
        int barCount = values.length;
        int barWidth = Math.max(10, (chartW - 20) / barCount - 6);
        int gap = 6;

        for (int i = 0; i < barCount; i++) {
            int barH = (int) ((values[i] / maxVal) * (chartH - 10));
            int x = leftMargin + 10 + i * (barWidth + gap);
            int y = topMargin + chartH - barH;

            // Draw bar with HSBC Red gradient
            GradientPaint gradient = new GradientPaint(
                    x, y, barColor,
                    x, y + barH, barColor.darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x, y, barWidth, barH, 4, 4);

            // Draw label below bar
            if (labels != null && i < labels.length) {
                g2d.setColor(ThemeManager.getTextMutedColor());
                g2d.setFont(ThemeManager.getFont(10));
                FontMetrics fm = g2d.getFontMetrics();
                int labelW = fm.stringWidth(labels[i]);
                g2d.drawString(labels[i], x + (barWidth - labelW) / 2,
                        topMargin + chartH + 15);
            }

            // Draw value above bar
            g2d.setColor(ThemeManager.getTextLightColor());
            g2d.setFont(ThemeManager.getFont(9));
            String valStr = String.format("%.0f", values[i]);
            FontMetrics fm = g2d.getFontMetrics();
            int valW = fm.stringWidth(valStr);
            if (barH > 15) {
                g2d.drawString(valStr, x + (barWidth - valW) / 2, y - 3);
            }
        }

        // Draw Y-axis labels
        g2d.setColor(ThemeManager.getTextMutedColor());
        g2d.setFont(ThemeManager.getFont(10));
        for (int i = 0; i <= 4; i++) {
            double val = maxVal * i / 4;
            int y = topMargin + chartH - (int) ((val / maxVal) * (chartH - 10));
            g2d.drawString(String.format("%.0f", val), 5, y + 4);

            // Grid line
            g2d.setColor(ThemeManager.getBorderColor());
            g2d.drawLine(leftMargin, y, leftMargin + chartW, y);
            g2d.setColor(ThemeManager.getTextMutedColor());
        }

        g2d.dispose();
    }
}
