package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * CardPanel — a production-grade rounded-corner card component.
 *
 * Features:
 * - Glass-effect background
 * - Smooth drop shadow
 * - Accent gradient bar at top
 * - Anti-aliased rendering
 * - Configurable glass intensity
 */
public class CardPanel extends JPanel {

    /** Corner radius */
    private static final int CORNER_RADIUS = 14;

    /** Card background color */
    private Color cardBackground = ThemeManager.getCardColor();

    /** Card border color */
    private Color borderColor = ThemeManager.getBorderColor();

    /** Optional accent bar color at the top of the card */
    private Color accentColor = null;

    /** Whether to use glass effect */
    private boolean useGlassEffect = true;

    /** Shadow intensity (0..1) */
    private float shadowIntensity = 0.5f;

    /**
     * Creates a card panel with no title.
     */
    public CardPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 22, 18, 22));
    }

    /**
     * Creates a card panel with a title.
     */
    public CardPanel(String title) {
        this();

        if (title != null && !title.isEmpty()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(ThemeManager.getBoldFont(15));
            titleLabel.setForeground(ThemeManager.getTextLightColor());
            titleLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
            add(titleLabel, BorderLayout.NORTH);
        }
    }

    /**
     * Creates a card panel with a title and an accent color bar.
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
     * Enables or disables the glass effect.
     */
    public void setUseGlassEffect(boolean use) {
        this.useGlassEffect = use;
        repaint();
    }

    /**
     * Custom paint — draws glass card with shadow and accent bar.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 4;
        int h = getHeight() - 4;

        // Draw shadow
        ThemeManager.drawShadow(g2, 4, 4, w, h, CORNER_RADIUS);

        // Draw main card
        if (useGlassEffect) {
            // Glass background
            g2.setColor(ThemeManager.getGlassColor());
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS));

            // Top highlight
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h / 3, CORNER_RADIUS, CORNER_RADIUS));
        } else {
            g2.setColor(cardBackground);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS));
        }

        // Draw border
        g2.setColor(ThemeManager.getGlassBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS));

        // Draw accent bar at top if color is set
        if (accentColor != null) {
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS));
            GradientPaint gradient = new GradientPaint(
                    0, 0, ThemeManager.lighten(accentColor, 0.1f),
                    w, 0, accentColor);
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, w, 4, CORNER_RADIUS, CORNER_RADIUS);
            g2.fillRect(0, 2, w, 2);
            g2.setClip(null);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
