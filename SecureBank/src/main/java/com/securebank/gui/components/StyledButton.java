package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * StyledButton — a production-grade custom JButton with smooth animations.
 *
 * Features:
 * - Smooth hover/press transitions via Swing Timer
 * - Gradient backgrounds
 * - Loading state with animated dots
 * - Focus glow effect
 * - Rounded corners with anti-aliasing
 */
public class StyledButton extends JButton {

    /** The normal background color */
    private Color normalColor;

    /** The hover background color (auto-calculated lighter shade) */
    private Color hoverColor;

    /** The pressed background color */
    private Color pressedColor;

    /** Whether the button is in a "loading" state */
    private boolean loading;

    /** The original text (saved when showing "Processing...") */
    private String originalText;

    /** Animation state */
    private float hoverProgress = 0f; // 0..1
    private float pressProgress = 0f; // 0..1
    private float focusProgress = 0f; // 0..1
    private boolean hasFocus = false;
    private Timer animTimer;

    /** Predefined color constants */
    public static final Color PRIMARY = new Color(0x1A, 0x2B, 0x4C);
    public static final Color ACCENT_TEAL = new Color(0x0D, 0x73, 0x77);
    public static final Color ACCENT_AMBER = new Color(0xF4, 0xA2, 0x61);
    public static final Color SUCCESS = new Color(0x2E, 0xCC, 0x71);
    public static final Color DANGER = new Color(0xE7, 0x4C, 0x3C);
    public static final Color WARNING = new Color(0xF3, 0x9C, 0x12);

    /** Loading animation dot index */
    private int loadingDotIndex = 0;
    private Timer loadingTimer;

    /**
     * Creates a styled button with text and a background color.
     */
    public StyledButton(String text, Color color) {
        super(text);
        this.normalColor = color;
        this.hoverColor = ThemeManager.lighten(color, 0.15f);
        this.pressedColor = ThemeManager.darken(color, 0.15f);
        this.loading = false;
        this.originalText = text;

        setFont(ThemeManager.getBoldFont(13));
        setForeground(ThemeManager.getTextLightColor());
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
                if (!loading) startHoverAnimation(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!loading) startHoverAnimation(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!loading) {
                    pressProgress = 1.0f;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!loading) {
                    pressProgress = 0f;
                    repaint();
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                hasFocus = true;
                startFocusAnimation(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                hasFocus = false;
                startFocusAnimation(false);
            }
        });
    }

    /**
     * Creates a styled button with the default accent color.
     */
    public StyledButton(String text) {
        this(text, ThemeManager.getPrimaryAccentColor());
    }

    /**
     * Starts the hover transition animation.
     */
    private void startHoverAnimation(boolean forward) {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        animTimer = new Timer(16, e -> {
            boolean needsUpdate = false;
            if (forward && hoverProgress < 1.0f) {
                hoverProgress = Math.min(1.0f, hoverProgress + 0.12f);
                needsUpdate = true;
            } else if (!forward && hoverProgress > 0.0f) {
                hoverProgress = Math.max(0.0f, hoverProgress - 0.12f);
                needsUpdate = true;
            }

            if (needsUpdate) {
                repaint();
            } else {
                animTimer.stop();
            }
        });
        animTimer.setRepeats(true);
        animTimer.start();
    }

    /**
     * Starts the focus glow animation.
     */
    private void startFocusAnimation(boolean forward) {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        animTimer = new Timer(16, e -> {
            boolean needsUpdate = false;
            if (forward && focusProgress < 1.0f) {
                focusProgress = Math.min(1.0f, focusProgress + 0.1f);
                needsUpdate = true;
            } else if (!forward && focusProgress > 0.0f) {
                focusProgress = Math.max(0.0f, focusProgress - 0.1f);
                needsUpdate = true;
            }

            if (needsUpdate) {
                repaint();
            } else {
                animTimer.stop();
            }
        });
        animTimer.setRepeats(true);
        animTimer.start();
    }

    /**
     * Sets the button to a loading state.
     */
    public void setLoading(boolean loading) {
        this.loading = loading;
        if (loading) {
            setText("● ● ●");
            setEnabled(false);

            // Animated dots
            loadingTimer = new Timer(400, e -> {
                loadingDotIndex = (loadingDotIndex + 1) % 3;
                StringBuilder dots = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    dots.append(i == loadingDotIndex ? "●" : "○");
                    dots.append(" ");
                }
                setText(dots.toString().trim());
            });
            loadingTimer.start();
        } else {
            if (loadingTimer != null) loadingTimer.stop();
            setText(originalText);
            setEnabled(true);
        }
        repaint();
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
        this.hoverColor = ThemeManager.lighten(color, 0.15f);
        this.pressedColor = ThemeManager.darken(color, 0.15f);
        repaint();
    }

    /**
     * Custom paint — draws rounded rectangle with smooth animations.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int radius = h; // pill shape

        // Interpolate color based on hover/press state
        Color bgColor = normalColor;
        if (pressProgress > 0) {
            bgColor = ThemeManager.lerp(hoverColor, pressedColor, pressProgress);
        } else if (hoverProgress > 0) {
            bgColor = ThemeManager.lerp(normalColor, hoverColor, hoverProgress);
        }

        // Focus glow
        if (focusProgress > 0) {
            for (int i = 3; i >= 0; i--) {
                float alpha = focusProgress * 0.08f * (4 - i);
                g2.setColor(new Color(
                        normalColor.getRed(), normalColor.getGreen(), normalColor.getBlue(),
                        Math.max(0, (int)(alpha * 255))));
                g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2 * i, h + 2 * i, radius + i, radius + i));
            }
        }

        // Button background with gradient
        GradientPaint gradient = new GradientPaint(
                0, 0, bgColor,
                0, h, ThemeManager.darken(bgColor, 0.1f));
        g2.setPaint(gradient);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

        // Top highlight
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h / 2, radius, radius));

        // Border
        g2.setColor(new Color(255, 255, 255, 15));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));

        g2.dispose();

        // Draw text on top
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
}
