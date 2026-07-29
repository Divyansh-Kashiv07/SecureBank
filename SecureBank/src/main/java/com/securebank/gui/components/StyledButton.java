package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * StyledButton — a custom JButton with modern styling, hover effects, and loading state.
 *
 * Features:
 * - Custom background color with hover darkening
 * - Rounded corners
 * - "Processing..." loading state
 * - No border/focus painting from default L&F
 */
public class StyledButton extends JButton {

    /** The normal background color */
    private Color normalColor;

    /** The hover background color (auto-calculated darker shade) */
    private Color hoverColor;

    /** The pressed background color */
    private Color pressedColor;

    /** Whether the button is in a "loading" state */
    private boolean loading;

    /** The original text (saved when showing "Processing...") */
    private String originalText;

    /** Predefined color constants */
    public static final Color PRIMARY = new Color(0x1A, 0x2B, 0x4C);      // Deep navy
    public static final Color ACCENT_TEAL = new Color(0x0D, 0x73, 0x77);  // Teal
    public static final Color ACCENT_AMBER = new Color(0xF4, 0xA2, 0x61); // Amber
    public static final Color SUCCESS = new Color(0x2E, 0xCC, 0x71);      // Green
    public static final Color DANGER = new Color(0xE7, 0x4C, 0x3C);       // Red
    public static final Color WARNING = new Color(0xF3, 0x9C, 0x12);      // Orange

    /**
     * Creates a styled button with text and a background color.
     *
     * @param text  the button label
     * @param color the background color
     */
    public StyledButton(String text, Color color) {
        super(text);
        this.normalColor = color;
        this.hoverColor = darken(color, 0.15f);
        this.pressedColor = darken(color, 0.25f);
        this.loading = false;
        this.originalText = text;

        // Style setup
        setFont(ThemeManager.getBoldFont(13));
        setForeground(ThemeManager.getTextLightColor());
        setBackground(normalColor);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(160, 40));

        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!loading) setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!loading) setBackground(normalColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!loading) setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!loading) setBackground(isHovered() ? hoverColor : normalColor);
            }
        });
    }

    /**
     * Creates a styled button with the teal accent color (default CTA).
     */
    public StyledButton(String text) {
        this(text, ThemeManager.getPrimaryAccentColor());
    }

    /**
     * Sets the button to a loading state (disabled + "Processing..." text).
     *
     * @param loading true to show loading state
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) {
            originalText = getText();
            setText("Processing...");
            setEnabled(false);
            setBackground(darken(normalColor, 0.3f));
        } else {
            setText(originalText);
            setEnabled(true);
            setBackground(normalColor);
        }
    }

    /**
     * Returns whether the button is in loading state.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Changes the button's color scheme.
     */
    public void setButtonColor(Color color) {
        this.normalColor = color;
        this.hoverColor = darken(color, 0.15f);
        this.pressedColor = darken(color, 0.25f);
        setBackground(normalColor);
    }

    /**
     * Custom paint — draws rounded rectangle background.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw fully rounded pill-shaped background
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

        g2d.dispose();
        super.paintComponent(g);
    }

    /**
     * Checks if the mouse is currently hovering over the button.
     */
    private boolean isHovered() {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(mousePos, this);
        return contains(mousePos);
    }

    /**
     * Darkens a color by a factor (0.0 = no change, 1.0 = black).
     */
    private static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }
}
