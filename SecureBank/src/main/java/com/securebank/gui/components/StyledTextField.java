package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * StyledTextField — a production-grade text field with smooth animations.
 *
 * Features:
 * - Animated focus glow effect
 * - Smooth border color transitions
 * - Placeholder text
 * - Rounded corners
 * - Anti-aliased rendering
 */
public class StyledTextField extends JTextField {

    private String placeholder;
    private boolean showingPlaceholder;
    private float focusProgress = 0f; // 0..1
    private Timer focusTimer;

    /** Current state colors */
    private Color currentBorderColor;

    public StyledTextField(String placeholder) {
        this.placeholder = placeholder;
        this.showingPlaceholder = true;
        this.currentBorderColor = ThemeManager.getBorderColor();

        setFont(ThemeManager.getFont(14));
        setBackground(ThemeManager.getCardColor());
        setCaretColor(ThemeManager.getTextLightColor());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        setPreferredSize(new Dimension(300, 44));
        setOpaque(false);

        // Show placeholder initially
        setText(placeholder);
        setForeground(ThemeManager.getTextMutedColor());

        // Focus listeners for placeholder behavior and animation
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingPlaceholder) {
                    setText("");
                    setForeground(ThemeManager.getTextLightColor());
                    showingPlaceholder = false;
                }
                startFocusAnimation(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setText(StyledTextField.this.placeholder);
                    setForeground(ThemeManager.getTextMutedColor());
                    showingPlaceholder = true;
                }
                startFocusAnimation(false);
            }
        });
    }

    /**
     * Creates a styled text field without a placeholder.
     */
    public StyledTextField() {
        this("");
        showingPlaceholder = false;
        setForeground(ThemeManager.getTextLightColor());
    }

    /**
     * Starts the focus glow animation.
     */
    private void startFocusAnimation(boolean forward) {
        if (focusTimer != null && focusTimer.isRunning()) {
            focusTimer.stop();
        }

        focusTimer = new Timer(16, e -> {
            boolean needsUpdate = false;
            if (forward && focusProgress < 1.0f) {
                focusProgress = Math.min(1.0f, focusProgress + 0.1f);
                needsUpdate = true;
            } else if (!forward && focusProgress > 0.0f) {
                focusProgress = Math.max(0.0f, focusProgress - 0.1f);
                needsUpdate = true;
            }

            // Interpolate border color
            currentBorderColor = ThemeManager.lerp(
                    ThemeManager.getBorderColor(),
                    ThemeManager.getPrimaryAccentColor(),
                    focusProgress);

            if (needsUpdate) {
                repaint();
            } else {
                focusTimer.stop();
            }
        });
        focusTimer.setRepeats(true);
        focusTimer.start();
    }

    /**
     * Custom paint for rounded corners and glow effect.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int radius = ThemeManager.RADIUS_SM;

        // Focus glow
        if (focusProgress > 0) {
            for (int i = 3; i >= 0; i--) {
                float alpha = focusProgress * 0.1f * (4 - i);
                g2.setColor(new Color(
                        ThemeManager.getPrimaryAccentColor().getRed(),
                        ThemeManager.getPrimaryAccentColor().getGreen(),
                        ThemeManager.getPrimaryAccentColor().getBlue(),
                        Math.max(0, (int)(alpha * 255))));
                g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2 * i, h + 2 * i,
                        radius + i, radius + i));
            }
        }

        // Background
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

        // Border
        g2.setColor(currentBorderColor);
        g2.setStroke(new BasicStroke(focusProgress > 0 ? 1.5f : 1.0f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));

        g2.dispose();

        // Draw text
        super.paintComponent(g);
    }

    /**
     * Returns the actual text (empty string if placeholder is showing).
     */
    public String getActualText() {
        return showingPlaceholder ? "" : getText();
    }

    /**
     * Returns whether the field is currently showing the placeholder.
     */
    public boolean isShowingPlaceholder() {
        return showingPlaceholder;
    }

    /**
     * Sets new text, clearing the placeholder state.
     */
    public void setActualText(String text) {
        showingPlaceholder = false;
        setForeground(ThemeManager.getTextLightColor());
        setText(text);
    }

    /**
     * Clears the field and restores the placeholder.
     */
    public void clearField() {
        setText(placeholder);
        setForeground(ThemeManager.getTextMutedColor());
        showingPlaceholder = true;
    }
}
