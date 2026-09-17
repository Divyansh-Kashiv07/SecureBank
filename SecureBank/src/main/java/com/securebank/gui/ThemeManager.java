package com.securebank.gui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * ThemeManager — Centralized UI styling for SecureBank.
 * Handles different themes (Neon, Navy Blue, Darker Black) and Font Sizes globally.
 *
 * Production-grade design tokens:
 * - Gradients, glass effects, shadows
 * - Table styling (alternating rows, headers, selection)
 * - Animation timing curves
 * - Consistent spacing scale
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
    private static Theme currentTheme = Theme.DARKER_BLACK;
    private static FontSize currentFontSize = FontSize.MEDIUM;

    // Core font name
    private static final String FONT_FAMILY = "Segoe UI";

    // ==================== RADIUS SCALE ====================
    public static final int RADIUS_SM = 6;
    public static final int RADIUS_MD = 12;
    public static final int RADIUS_LG = 18;
    public static final int RADIUS_PILL = 999;

    // ==================== SHADOW CONSTANTS ====================
    public static final float SHADOW_BLUR = 12f;
    public static final int SHADOW_OFFSET = 3;

    // ==================== ANIMATION TIMING ====================
    public static final int ANIM_FAST = 150;
    public static final int ANIM_NORMAL = 300;
    public static final int ANIM_SLOW = 500;

    // ==================== FONT METHODS ====================

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

    // ==================== CORE COLORS ====================

    public static Color getBackgroundColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x0B, 0x0C, 0x10);
            case NAVY_BLUE: return new Color(0x0A, 0x19, 0x2F);
            case DARKER_BLACK: return new Color(0x0A, 0x0A, 0x0A);
            default: return new Color(0x14, 0x14, 0x1E);
        }
    }

    public static Color getCardColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x1F, 0x28, 0x33);
            case NAVY_BLUE: return new Color(0x11, 0x22, 0x40);
            case DARKER_BLACK: return new Color(0x14, 0x14, 0x14);
            default: return new Color(0x1F, 0x23, 0x2C);
        }
    }

    public static Color getCardColorElevated() {
        switch (currentTheme) {
            case NEON: return new Color(0x25, 0x30, 0x3C);
            case NAVY_BLUE: return new Color(0x16, 0x2A, 0x4A);
            case DARKER_BLACK: return new Color(0x1A, 0x1A, 0x1A);
            default: return new Color(0x24, 0x28, 0x33);
        }
    }

    public static Color getTextLightColor() {
        switch (currentTheme) {
            case NEON: return new Color(0xC5, 0xC6, 0xC7);
            case NAVY_BLUE: return new Color(0xCC, 0xD6, 0xF6);
            case DARKER_BLACK: return new Color(0xF0, 0xF0, 0xF0);
            default: return new Color(0xE0, 0xE0, 0xE0);
        }
    }

    public static Color getTextMutedColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x7F, 0x8C, 0x8D);
            case NAVY_BLUE: return new Color(0x88, 0x92, 0xB0);
            case DARKER_BLACK: return new Color(0x70, 0x70, 0x70);
            default: return new Color(0x77, 0x88, 0x99);
        }
    }

    public static Color getPrimaryAccentColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x66, 0xFC, 0xF1);
            case NAVY_BLUE: return new Color(0x64, 0xFF, 0xDA);
            case DARKER_BLACK: return new Color(0xBB, 0x86, 0xFC);
            default: return new Color(0x34, 0x98, 0xDB);
        }
    }

    public static Color getSidebarHoverColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x45, 0xA2, 0x9E);
            case NAVY_BLUE: return new Color(0x23, 0x35, 0x54);
            case DARKER_BLACK: return new Color(0x22, 0x22, 0x22);
            default: return new Color(0x2C, 0x3E, 0x50);
        }
    }

    public static Color getBorderColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x45, 0xA2, 0x9E);
            case NAVY_BLUE: return new Color(0x23, 0x35, 0x54);
            case DARKER_BLACK: return new Color(0x28, 0x28, 0x28);
            default: return new Color(0x3C, 0x4A, 0x5A);
        }
    }

    // ==================== STATUS COLORS ====================
    public static Color getSuccessColor() { return new Color(0x2E, 0xCC, 0x71); }
    public static Color getDangerColor() { return new Color(0xE7, 0x4C, 0x3C); }
    public static Color getWarningColor() { return new Color(0xF1, 0xC4, 0x0F); }
    public static Color getInfoColor() { return new Color(0x34, 0x98, 0xDB); }

    // ==================== GRADIENT COLORS ====================

    private static Color getGradientStart() {
        switch (currentTheme) {
            case NEON: return new Color(0x0D, 0x0E, 0x14);
            case NAVY_BLUE: return new Color(0x06, 0x12, 0x24);
            case DARKER_BLACK: return new Color(0x06, 0x06, 0x0C);
            default: return new Color(0x0E, 0x12, 0x1A);
        }
    }

    private static Color getGradientEnd() {
        switch (currentTheme) {
            case NEON: return new Color(0x14, 0x1A, 0x24);
            case NAVY_BLUE: return new Color(0x0E, 0x1C, 0x38);
            case DARKER_BLACK: return new Color(0x10, 0x10, 0x18);
            default: return new Color(0x18, 0x1E, 0x28);
        }
    }

    public static Color getAccentGradientStart() {
        switch (currentTheme) {
            case NEON: return new Color(0x4E, 0xFC, 0xF1);
            case NAVY_BLUE: return new Color(0x4E, 0xE8, 0xDA);
            case DARKER_BLACK: return new Color(0xA0, 0x70, 0xF0);
            default: return new Color(0x28, 0x8C, 0xE8);
        }
    }

    public static Color getAccentGradientEnd() {
        switch (currentTheme) {
            case NEON: return new Color(0x00, 0xD4, 0xAA);
            case NAVY_BLUE: return new Color(0x30, 0xD0, 0xC0);
            case DARKER_BLACK: return new Color(0xD0, 0x80, 0xFC);
            default: return new Color(0x1E, 0x6E, 0xC8);
        }
    }

    // ==================== GLASS EFFECT COLORS ====================

    public static Color getGlassColor() {
        switch (currentTheme) {
            case NEON: return new Color(0x1F, 0x28, 0x33, 180);
            case NAVY_BLUE: return new Color(0x11, 0x22, 0x40, 180);
            case DARKER_BLACK: return new Color(0x14, 0x14, 0x14, 160);
            default: return new Color(0x1F, 0x23, 0x2C, 170);
        }
    }

    public static Color getGlassBorder() {
        switch (currentTheme) {
            case NEON: return new Color(0x45, 0xA2, 0x9E, 80);
            case NAVY_BLUE: return new Color(0x23, 0x35, 0x54, 80);
            case DARKER_BLACK: return new Color(0x40, 0x40, 0x40, 60);
            default: return new Color(0x3C, 0x4A, 0x5A, 70);
        }
    }

    // ==================== TABLE COLORS ====================

    public static Color getTableHeaderBackground() {
        switch (currentTheme) {
            case NEON: return new Color(0x16, 0x1E, 0x28);
            case NAVY_BLUE: return new Color(0x0C, 0x1A, 0x34);
            case DARKER_BLACK: return new Color(0x10, 0x10, 0x10);
            default: return new Color(0x18, 0x1C, 0x24);
        }
    }

    public static Color getTableHeaderForeground() {
        return getPrimaryAccentColor();
    }

    public static Color getTableRowEven() {
        switch (currentTheme) {
            case NEON: return new Color(0x1A, 0x24, 0x2E);
            case NAVY_BLUE: return new Color(0x0E, 0x1C, 0x36);
            case DARKER_BLACK: return new Color(0x10, 0x10, 0x10);
            default: return new Color(0x1A, 0x1E, 0x26);
        }
    }

    public static Color getTableRowOdd() {
        switch (currentTheme) {
            case NEON: return new Color(0x16, 0x20, 0x2A);
            case NAVY_BLUE: return new Color(0x0A, 0x16, 0x2C);
            case DARKER_BLACK: return new Color(0x0D, 0x0D, 0x0D);
            default: return new Color(0x16, 0x1A, 0x22);
        }
    }

    public static Color getTableSelectionBackground() {
        Color accent = getPrimaryAccentColor();
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40);
    }

    public static Color getTableSelectionForeground() {
        return getTextLightColor();
    }

    public static Color getTableGridColor() {
        return getBorderColor();
    }

    // ==================== SIDEBAR COLORS ====================

    public static Color getSidebarBackground() {
        return getBackgroundColor();
    }

    public static Color getSidebarItemSelected() {
        return getPrimaryAccentColor();
    }

    public static Color getSidebarItemHover() {
        return getSidebarHoverColor();
    }

    public static Color getSidebarItemText() {
        return getTextMutedColor();
    }

    public static Color getSidebarItemTextSelected() {
        return Color.WHITE;
    }

    // ==================== PAINTING UTILITIES ====================

    /**
     * Draws a shadow behind a rounded rectangle.
     */
    public static void drawShadow(Graphics2D g2, int x, int y, int w, int h, int radius) {
        for (int i = 0; i < SHADOW_BLUR; i++) {
            float alpha = 0.08f * (1f - (float) i / SHADOW_BLUR);
            g2.setColor(new Color(0, 0, 0, Math.max(0, (int)(alpha * 255))));
            g2.fill(new RoundRectangle2D.Float(
                    x - i, y - i, w + 2 * i, h + 2 * i, radius + i, radius + i));
        }
    }

    /**
     * Draws a gradient background for panels.
     */
    public static void drawGradientBackground(Graphics2D g2, int w, int h) {
        GradientPaint gradient = new GradientPaint(
                0, 0, getGradientStart(),
                0, h, getGradientEnd());
        g2.setPaint(gradient);
        g2.fillRect(0, 0, w, h);
    }

    /**
     * Draws a glass-effect card background.
     */
    public static void drawGlassCard(Graphics2D g2, int x, int y, int w, int h, int radius) {
        // Glass background
        g2.setColor(getGlassColor());
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, radius, radius));

        // Glass border
        g2.setColor(getGlassBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, radius, radius));

        // Top highlight
        g2.setColor(new Color(255, 255, 255, 15));
        g2.fill(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h / 3, radius, radius));
    }

    /**
     * Draws an accent gradient bar.
     */
    public static void drawAccentBar(Graphics2D g2, int x, int y, int w, int h) {
        GradientPaint gradient = new GradientPaint(
                x, y, getAccentGradientStart(),
                x + w, y, getAccentGradientEnd());
        g2.setPaint(gradient);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
    }

    /**
     * Interpolates between two colors.
     */
    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

    /**
     * Creates a slightly lighter version of a color.
     */
    public static Color lighten(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Creates a slightly darker version of a color.
     */
    public static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b, color.getAlpha());
    }
}
