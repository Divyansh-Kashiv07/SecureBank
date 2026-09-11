package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.BankIcon;
import com.securebank.gui.components.NotificationPanel;
import com.securebank.gui.components.StyledButton;
import com.securebank.gui.components.StyledTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginPanel — the SecureBank login screen with production-grade visual polish.
 *
 * Layout strategy (deterministic alignment):
 * The card uses a single-column GridBagLayout. Every row fills the same column
 * width, so the field labels, input boxes, and the Sign In button always share
 * one flush left edge and one center axis — regardless of font metrics or
 * platform look-and-feel. No per-component alignmentX hints are used.
 *
 * Features:
 * - Animated gradient background
 * - Glass-effect login card with vector logo badge
 * - Vector-drawn eye toggle for PIN visibility (no emoji dependency)
 * - Smooth fade-in animation on mount
 * - Accent gradient bar
 */
public class LoginPanel extends JPanel {

    private StyledTextField customerIdField;
    private JPasswordField pinField;
    private StyledButton loginButton;
    private final JFrame parentFrame;

    /** Card geometry: content width = card width − 2 × horizontal padding */
    private static final int CARD_WIDTH = 460;
    private static final int CARD_HEIGHT = 600;
    private static final int CARD_PADDING_X = 40;

    /** Animation state */
    private float fadeInProgress = 0f;
    private Timer fadeTimer;

    private EyeToggleButton eyeToggle;

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

        setLayout(new GridBagLayout());
        setBackground(ThemeManager.getBackgroundColor());

