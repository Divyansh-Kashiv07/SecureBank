package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * BankIcon — professional vector-drawn icons for the sidebar.
 *
 * Replaces emoji-based icons (Segoe UI Emoji) with clean Java2D vector paths
 * that look professional at any size and scale without relying on platform fonts.
 *
 * Each icon is drawn procedurally using Graphics2D shapes — no external files needed.
 */
public class BankIcon extends JPanel {

    public enum IconType {
        DASHBOARD,      // Grid / grid squares
        ACCOUNTS,       // Credit card
        DEPOSIT_WITHDRAW, // Arrow down-up
        TRANSFER,       // Two arrows
        LOANS,          // Document with dollar
        TRANSACTIONS,   // List / scroll
        REPORTS,        // Bar chart
        SETTINGS,       // Gear
        LOGOUT,         // Door with arrow
        SHIELD,         // Security shield
        CHART_UP        // Trending up
    }

    private IconType iconType;
    private Color iconColor;
    private int iconSize;
    private float animationProgress = 1.0f; // 0..1 for hover animation

    public BankIcon(IconType type, Color color, int size) {
        this.iconType = type;
        this.iconColor = color;
        this.iconSize = size;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    public void setIconColor(Color color) {
        this.iconColor = color;
        repaint();
    }

    public void setAnimationProgress(float progress) {
        this.animationProgress = progress;
        repaint();
    }

    public IconType getIconType() {
        return iconType;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int s = iconSize;
        int pad = s / 6;
        Color color = ThemeManager.lerp(Color.GRAY, iconColor, animationProgress);

        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(1.5f, s / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (iconType) {
            case DASHBOARD: drawDashboard(g2, pad, s); break;
            case ACCOUNTS: drawAccounts(g2, pad, s); break;
            case DEPOSIT_WITHDRAW: drawDepositWithdraw(g2, pad, s); break;
            case TRANSFER: drawTransfer(g2, pad, s); break;
            case LOANS: drawLoans(g2, pad, s); break;
            case TRANSACTIONS: drawTransactions(g2, pad, s); break;
            case REPORTS: drawReports(g2, pad, s); break;
            case SETTINGS: drawSettings(g2, pad, s); break;
            case LOGOUT: drawLogout(g2, pad, s); break;
            case SHIELD: drawShield(g2, pad, s); break;
            case CHART_UP: drawChartUp(g2, pad, s); break;
        }

        g2.dispose();
    }

    // ---- Icon drawing methods ----

    private void drawDashboard(Graphics2D g2, int p, int s) {
        int gap = s / 12;
        int halfW = (s - 2 * p - gap) / 2;
        int halfH = (s - 2 * p - gap) / 2;
        // Top-left
        g2.fill(new RoundRectangle2D.Float(p, p, halfW, halfH, 3, 3));
        // Top-right
        g2.fill(new RoundRectangle2D.Float(p + halfW + gap, p, halfW, halfH, 3, 3));
        // Bottom-left (smaller)
        g2.fill(new RoundRectangle2D.Float(p, p + halfH + gap, halfW, halfH, 3, 3));
        // Bottom-right (tall)
        g2.fill(new RoundRectangle2D.Float(p + halfW + gap, p, halfW, s - 2 * p, 3, 3));
    }

    private void drawAccounts(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        // Card body
        g2.draw(new RoundRectangle2D.Float(p, p + h / 5, w, h * 4 / 5, 4, 4));
        // Stripe
        g2.fillRect(p, p + h * 2 / 5, w, h / 8);
    }

    private void drawDepositWithdraw(Graphics2D g2, int p, int s) {
        int mid = s / 2;
        int arrowH = (s - 2 * p) / 3;
        // Down arrow
        drawArrowDown(g2, mid - p / 2, p + arrowH / 2, arrowH);
        // Up arrow
        drawArrowUp(g2, mid + p / 2, s - p - arrowH / 2, arrowH);
    }

    private void drawTransfer(Graphics2D g2, int p, int s) {
        int mid = s / 2;
        // Right arrow (top)
        drawArrowRight(g2, p, p + (s - 2 * p) / 3, s - 2 * p);
        // Left arrow (bottom)
        drawArrowLeft(g2, p, s - p - (s - 2 * p) / 3, s - 2 * p);
    }

    private void drawLoans(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        // Document
        g2.draw(new RoundRectangle2D.Float(p, p, w * 3 / 4, h, 3, 3));
        // Dollar sign area
        g2.fill(new RoundRectangle2D.Float(p + w / 3, p + h / 3, w / 3, h / 3, 2, 2));
        // Corner fold
        int[] xPoints = {p + w * 3 / 4 - 6, p + w * 3 / 4, p + w * 3 / 4};
        int[] yPoints = {p, p, p + 6};
        g2.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawTransactions(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int lineH = (s - 2 * p) / 4;
        int gap = lineH / 3;
        // Lines with dots
        for (int i = 0; i < 4; i++) {
            int y = p + i * (lineH + gap);
            // Dot
            g2.fillOval(p, y + lineH / 4, lineH / 2, lineH / 2);
            // Line
            g2.fillRect(p + lineH, y + lineH / 3, w - lineH - p, lineH / 3);
        }
    }

    private void drawReports(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        int barW = w / 5;
        int gap = barW / 2;
        // Bars
        int[] heights = {h * 2 / 5, h * 3 / 5, h * 4 / 5, h};
        for (int i = 0; i < 4; i++) {
            int bh = heights[i];
            int bx = p + i * (barW + gap);
            int by = s - p - bh;
            g2.fill(new RoundRectangle2D.Float(bx, by, barW, bh, 2, 2));
        }
        // Base line
        g2.drawLine(p, s - p, s - p, s - p);
    }

    private void drawSettings(Graphics2D g2, int p, int s) {
        int cx = s / 2, cy = s / 2;
        int outerR = (s - 2 * p) / 2;
        int innerR = outerR * 5 / 8;
        int teeth = 8;
        // Gear teeth
        for (int i = 0; i < teeth; i++) {
            double angle = Math.toRadians(i * 360.0 / teeth);
            int tx = (int) (cx + Math.cos(angle) * outerR * 0.85);
            int ty = (int) (cy + Math.sin(angle) * outerR * 0.85);
            g2.fillOval(tx - 3, ty - 3, 6, 6);
        }
        // Outer circle
        g2.drawOval(cx - outerR * 2 / 3, cy - outerR * 2 / 3, outerR * 4 / 3, outerR * 4 / 3);
        // Inner circle (hole)
        g2.setColor(ThemeManager.getBackgroundColor());
        g2.fillOval(cx - innerR / 2, cy - innerR / 2, innerR, innerR);
        g2.setColor(ThemeManager.getTextMutedColor());
    }

    private void drawLogout(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        // Door frame
        g2.draw(new RoundRectangle2D.Float(p + w / 4, p, w * 3 / 4, h, 3, 3));
        // Arrow pointing left
        int arrowY = s / 2;
        int arrowLen = w / 2;
        g2.drawLine(p, arrowY, p + arrowLen, arrowY);
        g2.drawLine(p + arrowLen / 3, arrowY - arrowLen / 4, p, arrowY);
        g2.drawLine(p + arrowLen / 3, arrowY + arrowLen / 4, p, arrowY);
    }

    private void drawShield(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        int[] xPoints = {p, p + w / 2, p + w, p + w, p + w / 2, p};
        int[] yPoints = {p, p, p, p + h * 2 / 3, s - p, p + h * 2 / 3};
        g2.fillPolygon(xPoints, yPoints, 6);
    }

    private void drawChartUp(Graphics2D g2, int p, int s) {
        int w = s - 2 * p;
        int h = s - 2 * p;
        // Line chart going up
        int[] xP = {p, p + w / 3, p + w * 2 / 3, p + w};
        int[] yP = {p + h, p + h * 2 / 3, p + h / 4, p};
        g2.drawPolyline(xP, yP, 4);
        // Arrow tip
        g2.drawLine(p + w, p, p + w - 5, p + 5);
        g2.drawLine(p + w, p, p + w - 5, p - 2);
    }

    // ---- Arrow helpers ----

    private void drawArrowDown(Graphics2D g2, int cx, int cy, int size) {
        int half = size / 2;
        g2.drawLine(cx, cy - half, cx, cy + half);
        g2.drawLine(cx - half / 2, cy + half / 2, cx, cy + half);
        g2.drawLine(cx + half / 2, cy + half / 2, cx, cy + half);
    }

    private void drawArrowUp(Graphics2D g2, int cx, int cy, int size) {
        int half = size / 2;
        g2.drawLine(cx, cy + half, cx, cy - half);
        g2.drawLine(cx - half / 2, cy - half / 2, cx, cy - half);
        g2.drawLine(cx + half / 2, cy - half / 2, cx, cy - half);
    }

    private void drawArrowRight(Graphics2D g2, int x, int cy, int length) {
        g2.drawLine(x, cy, x + length, cy);
        g2.drawLine(x + length - 5, cy - 4, x + length, cy);
        g2.drawLine(x + length - 5, cy + 4, x + length, cy);
    }

    private void drawArrowLeft(Graphics2D g2, int x, int cy, int length) {
        g2.drawLine(x + length, cy, x, cy);
        g2.drawLine(x + 5, cy - 4, x, cy);
        g2.drawLine(x + 5, cy + 4, x, cy);
    }

    // ---- Static factory for convenience ----

    public static BankIcon create(IconType type, int size) {
        return new BankIcon(type, ThemeManager.getTextMutedColor(), size);
    }

    public static BankIcon create(IconType type, Color color, int size) {
        return new BankIcon(type, color, size);
    }
}
