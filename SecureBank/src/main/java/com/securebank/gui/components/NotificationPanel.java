package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * NotificationPanel — a toast-style notification that slides in and auto-dismisses.
 *
 * Replaces ugly JOptionPane popups with modern, non-blocking toast notifications.
 * Appears at the top-right of the parent frame and fades out after a delay.
 *
 * Usage:
 *   NotificationPanel.showSuccess(parentFrame, "Deposit successful!");
 *   NotificationPanel.showError(parentFrame, "Insufficient balance");
 *   NotificationPanel.showInfo(parentFrame, "Welcome back, Divyansh!");
 */
public class NotificationPanel extends JPanel {

    /** How long the notification stays visible (milliseconds) */
    private static final int DISPLAY_DURATION = 3500;

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

    /**
     * Creates a notification panel.
     *
     * @param message the notification message
     * @param type    the notification type (SUCCESS, ERROR, WARNING, INFO)
     */
    public NotificationPanel(String message, NotificationType type) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        setBackground(type.color);
        setPreferredSize(new Dimension(350, 50));
        setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        // Icon
        JLabel iconLabel = new JLabel(type.icon);
        iconLabel.setFont(ThemeManager.getBoldFont(18));
        iconLabel.setForeground(Color.WHITE);

        // Message
        JLabel msgLabel = new JLabel(message);
        msgLabel.setFont(ThemeManager.getFont(13));
        msgLabel.setForeground(Color.WHITE);

        add(iconLabel);
        add(msgLabel);

        // Let it compute its preferred width, but min 350, height 50
        int prefWidth = Math.max(350, msgLabel.getPreferredSize().width + iconLabel.getPreferredSize().width + 60);
        setPreferredSize(new Dimension(prefWidth, 50));
        setSize(prefWidth, 50);

        // Auto-dismiss timer
        dismissTimer = new Timer(DISPLAY_DURATION, e -> {
            Container parent = getParent();
            if (parent != null) {
                parent.remove(NotificationPanel.this);
                parent.revalidate();
                parent.repaint();
            }
        });
        dismissTimer.setRepeats(false);
    }

    /**
     * Custom paint — draws rounded rectangle with slight shadow.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 10, 10);

        // Background
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 10, 10);

        g2d.dispose();
        super.paintComponent(g);
    }

    // ==================== STATIC CONVENIENCE METHODS ====================

    /**
     * Shows a success notification.
     */
    public static void showSuccess(JFrame frame, String message) {
        show(frame, message, NotificationType.SUCCESS);
    }

    /**
     * Shows an error notification.
     */
    public static void showError(JFrame frame, String message) {
        show(frame, message, NotificationType.ERROR);
    }

    /**
     * Shows a warning notification.
     */
    public static void showWarning(JFrame frame, String message) {
        show(frame, message, NotificationType.WARNING);
    }

    /**
     * Shows an info notification.
     */
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
        glassPane.setLayout(null); // Absolute positioning
        glassPane.setOpaque(false);

        // Calculate dynamic width based on message length
        int width = Math.max(350, notification.getPreferredSize().width);
        int x = frame.getWidth() - width - 20;
        int y = 15;

        // Stack multiple notifications (offset by 60px each)
        Component[] existing = glassPane.getComponents();
        y += existing.length * 60;

        notification.setBounds(x, y, width, 50);
        glassPane.add(notification);
        glassPane.revalidate();
        glassPane.repaint();

        // Start the auto-dismiss timer
        notification.dismissTimer.start();
    }
}
