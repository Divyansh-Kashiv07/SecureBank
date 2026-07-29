package com.securebank.gui;

import com.securebank.gui.components.CardPanel;
import com.securebank.gui.components.NotificationPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * SettingsPanel — app settings including language toggle.
 *
 * Now supports dynamically changing Theme and Font Size using ThemeManager.
 */
public class SettingsPanel extends JPanel {

    private final JFrame parentFrame;

    public SettingsPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBackground(ThemeManager.getBackgroundColor());
        buildPanel();
    }

    public void buildPanel() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        // Use JTabbedPane for "Themes Tab"
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.getBoldFont(14));
        tabbedPane.setBackground(ThemeManager.getCardColor());
        tabbedPane.setForeground(ThemeManager.getTextLightColor());

        // --- GENERAL TAB ---
        CardPanel generalCard = new CardPanel();
        generalCard.setLayout(new BoxLayout(generalCard, BoxLayout.Y_AXIS));
        generalCard.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel header = new JLabel(AppLanguage.get("settings.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        generalCard.add(header);
        generalCard.add(Box.createVerticalStrut(25));

        // Font size
        JPanel fontRow = new JPanel(new BorderLayout());
        fontRow.setOpaque(false);
        fontRow.setMaximumSize(new Dimension(370, 50));
        fontRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fontLabel = new JLabel(AppLanguage.get("settings.font.size"));
        fontLabel.setFont(ThemeManager.getFont(15));
        fontLabel.setForeground(ThemeManager.getTextLightColor());

        JComboBox<String> fontSizeSelector = new JComboBox<>(
                new String[]{"Small", "Medium", "Large"});
        
        ThemeManager.FontSize currentSize = ThemeManager.getFontSize();
        if (currentSize == ThemeManager.FontSize.SMALL) fontSizeSelector.setSelectedIndex(0);
        else if (currentSize == ThemeManager.FontSize.LARGE) fontSizeSelector.setSelectedIndex(2);
        else fontSizeSelector.setSelectedIndex(1);
        
        fontSizeSelector.setPreferredSize(new Dimension(140, 30));
        fontSizeSelector.addActionListener(e -> {
            int idx = fontSizeSelector.getSelectedIndex();
            if (idx == 0) ThemeManager.setFontSize(ThemeManager.FontSize.SMALL);
            else if (idx == 2) ThemeManager.setFontSize(ThemeManager.FontSize.LARGE);
            else ThemeManager.setFontSize(ThemeManager.FontSize.MEDIUM);
            
            if (parentFrame instanceof SecureBankApp) {
                ((SecureBankApp) parentFrame).refreshAllPanelsForLanguage();
            }
        });

        fontRow.add(fontLabel, BorderLayout.WEST);
        fontRow.add(fontSizeSelector, BorderLayout.EAST);
        generalCard.add(fontRow);
        
        generalCard.add(Box.createVerticalGlue());
        
        // Version info
        JLabel versionLabel = new JLabel(AppLanguage.get("settings.version"));
        versionLabel.setFont(ThemeManager.getItalicFont(12));
        versionLabel.setForeground(ThemeManager.getTextMutedColor());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        generalCard.add(versionLabel);

        // --- THEMES TAB ---
        CardPanel themesCard = new CardPanel();
        themesCard.setLayout(new BoxLayout(themesCard, BoxLayout.Y_AXIS));
        themesCard.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel themeHeader = new JLabel("Theme Settings");
        themeHeader.setFont(ThemeManager.getBoldFont(22));
        themeHeader.setForeground(ThemeManager.getTextLightColor());
        themeHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        themesCard.add(themeHeader);
        themesCard.add(Box.createVerticalStrut(25));

        // Theme selector
        JPanel themeRow = new JPanel(new BorderLayout());
        themeRow.setOpaque(false);
        themeRow.setMaximumSize(new Dimension(370, 50));
        themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel themeLabel = new JLabel("App Theme");
        themeLabel.setFont(ThemeManager.getFont(15));
        themeLabel.setForeground(ThemeManager.getTextLightColor());

        JComboBox<String> themeSelector = new JComboBox<>(
                new String[]{"Neon", "Navy Blue", "Darker Black"});
        
        ThemeManager.Theme currentTheme = ThemeManager.getTheme();
        if (currentTheme == ThemeManager.Theme.NEON) themeSelector.setSelectedIndex(0);
        else if (currentTheme == ThemeManager.Theme.NAVY_BLUE) themeSelector.setSelectedIndex(1);
        else themeSelector.setSelectedIndex(2);
        
        themeSelector.setPreferredSize(new Dimension(140, 30));
        themeSelector.addActionListener(e -> {
            int idx = themeSelector.getSelectedIndex();
            if (idx == 0) ThemeManager.setTheme(ThemeManager.Theme.NEON);
            else if (idx == 1) ThemeManager.setTheme(ThemeManager.Theme.NAVY_BLUE);
            else ThemeManager.setTheme(ThemeManager.Theme.DARKER_BLACK);
            
            if (parentFrame instanceof SecureBankApp) {
                ((SecureBankApp) parentFrame).refreshAllPanelsForLanguage();
            }
        });

        themeRow.add(themeLabel, BorderLayout.WEST);
        themeRow.add(themeSelector, BorderLayout.EAST);
        themesCard.add(themeRow);
        
        themesCard.add(Box.createVerticalGlue());

        // Add tabs
        tabbedPane.addTab("General", generalCard);
        tabbedPane.addTab("Themes", themesCard);
        
        tabbedPane.setPreferredSize(new Dimension(480, 400));
        add(tabbedPane);
        
        revalidate();
        repaint();
    }
}
