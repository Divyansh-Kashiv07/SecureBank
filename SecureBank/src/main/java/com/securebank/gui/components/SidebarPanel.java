package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;
import com.securebank.gui.AppLanguage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SidebarPanel — a modern, production-grade sidebar with smooth animations.
 *
 * Features:
 * - Vector-drawn icons (BankIcon) instead of emoji
 * - Smooth hover animations via Swing Timer
 * - Gradient selection indicator
 * - Glass-effect card for active item
 * - Professional typography hierarchy
 */
public class SidebarPanel extends JPanel {

    /** Width of the sidebar in pixels */
    private static final int SIDEBAR_WIDTH = 240;

    /** Nav item height */
    private static final int NAV_ITEM_HEIGHT = 44;

    /** Animation step count */
    private static final int ANIM_STEPS = 8;

    /** List of navigation items */
    private List<NavItem> navItems;

    /** The currently selected nav item index */
    private int selectedIndex;

    /** Callback interface for when a nav item is clicked */
    public interface NavigationListener {
        void onNavigate(String screenName);
    }

    private NavigationListener navigationListener;

    /**
     * Creates the sidebar with predefined navigation items.
     */
    public SidebarPanel() {
        this.navItems = new ArrayList<>();
        this.selectedIndex = 0;

        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMaximumSize(new Dimension(SIDEBAR_WIDTH, Integer.MAX_VALUE));
        setBackground(ThemeManager.getSidebarBackground());
        setLayout(new BorderLayout());

        buildSidebar();
    }

    /**
     * Rebuilds the entire sidebar with fresh language strings.
     */
    public void rebuild() {
        int previousIndex = selectedIndex;
        removeAll();
        navItems = new ArrayList<>();
        buildSidebar();
        selectItem(previousIndex);
        revalidate();
        repaint();
    }

