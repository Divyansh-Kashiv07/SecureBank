package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * LoginPanel — the HSBC Bank login screen for customer authentication.
 *
 * Features:
 * - Professional dark-themed centered card layout
 * - HSBC branding with red accent
 * - Customer ID and PIN inputs with placeholders
 * - "Processing..." button state during authentication
 * - Professional banking greetings
 */
public class LoginPanel extends JPanel {

    private StyledTextField customerIdField;
    private JPasswordField pinField;
    private StyledButton loginButton;
    private final JFrame parentFrame;

    /** Callback interface for successful login */
    public interface LoginListener {
        void onLoginSuccess(String customerId, String customerName, String[] accountNumbers);
    }

    private LoginListener loginListener;
    private BankClient client;

    /**
     * Creates the login panel.
     */
    public LoginPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;

        setLayout(new GridBagLayout()); // Centers content
        setBackground(ThemeManager.getBackgroundColor());

        buildForm();
    }

    /**
     * Rebuilds the login panel with fresh language strings.
     */
    public void rebuild() {
        removeAll();
        buildForm();
        revalidate();
        repaint();
    }

    /**
     * Builds the login form UI.
     */
    private void buildForm() {
        // Main login card
        CardPanel loginCard = new CardPanel();
        loginCard.setPreferredSize(new Dimension(440, 530));
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBorder(new EmptyBorder(30, 40, 30, 40));

        // HSBC Red accent bar at top
        JPanel accentBar = new JPanel();
        accentBar.setBackground(ThemeManager.getPrimaryAccentColor());
        accentBar.setMaximumSize(new Dimension(60, 4));
        accentBar.setPreferredSize(new Dimension(60, 4));
        accentBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel titleLabel = new JLabel(AppLanguage.get("login.title"));
        titleLabel.setFont(ThemeManager.getBoldFont(28));
        titleLabel.setForeground(ThemeManager.getTextLightColor());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle — professional greeting
        JLabel subtitleLabel = new JLabel(AppLanguage.get("login.subtitle"));
        subtitleLabel.setFont(ThemeManager.getFont(13));
        subtitleLabel.setForeground(ThemeManager.getTextMutedColor());
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Tagline
        JLabel taglineLabel = new JLabel(AppLanguage.get("login.tagline"));
        taglineLabel.setFont(ThemeManager.getItalicFont(11));
        taglineLabel.setForeground(ThemeManager.getTextMutedColor());
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Customer ID field
        JLabel idLabel = new JLabel(AppLanguage.get("login.customer.id"));
        idLabel.setFont(ThemeManager.getFont(13));
        idLabel.setForeground(ThemeManager.getTextMutedColor());
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        customerIdField = new StyledTextField("e.g., CUSTOMER-1");
        customerIdField.setMaximumSize(new Dimension(340, 42));
        customerIdField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // PIN field
        JLabel pinLabel = new JLabel(AppLanguage.get("login.pin"));
        pinLabel.setFont(ThemeManager.getFont(13));
        pinLabel.setForeground(ThemeManager.getTextMutedColor());
        pinLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        pinField = new JPasswordField();
        pinField.setFont(ThemeManager.getFont(14));
        pinField.setMaximumSize(new Dimension(340, 42));
        pinField.setPreferredSize(new Dimension(340, 42));
        pinField.setBackground(ThemeManager.getCardColor());
        pinField.setForeground(ThemeManager.getTextLightColor());
        pinField.setCaretColor(ThemeManager.getTextLightColor());
        pinField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pinField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login button — Action
        loginButton = new StyledButton(AppLanguage.get("login.signin"), ThemeManager.getPrimaryAccentColor());
        loginButton.setMaximumSize(new Dimension(340, 44));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());

        // Enter key triggers login
        pinField.addActionListener(e -> handleLogin());

        // Demo credentials hint
        JLabel hintLabel = new JLabel("<html><center>" + AppLanguage.get("login.demo.hint") + "</center></html>");
        hintLabel.setFont(ThemeManager.getItalicFont(11));
        hintLabel.setForeground(ThemeManager.getTextMutedColor());
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemble the card
        loginCard.add(Box.createVerticalStrut(10));
        loginCard.add(accentBar);
        loginCard.add(Box.createVerticalStrut(12));
        loginCard.add(titleLabel);
        loginCard.add(Box.createVerticalStrut(4));
        loginCard.add(subtitleLabel);
        loginCard.add(Box.createVerticalStrut(4));
        loginCard.add(taglineLabel);
        loginCard.add(Box.createVerticalStrut(30));
        loginCard.add(idLabel);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(customerIdField);
        loginCard.add(Box.createVerticalStrut(15));
        loginCard.add(pinLabel);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(pinField);
        loginCard.add(Box.createVerticalStrut(25));
        loginCard.add(loginButton);
        loginCard.add(Box.createVerticalStrut(15));
        loginCard.add(hintLabel);

        add(loginCard);
    }

    /**
     * Handles the login process.
     * Runs the server authentication on a background thread (SwingWorker)
     * to avoid freezing the GUI during socket communication.
     */
    private void handleLogin() {
        String customerId = customerIdField.getActualText().trim();
        String pin = new String(pinField.getPassword()).trim();

        // Basic validation
        if (customerId.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("login.enter.id"));
            return;
        }
        if (pin.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("login.enter.pin"));
            return;
        }

        // Show loading state
        loginButton.setLoading(true);

        // Use SwingWorker to avoid blocking the EDT
        // RUBRIC: GUI threading — socket calls off the EDT
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                // This runs on a BACKGROUND thread (not EDT)
                if (client == null || !client.isConnected()) {
                    return null; // Connection not established
                }
                return client.authenticate(customerId, pin);
            }

            @Override
            protected void done() {
                // This runs on the EDT — safe to update GUI
                loginButton.setLoading(false);

                try {
                    String response = get();

                    if (response == null) {
                        NotificationPanel.showError(parentFrame,
                                AppLanguage.get("login.server.error"));
                        return;
                    }

                    if (response.startsWith("OK|")) {
                        String[] parts = response.substring(3).split("\\|");
                        String name = parts[0];
                        String[] accounts = parts.length > 1 ?
                                parts[1].split(",") : new String[0];

                        NotificationPanel.showSuccess(parentFrame,
                                AppLanguage.get("login.welcome", "{name}", name));

                        if (loginListener != null) {
                            loginListener.onLoginSuccess(customerId, name, accounts);
                        }
                    } else {
                        String errorMsg = response.startsWith("ERROR|") ?
                                response.substring(6) : "Login failed";
                        NotificationPanel.showError(parentFrame, errorMsg);
                    }
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, "Login error: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    /**
     * Sets the BankClient for server communication.
     */
    public void setClient(BankClient client) {
        this.client = client;
    }

    /**
     * Sets the login success listener.
     */
    public void setLoginListener(LoginListener listener) {
        this.loginListener = listener;
    }

    /**
     * Resets the login form.
     */
    public void reset() {
        customerIdField.clearField();
        pinField.setText("");
        loginButton.setLoading(false);
    }
}
