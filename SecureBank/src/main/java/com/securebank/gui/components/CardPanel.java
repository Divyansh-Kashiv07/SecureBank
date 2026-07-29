package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * CardPanel — a rounded-corner card component for the dashboard and other panels.
 *
 * This is a reusable JPanel that draws itself with:
 * - Rounded corners (12px radius)
 * - Dark background with subtle border glow
 * - Optional title header
 * - Generous padding
 *
 * Used throughout the app for grouping related content into visual "cards"
 * (similar to Material Design cards).
 */
public class CardPanel extends JPanel {

    /** Corner radius for the rounded rectangle */
    private static final int CORNER_RADIUS = 12;

    /** Card background color */
    private Color cardBackground = ThemeManager.getCardColor();

    /** Card border color */
    private Color borderColor = ThemeManager.getBorderColor();

    /** Optional accent bar color at the top of the card */
    private Color accentColor = null;

    /**
     * Creates a card panel with no title.
     */
    public CardPanel() {
        setOpaque(false); // We'll paint our own background with rounded corners
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 20, 16, 20)); // Generous internal padding
    }

    /**
     * Creates a card panel with a title.
     *
     * @param title the title to display at the top of the card
     */
    public CardPanel(String title) {
        this();

        if (title != null && !title.isEmpty()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(ThemeManager.getBoldFont(15));
            titleLabel.setForeground(ThemeManager.getTextLightColor()); // Light text for dark theme
            titleLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
            add(titleLabel, BorderLayout.NORTH);
        }
    }

    /**
     * Creates a card panel with a title and an accent color bar.
     *
     * @param title       the title
     * @param accentColor the color of the accent bar at the top
     */
    public CardPanel(String title, Color accentColor) {
        this(title);
        this.accentColor = accentColor;
    }

    /**
     * Sets the card's background color.
     */
    public void setCardBackground(Color color) {
        this.cardBackground = color;
        repaint();
    }

    /**
     * Sets the accent color bar at the top.
     */
    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    /**
     * Custom paint — draws rounded rectangle background with dark border and optional accent bar.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable anti-aliasing for smooth rounded corners
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw subtle glow/shadow (dark mode compatible)
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillRoundRect(3, 3, getWidth() - 4, getHeight() - 4, CORNER_RADIUS, CORNER_RADIUS);

        // Draw main card background
        g2d.setColor(cardBackground);
        g2d.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, CORNER_RADIUS, CORNER_RADIUS);

        // Draw dark border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, CORNER_RADIUS, CORNER_RADIUS);

        // Draw accent bar at the top if color is set
        if (accentColor != null) {
            g2d.setColor(accentColor);
            g2d.fillRoundRect(0, 0, getWidth() - 4, 4, CORNER_RADIUS, CORNER_RADIUS);
            // Fill the bottom part of the accent to make it flat
            g2d.fillRect(0, 2, getWidth() - 4, 2);
        }

        g2d.dispose();
        super.paintComponent(g);
    }
}
