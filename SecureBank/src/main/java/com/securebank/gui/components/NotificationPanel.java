package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * NotificationPanel — a production-grade toast notification.
 *
 * Features:
 * - Glass-effect background
 * - Smooth slide-in animation
 * - Color-coded icons
 * - Auto-dismiss with fade-out
 * - Stacking support
 */
public class NotificationPanel extends JPanel {

    /** How long the notification stays visible (milliseconds) */
    private static final int DISPLAY_DURATION = 3500;

    /** Slide-in animation duration */
    private static final int SLIDE_DURATION = 300;

    /** Notification types with their corresponding colors */
    public enum NotificationType {
        SUCCESS(new Color(0x2E, 0xCC, 0x71), "✓"),
        ERROR(new Color(0xE7, 0x4C, 0x3C), "✕"),
        WARNING(new Color(0xF3, 0x9C, 0x12), "⚠"),
        INFO(new Color(0x34, 0x98, 0xDB), "ℹ");

        final Color color;
        final String icon;

        NotificationType(Color color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    private final Timer dismissTimer;
    private float slideProgress = 0f;
    private Timer slideTimer;
    private final NotificationType type;

    /**
     * Creates a notification panel.
     */
    public NotificationPanel(String message, NotificationType type) {
        this.type = type;
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        setOpaque(false);
        setPreferredSize(new Dimension(350, 50));
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        // Icon
        JLabel iconLabel = new JLabel(type.icon);
        iconLabel.setFont(ThemeManager.getBoldFont(18));
        iconLabel.setForeground(type.color);

        // Message
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(ThemeManager.getFont(13));
        msgLabel.setForeground(ThemeManager.getTextLightColor());

        add(iconLabel);
        add(msgLabel);

        // Calculate width
        int prefWidth = Math.max(350, msgLabel.getPreferredSize().width +
                iconLabel.getPreferredSize().width + 60);
        setPreferredSize(new Dimension(prefWidth, 50));
        setSize(prefWidth, 50);

        // Auto-dismiss timer
        dismissTimer = new Timer(DISPLAY_DURATION, e -> {
            // Fade out
            Timer fadeOut = new Timer(16, ev -> {
                float alpha = getAlpha() - 0.05f;
                if (alpha <= 0) {
                    ((Timer) ev.getSource()).stop();
                    Container parent = getParent();
                    if (parent != null) {
                        parent.remove(NotificationPanel.this);
                        parent.revalidate();
                        parent.repaint();
                    }
                } else {
                    setAlpha(alpha);
                }
            });
            fadeOut.setRepeats(true);
            fadeOut.start();
        });
        dismissTimer.setRepeats(false);
    }

    private float alpha = 1.0f;

    private void setAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
        repaint();
    }

    private float getAlpha() {
        return alpha;
    }

    /**
     * Custom paint — draws glass-effect rounded rectangle.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 2;
        int h = getHeight() - 2;

        // Apply alpha
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new RoundRectangle2D.Float(3, 3, w, h, 12, 12));

        // Glass background
        g2.setColor(new Color(type.color.getRed(), type.color.getGreen(),
                type.color.getBlue(), 200));
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));

        // Top highlight
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h / 2, 12, 12));

        // Border
        g2.setColor(new Color(255, 255, 255, 20));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12));

        g2.dispose();
        super.paintComponent(g);
    }

    // ==================== STATIC CONVENIENCE METHODS ====================

    public static void showSuccess(JFrame frame, String message) {
        show(frame, message, NotificationType.SUCCESS);
    }

    public static void showError(JFrame frame, String message) {
        show(frame, message, NotificationType.ERROR);
    }

    public static void showWarning(JFrame frame, String message) {
        show(frame, message, NotificationType.WARNING);
    }

    public static void showInfo(JFrame frame, String message) {
        show(frame, message, NotificationType.INFO);
    }

    /**
     * Displays a notification at the top-right of the given frame.
     */
    private static void show(JFrame frame, String message, NotificationType type) {
        if (frame == null) return;

        NotificationPanel notification = new NotificationPanel(message, type);

        // Get the glass pane (overlay layer) of the frame
        JPanel glassPane = (JPanel) frame.getGlassPane();
        glassPane.setVisible(true);
        glassPane.setLayout(null);
        glassPane.setOpaque(false);

        // Calculate dynamic width based on message length
        int width = Math.max(350, notification.getPreferredSize().width);
        int x = frame.getWidth() - width - 20;
        int baseY = 15;

        // Stack multiple notifications
        Component[] existing = glassPane.getComponents();
        final int y = baseY + existing.length * 60;

        notification.setBounds(x, y, width, 50);
        glassPane.add(notification);
        glassPane.revalidate();
        glassPane.repaint();

        // Start slide-in animation
        notification.slideProgress = 0f;
        notification.slideTimer = new Timer(16, e -> {
            notification.slideProgress = Math.min(1.0f, notification.slideProgress + 0.06f);
            int currentX = (int) (frame.getWidth() + width -
                    (frame.getWidth() - x) * notification.slideProgress);
            notification.setLocation(currentX, y);
            notification.repaint();
            if (notification.slideProgress >= 1.0f) {
                notification.slideTimer.stop();
                // Start auto-dismiss
                notification.dismissTimer.start();
            }
        });
        notification.slideTimer.setRepeats(true);
        notification.slideTimer.start();
    }
}