    /**
     * Internal method that constructs the sidebar content.
     */
    private void buildSidebar() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ThemeManager.getSidebarBackground());
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Logo / App name at the top
        contentPanel.add(createLogoPanel());
        contentPanel.add(Box.createVerticalStrut(24));

        // Navigation items with icons
        addNavItem(contentPanel, BankIcon.IconType.DASHBOARD, AppLanguage.get("sidebar.dashboard"), "Dashboard");
        addNavItem(contentPanel, BankIcon.IconType.ACCOUNTS, AppLanguage.get("sidebar.accounts"), "Accounts");
        addNavItem(contentPanel, BankIcon.IconType.PAYEES, AppLanguage.get("sidebar.payees"), "Beneficiaries");
        addNavItem(contentPanel, BankIcon.IconType.DEPOSIT_WITHDRAW, AppLanguage.get("sidebar.deposit.withdraw"), "DepositWithdraw");
        addNavItem(contentPanel, BankIcon.IconType.TRANSFER, AppLanguage.get("sidebar.transfer"), "Transfer");
        addNavItem(contentPanel, BankIcon.IconType.LOANS, AppLanguage.get("sidebar.loans"), "Loans");
        addNavItem(contentPanel, BankIcon.IconType.TRANSACTIONS, AppLanguage.get("sidebar.transactions"), "History");
        addNavItem(contentPanel, BankIcon.IconType.REPORTS, AppLanguage.get("sidebar.reports"), "Reports");
        addNavItem(contentPanel, BankIcon.IconType.SETTINGS, AppLanguage.get("sidebar.settings"), "Settings");

        // Push remaining space to bottom
        contentPanel.add(Box.createVerticalGlue());

        // Logout at the bottom
        addNavItem(contentPanel, BankIcon.IconType.LOGOUT, AppLanguage.get("sidebar.logout"), "Logout");

        add(contentPanel, BorderLayout.CENTER);

        // Select the first item (Dashboard) by default
        if (!navItems.isEmpty()) {
            selectItem(0);
        }
    }

    /**
     * Creates the logo/app name panel at the top of the sidebar.
     */
    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Accent gradient bar
                ThemeManager.drawAccentBar(g2, 20, 25, 40, 3);

                g2.dispose();
            }
        };
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(ThemeManager.getSidebarBackground());
        logoPanel.setBorder(new EmptyBorder(30, 20, 10, 20));
        logoPanel.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 90));

        // Bank name
        JLabel nameLabel = new JLabel(AppLanguage.get("sidebar.bank.name"));
        nameLabel.setFont(ThemeManager.getBoldFont(18));
        nameLabel.setForeground(ThemeManager.getTextLightColor());
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tagline
        JLabel tagline = new JLabel(AppLanguage.get("sidebar.tagline"));
        tagline.setFont(ThemeManager.getFont(11));
        tagline.setForeground(ThemeManager.getTextMutedColor());
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        logoPanel.add(Box.createVerticalStrut(8));
        logoPanel.add(nameLabel);
        logoPanel.add(Box.createVerticalStrut(2));
        logoPanel.add(tagline);

        return logoPanel;
    }

    /**
     * Adds a navigation item to the sidebar.
     */
    private void addNavItem(JPanel parent, BankIcon.IconType iconType, String label, String screenId) {
        NavItem item = new NavItem(iconType, label, screenId, navItems.size());
        navItems.add(item);
        parent.add(item);
    }

    /**
     * Selects a navigation item by index, updating visual styles.
     */
    public void selectItem(int index) {
        if (index < 0 || index >= navItems.size()) return;

        // Deselect previous
        if (selectedIndex >= 0 && selectedIndex < navItems.size()) {
            navItems.get(selectedIndex).setSelected(false);
        }

        // Select new
        selectedIndex = index;
        navItems.get(selectedIndex).setSelected(true);
    }

    /**
     * Sets the navigation listener callback.
     */
    public void setNavigationListener(NavigationListener listener) {
        this.navigationListener = listener;
    }

    // ==================== INNER CLASS: NavItem ====================

    /**
     * NavItem — a single clickable navigation item with smooth animations.
     *
     * Features:
     * - Animated hover/selection transitions
     * - Gradient selection indicator on the left
     * - Vector-drawn icons
     * - Professional typography
     */
    private class NavItem extends JPanel {
        private final String screenId;
        private final int index;
        private boolean selected;
        private boolean hovered;
        private final BankIcon icon;
        private final JLabel textLabel;
        private float hoverProgress = 0f; // 0..1
        private Timer animTimer;

        NavItem(BankIcon.IconType iconType, String text, String screenId, int index) {
            this.screenId = screenId;
            this.index = index;
            this.selected = false;
            this.hovered = false;

            setLayout(new BorderLayout());
            setBackground(ThemeManager.getSidebarBackground());
            setMaximumSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            setPreferredSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            setMinimumSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            setBorder(new EmptyBorder(8, 20, 8, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setOpaque(false);

            // Icon
            icon = BankIcon.create(iconType, ThemeManager.getTextMutedColor(), 18);
            icon.setPreferredSize(new Dimension(24, 24));
            icon.setMinimumSize(new Dimension(24, 24));
            icon.setMaximumSize(new Dimension(24, 24));
            icon.setOpaque(false);

            // Text label
            textLabel = new JLabel(text);
            textLabel.setFont(ThemeManager.getFont(13));
            textLabel.setForeground(ThemeManager.getTextMutedColor());

            // Place icon on the left, text stretches center
            JPanel iconWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            iconWrapper.setOpaque(false);
            iconWrapper.add(icon);
            add(iconWrapper, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);

            // Hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    startHoverAnimation(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    startHoverAnimation(false);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectItem(index);
                    if (navigationListener != null) {
                        navigationListener.onNavigate(screenId);
                    }
                }
            });
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            updateStyle();
        }

        private void startHoverAnimation(boolean forward) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }

            animTimer = new Timer(ANIM_STEPS, e -> {
                if (forward && hoverProgress < 1.0f) {
                    hoverProgress = Math.min(1.0f, hoverProgress + 0.15f);
                } else if (!forward && hoverProgress > 0.0f) {
                    hoverProgress = Math.max(0.0f, hoverProgress - 0.15f);
                } else {
                    animTimer.stop();
                }
                updateStyle();
            });
            animTimer.start();
        }

        private void updateStyle() {
            Color targetBg;
            Color targetFg;
            Color targetIconColor;
            Font targetFont;

            if (selected) {
                targetBg = ThemeManager.getSidebarItemSelected();
                targetFg = ThemeManager.getSidebarItemTextSelected();
                targetIconColor = ThemeManager.getSidebarItemTextSelected();
                targetFont = ThemeManager.getBoldFont(13);
            } else if (hovered || hoverProgress > 0) {
                targetBg = ThemeManager.lerp(ThemeManager.getSidebarBackground(),
                        ThemeManager.getSidebarItemHover(), hoverProgress);
                targetFg = ThemeManager.lerp(ThemeManager.getTextMutedColor(),
                        ThemeManager.getTextLightColor(), hoverProgress);
                targetIconColor = ThemeManager.lerp(ThemeManager.getTextMutedColor(),
                        ThemeManager.getPrimaryAccentColor(), hoverProgress);
                targetFont = ThemeManager.getFont(13);
            } else {
                targetBg = ThemeManager.getSidebarBackground();
                targetFg = ThemeManager.getTextMutedColor();
                targetIconColor = ThemeManager.getTextMutedColor();
                targetFont = ThemeManager.getFont(13);
            }

            setBackground(targetBg);
            textLabel.setForeground(targetFg);
            textLabel.setFont(targetFont);
            icon.setIconColor(targetIconColor);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Paint background
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Selection indicator bar (left edge gradient)
            if (selected) {
                GradientPaint gradient = new GradientPaint(
                        0, 0, ThemeManager.getAccentGradientStart(),
                        0, getHeight(), ThemeManager.getAccentGradientEnd());
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 4, 3, getHeight() - 8, 3, 3);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
