package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * MiniChart — a production-grade bar chart component.
 *
 * Features:
 * - Gradient bars with rounded tops
 * - Smooth hover effects
 * - Anti-aliased rendering
 * - Professional grid lines
 * - Value labels above bars
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

    /** Bar end color for gradient */
    private Color barColorEnd = ThemeManager.getAccentGradientEnd();

    /** Background color for chart area */
    private Color chartBg = ThemeManager.getCardColor();

    /** Hover index (-1 = none) */
    private int hoverIndex = -1;

    /**
     * Creates a mini chart with the given title.
     */
    public MiniChart(String title) {
        this.title = title;
        this.values = new double[]{0};
        this.labels = new String[]{""};
        setOpaque(false);
        setPreferredSize(new Dimension(300, 180));

        // Track mouse for hover effects
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int oldHover = hoverIndex;
                hoverIndex = getBarIndexAt(e.getX(), e.getY());
                if (oldHover != hoverIndex) {
                    repaint();
                }
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        });
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
     * Gets the bar index at the given x coordinate.
     */
    private int getBarIndexAt(int x, int y) {
        if (values == null || values.length == 0) return -1;

        int leftMargin = 50;
        int rightMargin = 15;
        int topMargin = 30;
        int bottomMargin = 35;

        int chartW = getWidth() - leftMargin - rightMargin;
        int chartH = getHeight() - topMargin - bottomMargin;

        int barCount = values.length;
        int barWidth = Math.max(10, (chartW - 20) / barCount - 6);
        int gap = 6;

        for (int i = 0; i < barCount; i++) {
            int bx = leftMargin + 10 + i * (barWidth + gap);
            if (x >= bx && x <= bx + barWidth && y >= topMargin && y <= topMargin + chartH) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Custom painting — draws the bar chart using Java2D with dark theme.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

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
        g2.setFont(ThemeManager.getBoldFont(13));
        g2.setColor(ThemeManager.getTextLightColor());
        g2.drawString(title, leftMargin, 20);

        // Draw chart background — glass effect
        g2.setColor(ThemeManager.getGlassColor());
        g2.fill(new RoundRectangle2D.Float(leftMargin, topMargin, chartW, chartH, 8, 8));

        // Draw border
        g2.setColor(ThemeManager.getGlassBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(leftMargin, topMargin, chartW, chartH, 8, 8));

        if (values == null || values.length == 0) {
            g2.setColor(ThemeManager.getTextMutedColor());
            g2.setFont(ThemeManager.getItalicFont(12));
            g2.drawString("No data available", leftMargin + 20, topMargin + chartH / 2);
            g2.dispose();
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

            boolean isHovered = (i == hoverIndex);

            // Draw bar with gradient
            GradientPaint gradient = new GradientPaint(
                    x, y, isHovered ? ThemeManager.lighten(barColor, 0.2f) : barColor,
                    x, y + barH, isHovered ? barColor : barColorEnd);
            g2.setPaint(gradient);

            // Rounded top bar
            if (barH > 4) {
                g2.fill(new RoundRectangle2D.Float(x, y, barWidth, barH, 4, 4));
            } else if (barH > 0) {
                g2.fillRect(x, y, barWidth, barH);
            }

            // Hover highlight
            if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fill(new RoundRectangle2D.Float(x, y, barWidth, barH, 4, 4));
            }

            // Draw label below bar
            if (labels != null && i < labels.length) {
                g2.setColor(ThemeManager.getTextMutedColor());
                g2.setFont(ThemeManager.getFont(10));
                FontMetrics fm = g2.getFontMetrics();
                int labelW = fm.stringWidth(labels[i]);
                g2.drawString(labels[i], x + (barWidth - labelW) / 2,
                        topMargin + chartH + 15);
            }

            // Draw value above bar
            g2.setColor(isHovered ? ThemeManager.getTextLightColor() :
                    ThemeManager.getTextMutedColor());
            g2.setFont(ThemeManager.getBoldFont(9));
            String valStr = String.format("%.0f", values[i]);
            FontMetrics fm = g2.getFontMetrics();
            int valW = fm.stringWidth(valStr);
            if (barH > 15) {
                g2.drawString(valStr, x + (barWidth - valW) / 2, y - 5);
            }
        }

        // Draw Y-axis labels
        g2.setColor(ThemeManager.getTextMutedColor());
        g2.setFont(ThemeManager.getFont(10));
        for (int i = 0; i <= 4; i++) {
            double val = maxVal * i / 4;
            int y = topMargin + chartH - (int) ((val / maxVal) * (chartH - 10));
            g2.drawString(String.format("%.0f", val), 5, y + 4);

            // Grid line
            g2.setColor(new Color(ThemeManager.getBorderColor().getRed(),
                    ThemeManager.getBorderColor().getGreen(),
                    ThemeManager.getBorderColor().getBlue(), 60));
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(leftMargin, y, leftMargin + chartW, y);
            g2.setColor(ThemeManager.getTextMutedColor());
        }

        g2.dispose();
    }
}
