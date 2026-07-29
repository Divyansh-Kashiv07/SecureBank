package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import com.securebank.gui.AppLanguage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * SidebarPanel — a modern, fixed-width dark sidebar for HSBC Bank navigation.
 *
 * RUBRIC: Unit 5 — GUI with Java Swing.
 * This is a custom JPanel that provides icon + label navigation items
 * with hover effects and selection highlighting. HSBC branding with red accent.
 *
 * Layout: Fixed-width (220px), near-black background, vertical list of nav items.
 */
public class SidebarPanel extends JPanel {

    /** Width of the sidebar in pixels */
    private static final int SIDEBAR_WIDTH = 220;

    /** Nav item height — shared constant to avoid overflow */
    private static final int NAV_ITEM_HEIGHT = 44;

    /** HSBC-branded color constants */
    private static final Color BG_COLOR = ThemeManager.getBackgroundColor();
    private static final Color HOVER_COLOR = ThemeManager.getSidebarHoverColor();
    private static final Color SELECTED_COLOR = ThemeManager.getPrimaryAccentColor();
    private static final Color TEXT_COLOR = ThemeManager.getTextMutedColor();
    private static final Color SELECTED_TEXT_COLOR = ThemeManager.getTextLightColor();
    private static final Color LOGO_COLOR = ThemeManager.getTextLightColor();
    private static final Color HSBC_RED = ThemeManager.getPrimaryAccentColor();

    /** List of navigation items for managing selection state */
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

        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0)); // Fixed width, auto height
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMaximumSize(new Dimension(SIDEBAR_WIDTH, Integer.MAX_VALUE));
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        buildSidebar();
    }

    /**
     * Rebuilds the entire sidebar with fresh language strings.
     * Preserves the current selection index and navigation listener.
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
     * Called from constructor and from rebuild().
     */
    private void buildSidebar() {
        // Build the sidebar content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Logo / App name at the top
        contentPanel.add(createLogoPanel());
        contentPanel.add(Box.createVerticalStrut(20));

        // Navigation items
        addNavItem(contentPanel, "\uD83C\uDFE0", AppLanguage.get("sidebar.dashboard"), "Dashboard");     // 🏠
        addNavItem(contentPanel, "\uD83D\uDCB0", AppLanguage.get("sidebar.accounts"), "Accounts");        // 💰
        addNavItem(contentPanel, "\uD83D\uDCB3", AppLanguage.get("sidebar.deposit.withdraw"), "DepositWithdraw"); // 💳
        addNavItem(contentPanel, "\u21C4",        AppLanguage.get("sidebar.transfer"), "Transfer");   // ⇄
        addNavItem(contentPanel, "\uD83C\uDFE6", AppLanguage.get("sidebar.loans"), "Loans");              // 🏦
        addNavItem(contentPanel, "\uD83D\uDCCA", AppLanguage.get("sidebar.transactions"), "History");     // 📊
        addNavItem(contentPanel, "\uD83D\uDCC8", AppLanguage.get("sidebar.reports"), "Reports");          // 📈
        addNavItem(contentPanel, "\u2699",        AppLanguage.get("sidebar.settings"), "Settings");        // ⚙

        // Push remaining space to bottom
        contentPanel.add(Box.createVerticalGlue());

        // Logout at the bottom
        addNavItem(contentPanel, "\uD83D\uDEAA", AppLanguage.get("sidebar.logout"), "Logout");           // 🚪

        add(contentPanel, BorderLayout.CENTER);

        // Select the first item (Dashboard) by default
        if (!navItems.isEmpty()) {
            selectItem(0);
        }
    }

    /**
     * Creates the logo/app name panel at the top of the sidebar.
     * Uses HSBC branding with red hexagon accent.
     */
    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setBackground(BG_COLOR);
        logoPanel.setBorder(new EmptyBorder(25, 20, 10, 20));
        logoPanel.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 90));

        // HSBC Red hexagon accent bar
        JPanel accentBar = new JPanel();
        accentBar.setBackground(HSBC_RED);
        accentBar.setMaximumSize(new Dimension(40, 4));
        accentBar.setPreferredSize(new Dimension(40, 4));
        accentBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Bank name
        JLabel nameLabel = new JLabel(AppLanguage.get("sidebar.bank.name"));
        nameLabel.setFont(ThemeManager.getBoldFont(20));
        nameLabel.setForeground(LOGO_COLOR);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tagline
        JLabel tagline = new JLabel(AppLanguage.get("sidebar.tagline"));
        tagline.setFont(ThemeManager.getFont(11));
        tagline.setForeground(ThemeManager.getTextMutedColor());
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        logoPanel.add(accentBar);
        logoPanel.add(Box.createVerticalStrut(8));
        logoPanel.add(nameLabel);
        logoPanel.add(tagline);

        return logoPanel;
    }

    /**
     * Adds a navigation item to the sidebar.
     */
    private void addNavItem(JPanel parent, String icon, String label, String screenId) {
        NavItem item = new NavItem(icon, label, screenId, navItems.size());
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
     * NavItem — a single clickable navigation item in the sidebar.
     * Handles its own hover and selected states with HSBC Red highlight.
     *
     * Uses BorderLayout with controlled padding so content never overflows
     * the fixed NAV_ITEM_HEIGHT.
     */
    private class NavItem extends JPanel {
        private final String screenId;
        private final int index;
        private boolean selected;
        private boolean hovered;
        private final JLabel iconLabel;
        private final JLabel textLabel;

        NavItem(String icon, String text, String screenId, int index) {
            this.screenId = screenId;
            this.index = index;
            this.selected = false;
            this.hovered = false;

            // Use BorderLayout so the inner content panel controls alignment
            // without FlowLayout's unpredictable gap-based overflow
            setLayout(new BorderLayout());
            setBackground(BG_COLOR);
            setMaximumSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            setPreferredSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            setMinimumSize(new Dimension(SIDEBAR_WIDTH, NAV_ITEM_HEIGHT));
            // Reduced vertical padding (8px top/bottom) to fit within 44px
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Icon
        iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, ThemeManager.getFont(16).getSize()));
        iconLabel.setForeground(TEXT_COLOR);
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 12)); // Gap between icon and text

        // Text label
        textLabel = new JLabel(text);
        textLabel.setFont(ThemeManager.getFont(14));
        textLabel.setForeground(TEXT_COLOR);

            // Place icon on the left, text stretches center
            add(iconLabel, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);

            // Hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    updateStyle();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    updateStyle();
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

        private void updateStyle() {
            if (selected) {
                setBackground(SELECTED_COLOR);
                textLabel.setForeground(SELECTED_TEXT_COLOR);
                textLabel.setFont(ThemeManager.getBoldFont(14));
            } else if (hovered) {
                setBackground(HOVER_COLOR);
                textLabel.setForeground(SELECTED_TEXT_COLOR);
                textLabel.setFont(ThemeManager.getFont(14));
            } else {
                setBackground(BG_COLOR);
                textLabel.setForeground(TEXT_COLOR);
                textLabel.setFont(ThemeManager.getFont(14));
            }
            repaint();
        }
    }
}
