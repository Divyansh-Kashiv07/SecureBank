package com.securebank.gui.components;

import com.securebank.gui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * StyledTextField — a modern text field with placeholder text and dark-themed borders.
 *
 * Features:
 * - Placeholder text that disappears on focus
 * - Dark border with HSBC Red focus-state color change
 * - Consistent dark theme styling
 */
public class StyledTextField extends JTextField {

    private String placeholder;
    private boolean showingPlaceholder;
    private static final Color PLACEHOLDER_COLOR = ThemeManager.getTextMutedColor();
    private static final Color BORDER_COLOR = ThemeManager.getBorderColor();
    private static final Color FOCUS_BORDER_COLOR = ThemeManager.getPrimaryAccentColor();
    private static final Color TEXT_COLOR = ThemeManager.getTextLightColor();
    private static final Color BG_COLOR = ThemeManager.getCardColor();

    /**
     * Creates a styled text field with a placeholder.
     *
     * @param placeholder the placeholder text to show when empty
     */
    public StyledTextField(String placeholder) {
        this.placeholder = placeholder;
        this.showingPlaceholder = true;

        setFont(ThemeManager.getFont(14));
        setBackground(BG_COLOR);
        setCaretColor(TEXT_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        setPreferredSize(new Dimension(300, 40));

        // Show placeholder initially
        setText(placeholder);
        setForeground(PLACEHOLDER_COLOR);

        // Focus listeners for placeholder behavior
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingPlaceholder) {
                    setText("");
                    setForeground(TEXT_COLOR);
                    showingPlaceholder = false;
                }
                // Change border color on focus — HSBC Red
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(FOCUS_BORDER_COLOR, 2, true),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    setText(StyledTextField.this.placeholder);
                    setForeground(PLACEHOLDER_COLOR);
                    showingPlaceholder = true;
                }
                // Revert border color
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });
    }

    /**
     * Creates a styled text field without a placeholder.
     */
    public StyledTextField() {
        this("");
        showingPlaceholder = false;
        setForeground(TEXT_COLOR);
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
        setForeground(TEXT_COLOR);
        setText(text);
    }

    /**
     * Clears the field and restores the placeholder.
     */
    public void clearField() {
        setText(placeholder);
        setForeground(PLACEHOLDER_COLOR);
        showingPlaceholder = true;
    }
}
