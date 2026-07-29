package com.securebank.gui;

import java.awt.*;

/**
 * ThemeManager — Centralized UI styling for SecureBank.
 * Handles different themes (Neon, Navy Blue, Darker Black) and Font Sizes globally.
 */
public class ThemeManager {

    public enum Theme {
        NEON,
        NAVY_BLUE,
        DARKER_BLACK
    }

    public enum FontSize {
        SMALL(-2),
        MEDIUM(0),
        LARGE(2);

        private final int offset;
        FontSize(int offset) { this.offset = offset; }
        public int getOffset() { return offset; }
    }

    // Default states
    private static Theme currentTheme = Theme.DARKER_BLACK; // Default to user request
    private static FontSize currentFontSize = FontSize.MEDIUM;
    
    // Core font name
    private static final String FONT_FAMILY = "Segoe UI";

    public static void setTheme(Theme theme) {
        currentTheme = theme;
    }

    public static Theme getTheme() {
        return currentTheme;
    }

    public static void setFontSize(FontSize size) {
        currentFontSize = size;
    }

    public static FontSize getFontSize() {
        return currentFontSize;
    }

    /**
     * Gets a font with the global size offset applied.
     */
    public static Font getFont(int style, int baseSize) {
        return new Font(FONT_FAMILY, style, baseSize + currentFontSize.getOffset());
    }
    
    public static Font getFont(int baseSize) {
        return getFont(Font.PLAIN, baseSize);
    }
    
    public static Font getBoldFont(int baseSize) {
        return getFont(Font.BOLD, baseSize);
    }
    
    public static Font getItalicFont(int baseSize) {
        return getFont(Font.ITALIC, baseSize);
    }

    // ==================== COLORS ====================

    public static Color getBackgroundColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x0B, 0x0C, 0x10);
            case NAVY_BLUE: return new Color(0x0A, 0x19, 0x2F);
            case DARKER_BLACK: return new Color(0x00, 0x00, 0x00);
            default: return new Color(0x14, 0x14, 0x1E);
        }
    }

    public static Color getCardColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x1F, 0x28, 0x33);
            case NAVY_BLUE: return new Color(0x11, 0x22, 0x40);
            case DARKER_BLACK: return new Color(0x12, 0x12, 0x12);
            default: return new Color(0x1F, 0x23, 0x2C);
        }
    }

    public static Color getTextLightColor() {
        switch (currentTheme) {
            case NEON: return new Color(0xC5, 0xC6, 0xC7);
            case NAVY_BLUE: return new Color(0xCC, 0xD6, 0xF6);
            case DARKER_BLACK: return new Color(0xFF, 0xFF, 0xFF);
            default: return new Color(0xE0, 0xE0, 0xE0);
        }
    }

    public static Color getTextMutedColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x7F, 0x8C, 0x8D);
            case NAVY_BLUE: return new Color(0x88, 0x92, 0xB0);
            case DARKER_BLACK: return new Color(0x88, 0x88, 0x88);
            default: return new Color(0x77, 0x88, 0x99);
        }
    }

    public static Color getPrimaryAccentColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x66, 0xFC, 0xF1); // Neon Cyan
            case NAVY_BLUE: return new Color(0x64, 0xFF, 0xDA); // Teal
            case DARKER_BLACK: return new Color(0xBB, 0x86, 0xFC); // Material Purple
            default: return new Color(0x34, 0x98, 0xDB);
        }
    }
    
    public static Color getSidebarHoverColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x45, 0xA2, 0x9E); 
            case NAVY_BLUE: return new Color(0x23, 0x35, 0x54); 
            case DARKER_BLACK: return new Color(0x2A, 0x2A, 0x2A); 
            default: return new Color(0x2C, 0x3E, 0x50);
        }
    }
    
    public static Color getBorderColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x45, 0xA2, 0x9E);
            case NAVY_BLUE: return new Color(0x23, 0x35, 0x54);
            case DARKER_BLACK: return new Color(0x33, 0x33, 0x33);
            default: return new Color(0x3C, 0x4A, 0x5A);
        }
    }
    
    // Status colors
    public static Color getSuccessColor() { return new Color(0x2E, 0xCC, 0x71); }
    public static Color getDangerColor() { return new Color(0xE7, 0x4C, 0x3C); }
    public static Color getWarningColor() { return new Color(0xF1, 0xC4, 0x0F); }
}