        buildForm();
        startFadeIn();
    }

    /**
     * Starts the fade-in animation.
     */
    private void startFadeIn() {
        fadeInProgress = 0f;
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
        fadeTimer = new Timer(16, e -> {
            fadeInProgress = Math.min(1.0f, fadeInProgress + 0.05f);
            setOpaque(fadeInProgress >= 1.0f);
            repaint();
            if (fadeInProgress >= 1.0f) {
                fadeTimer.stop();
            }
        });
        fadeTimer.start();
    }

    /**
     * Rebuilds the login panel with fresh language strings.
     */
    public void rebuild() {
        removeAll();
        buildForm();
        startFadeIn();
        revalidate();
        repaint();
    }

    /**
     * GridBag constraints for the next row of the single-column form.
     */
    private GridBagConstraints row(int anchor, int fill, int bottomInset) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.anchor = anchor;
        gbc.fill = fill;
        gbc.insets = new Insets(0, 0, bottomInset, 0);
        return gbc;
    }

    /**
     * Builds the login form UI.
     */
    private void buildForm() {
        // Main login card — glass effect with shadow
        JPanel loginCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Apply fade
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeInProgress));

                // Shadow
                ThemeManager.drawShadow(g2, 6, 6, getWidth() - 6, getHeight() - 6, 16);

                // Glass background
                ThemeManager.drawGlassCard(g2, 0, 0, getWidth(), getHeight(), 16);

                g2.dispose();
            }
        };
        loginCard.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        loginCard.setLayout(new GridBagLayout());
        loginCard.setOpaque(false);
        loginCard.setBorder(new EmptyBorder(30, CARD_PADDING_X, 28, CARD_PADDING_X));

        // ---- Logo badge: rounded gradient square with vector shield ----
        JPanel logoBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ThemeManager.getAccentGradientStart(),
                        getWidth(), getHeight(), ThemeManager.getAccentGradientEnd());
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(54, 54));
        logoBadge.add(new BankIcon(BankIcon.IconType.SHIELD, Color.WHITE, 30));
        loginCard.add(logoBadge, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 12));

        // ---- Accent gradient bar ----
        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.drawAccentBar(g2, 0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        accentBar.setPreferredSize(new Dimension(60, 3));
        loginCard.add(accentBar, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 14));

        // ---- Title block (all centered on the shared axis) ----
        JLabel titleLabel = new JLabel(AppLanguage.get("login.title"));
        titleLabel.setFont(ThemeManager.getBoldFont(28));
        titleLabel.setForeground(ThemeManager.getTextLightColor());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginCard.add(titleLabel, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 4));

        JLabel subtitleLabel = new JLabel(AppLanguage.get("login.subtitle"));
        subtitleLabel.setFont(ThemeManager.getFont(13));
        subtitleLabel.setForeground(ThemeManager.getTextMutedColor());
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginCard.add(subtitleLabel, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 4));

        JLabel taglineLabel = new JLabel(AppLanguage.get("login.tagline"));
        taglineLabel.setFont(ThemeManager.getItalicFont(11));
        taglineLabel.setForeground(ThemeManager.getTextMutedColor());
        taglineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginCard.add(taglineLabel, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 26));

        // ---- Form rows: labels and fields share one flush left edge ----
        JLabel idLabel = new JLabel(AppLanguage.get("login.customer.id"));
        idLabel.setFont(ThemeManager.getFont(13));
        idLabel.setForeground(ThemeManager.getTextMutedColor());
        loginCard.add(idLabel, row(GridBagConstraints.LINE_START, GridBagConstraints.NONE, 6));

        customerIdField = new StyledTextField("e.g., CUSTOMER-1");
        loginCard.add(customerIdField, row(GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, 16));

        JLabel pinLabel = new JLabel(AppLanguage.get("login.pin"));
        pinLabel.setFont(ThemeManager.getFont(13));
        pinLabel.setForeground(ThemeManager.getTextMutedColor());
        loginCard.add(pinLabel, row(GridBagConstraints.LINE_START, GridBagConstraints.NONE, 6));

        // PIN field with vector visibility toggle
        JPanel pinFieldWrapper = new JPanel(new BorderLayout());
        pinFieldWrapper.setOpaque(false);

        pinField = new JPasswordField();
        pinField.setFont(ThemeManager.getFont(14));
        pinField.setBackground(ThemeManager.getCardColor());
        pinField.setForeground(ThemeManager.getTextLightColor());
        pinField.setCaretColor(ThemeManager.getTextLightColor());
        pinField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                new EmptyBorder(8, 12, 8, 46)
        ));
        pinField.setEchoChar('•');
        pinField.addActionListener(e -> handleLogin());

        eyeToggle = new EyeToggleButton();
        pinFieldWrapper.add(pinField, BorderLayout.CENTER);
        pinFieldWrapper.add(eyeToggle, BorderLayout.EAST);
        loginCard.add(pinFieldWrapper, row(GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, 24));

        // ---- Sign In button: same column width as the fields ----
        loginButton = new StyledButton(AppLanguage.get("login.signin"), ThemeManager.getPrimaryAccentColor());
        loginButton.setPreferredSize(new Dimension(200, 46));
        loginButton.addActionListener(e -> handleLogin());
        loginCard.add(loginButton, row(GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, 14));

        // ---- Demo credentials hint (centered) ----
        JLabel hintLabel = new JLabel(AppLanguage.get("login.demo.hint"));
        hintLabel.setFont(ThemeManager.getItalicFont(11));
        hintLabel.setForeground(ThemeManager.getTextMutedColor());
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginCard.add(hintLabel, row(GridBagConstraints.CENTER, GridBagConstraints.NONE, 0));

        add(loginCard);
    }

    /**
     * Paints the animated gradient background.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Animated gradient background
        ThemeManager.drawGradientBackground(g2, getWidth(), getHeight());

        // Subtle radial glow at center
        float glowAlpha = 0.04f * fadeInProgress;
        for (int i = 0; i < 5; i++) {
            int r = 200 + i * 100;
            g2.setColor(new Color(ThemeManager.getPrimaryAccentColor().getRed(),
                    ThemeManager.getPrimaryAccentColor().getGreen(),
                    ThemeManager.getPrimaryAccentColor().getBlue(),
                    Math.max(0, (int)(glowAlpha * 255 * (1 - i * 0.2)))));
            g2.fillOval(getWidth() / 2 - r, getHeight() / 2 - r, r * 2, r * 2);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Handles the login process.
     */
    private void handleLogin() {
        String customerId = customerIdField.getActualText().trim();
        String pin = new String(pinField.getPassword()).trim();

        if (customerId.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("login.enter.id"));
            return;
        }
        if (pin.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("login.enter.pin"));
            return;
        }

        loginButton.setLoading(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                if (client == null || !client.isConnected()) {
                    return null;
                }
                return client.authenticate(customerId, pin);
            }

            @Override
            protected void done() {
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
        eyeToggle.setShown(false);
        startFadeIn();
    }

    /**
     * Vector-drawn eye toggle for PIN visibility — replaces the emoji
     * (👁/🙈) which renders inconsistently across platforms.
     * Draws an eye outline with a pupil; adds a diagonal slash when the
     * PIN is currently visible.
     */
    private final class EyeToggleButton extends JComponent {

        private boolean shown = false;

        EyeToggleButton() {
            setPreferredSize(new Dimension(42, 44));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setShown(!shown);
                }
            });
        }

        void setShown(boolean pinVisible) {
            this.shown = pinVisible;
            pinField.setEchoChar(pinVisible ? '\0' : '•');
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color ink = shown
                    ? ThemeManager.getPrimaryAccentColor()
                    : ThemeManager.getTextMutedColor();
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int halfW = 8;
            int halfH = 5;

            // Eye outline (two symmetric quadratic curves)
            Path2D eye = new Path2D.Float();
            eye.moveTo(cx - halfW, cy);
            eye.quadTo(cx, cy - halfH * 2.4, cx + halfW, cy);
            eye.quadTo(cx, cy + halfH * 2.4, cx - halfW, cy);
            eye.closePath();
            g2.draw(eye);

            // Pupil — slightly larger when hidden (closed state emphasis)
            int pupilR = shown ? 3 : 4;
            g2.fillOval(cx - pupilR / 2, cy - pupilR / 2, pupilR, pupilR);

            // Slash when the PIN is visible
            if (shown) {
                g2.drawLine(cx - halfW - 3, cy + halfH * 2, cx + halfW + 3, cy - halfH * 2);
            }

            g2.dispose();
        }
    }
}
