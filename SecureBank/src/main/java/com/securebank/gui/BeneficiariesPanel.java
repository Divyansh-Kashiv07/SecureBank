package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.CardPanel;
import com.securebank.gui.components.NotificationPanel;
import com.securebank.gui.components.StyledButton;
import com.securebank.gui.components.StyledTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BeneficiariesPanel — manage the signed-in customer's saved payees.
 *
 * Features:
 * - Payee cards loaded over the wire via SwingWorker (never blocks the EDT)
 * - Inline add form with server-side validation surfaced as toasts
 * - Per-payee remove with instant list refresh
 * - Loading skeletons, empty state, and full language rebuild support
 */
public class BeneficiariesPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private JPanel listPanel;

    /** Add-form fields (rebuilt with the panel on language change) */
    private StyledTextField nameField;
    private StyledTextField accountField;
    private StyledTextField bankField;
    private StyledTextField nicknameField;

    public BeneficiariesPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));
        buildPanel();
    }

    /** Builds (or rebuilds) the whole screen — used on language change too. */
    public void buildPanel() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        JLabel header = new JLabel(AppLanguage.get("bene.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(header, BorderLayout.NORTH);

        // Center: list + form stacked vertically
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        center.add(buildAddForm());
        center.add(Box.createVerticalStrut(20));

        JLabel listTitle = new JLabel(AppLanguage.get("bene.your.payees"));
        listTitle.setFont(ThemeManager.getBoldFont(16));
        listTitle.setForeground(ThemeManager.getTextLightColor());
        listTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        center.add(listTitle);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        showSkeletons();
        center.add(listPanel);

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /** The "save a new payee" card with four inline fields. */
    private CardPanel buildAddForm() {
        CardPanel form = new CardPanel(AppLanguage.get("bene.add.title"), ThemeManager.getPrimaryAccentColor());
        form.setLayout(new GridLayout(0, 2, 12, 10));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(new EmptyBorder(16, 20, 16, 20));

        nameField = new StyledTextField(AppLanguage.get("bene.name"));
        accountField = new StyledTextField(AppLanguage.get("bene.account"));
        bankField = new StyledTextField(AppLanguage.get("bene.bank"));
        nicknameField = new StyledTextField(AppLanguage.get("bene.nickname"));

        form.add(nameField);
        form.add(accountField);
        form.add(bankField);
        form.add(nicknameField);

        StyledButton save = new StyledButton(AppLanguage.get("bene.add.button"), ThemeManager.getPrimaryAccentColor());
        save.addActionListener(e -> addPayee(save));
        form.add(save);

        return form;
    }

    /** Loading skeleton rows shown until data arrives. */
    private void showSkeletons() {
        listPanel.removeAll();
        for (int i = 0; i < 3; i++) {
            JPanel skeleton = new JPanel(new BorderLayout());
            skeleton.setOpaque(false);
            skeleton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            skeleton.setBorder(new EmptyBorder(14, 20, 14, 20));
            skeleton.add(new JLabel("░░░░░░░░░░░░░░░░░░░░"), BorderLayout.WEST);
            skeleton.add(new JLabel("░░░░░░░░░"), BorderLayout.EAST);
            for (Component c : skeleton.getComponents()) {
                ((JLabel) c).setFont(ThemeManager.getBoldFont(14));
                ((JLabel) c).setForeground(ThemeManager.getBorderColor());
            }
            listPanel.add(skeleton);
            listPanel.add(Box.createVerticalStrut(12));
        }
    }

    /** Loads the saved payees from the server without blocking the UI. */
    public void loadBeneficiaries() {
        if (client == null || !client.isConnected() || customerId == null) return;

        showSkeletons();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.listBeneficiaries();
            }

            @Override
            protected void done() {
                try {
                    renderList(get());
                } catch (Exception e) {
                    listPanel.removeAll();
                    JLabel error = new JLabel(AppLanguage.get("common.server.error"));
                    error.setFont(ThemeManager.getItalicFont(14));
                    error.setForeground(ThemeManager.getDangerColor());
                    listPanel.add(error);
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        }.execute();
    }

    /** Turns an "OK|id|name|acc|bank|nick;..." response into payee cards. */
    private void renderList(String response) {
        listPanel.removeAll();

        List<String[]> payees = new ArrayList<>();
        if (response != null && response.startsWith("OK|") && !response.equals("OK|EMPTY")) {
            for (String record : response.substring(3).split(";")) {
                String[] f = record.split("\\|");
                if (f.length >= 5) payees.add(f);
            }
        }

        if (payees.isEmpty()) {
            JLabel empty = new JLabel(AppLanguage.get("bene.none"));
            empty.setFont(ThemeManager.getItalicFont(14));
            empty.setForeground(ThemeManager.getTextMutedColor());
            listPanel.add(empty);
            return;
        }

        for (String[] f : payees) {
            listPanel.add(createPayeeCard(f[0], f[1], f[2], f[3], f[4]));
            listPanel.add(Box.createVerticalStrut(12));
        }
    }

    /** One payee card: avatar chip, name/account/bank, and a remove button. */
    private CardPanel createPayeeCard(String id, String name, String account, String bank, String nickname) {
        CardPanel card = new CardPanel(null, ThemeManager.getSuccessColor());
        card.setLayout(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Avatar chip: first letter of the payee
        JPanel left = new JPanel(new BorderLayout(14, 0));
        left.setOpaque(false);
        JLabel avatar = new JLabel(name.substring(0, 1).toUpperCase()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getPrimaryAccentColor());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(ThemeManager.getBoldFont(16));
        avatar.setForeground(Color.WHITE);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(40, 40));
        left.add(avatar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel nameLabel = new JLabel(nickname.isEmpty() ? name : name + "  ·  " + nickname);
        nameLabel.setFont(ThemeManager.getBoldFont(14));
        nameLabel.setForeground(ThemeManager.getTextLightColor());

        JLabel accLabel = new JLabel(account + "  •  " + bank);
        accLabel.setFont(ThemeManager.getFont(12));
        accLabel.setForeground(ThemeManager.getTextMutedColor());

        text.add(nameLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(accLabel);
        left.add(text, BorderLayout.CENTER);

        card.add(left, BorderLayout.CENTER);

        StyledButton remove = new StyledButton(AppLanguage.get("bene.remove"), ThemeManager.getDangerColor());
        remove.setPreferredSize(new Dimension(100, 34));
        remove.addActionListener(e -> removePayee(id, name, remove));
        card.add(remove, BorderLayout.EAST);

        return card;
    }

    /** Sends BENEFICIARY_ADD off the EDT and refreshes the list on success. */
    private void addPayee(StyledButton saveButton) {
        String name = nameField.getText().trim();
        String account = accountField.getText().trim();
        if (name.isEmpty() || account.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("bene.required"));
            return;
        }
        if (client == null || !client.isConnected()) {
            NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
            return;
        }

        String bank = bankField.getText().trim();
        String nickname = nicknameField.getText().trim();
        saveButton.setEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.addBeneficiary(name, account, bank, nickname);
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    handleAddResponse(get());
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
                }
            }
        }.execute();
    }

    /** Applies a BENEFICIARY_ADD response on the EDT (toast + list refresh). */
    private void handleAddResponse(String response) {
        if (response != null && response.startsWith("OK|")) {
            NotificationPanel.showSuccess(parentFrame, AppLanguage.get("bene.added"));
            clearForm();
            loadBeneficiaries();
        } else {
            String error = response != null && response.startsWith("ERROR|")
                    ? response.substring(6) : AppLanguage.get("common.server.error");
            NotificationPanel.showError(parentFrame, error);
        }
    }

    /** Sends BENEFICIARY_REMOVE off the EDT and refreshes the list on success. */
    private void removePayee(String id, String name, StyledButton removeButton) {
        if (client == null || !client.isConnected()) {
            NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
            return;
        }

        removeButton.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.removeBeneficiary(id);
            }

            @Override
            protected void done() {
                removeButton.setEnabled(true);
                try {
                    handleRemoveResponse(get(), name);
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
                }
            }
        }.execute();
    }

    /** Applies a BENEFICIARY_REMOVE response on the EDT (toast + list refresh). */
    private void handleRemoveResponse(String response, String name) {
        if (response != null && response.startsWith("OK|")) {
            NotificationPanel.showInfo(parentFrame,
                    AppLanguage.get("bene.removed", "{name}", name));
            loadBeneficiaries();
        } else {
            String error = response != null && response.startsWith("ERROR|")
                    ? response.substring(6) : AppLanguage.get("common.server.error");
            NotificationPanel.showError(parentFrame, error);
        }
    }

    private void clearForm() {
        nameField.setText("");
        accountField.setText("");
        bankField.setText("");
        nicknameField.setText("");
    }

    public void setSessionData(String customerId) {
        this.customerId = customerId;
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
