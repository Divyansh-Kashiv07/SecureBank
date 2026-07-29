package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * TransferPanel — fund transfer form between accounts.
 */
public class TransferPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String[] accountNumbers;

    private JComboBox<String> fromAccountSelector;
    private StyledTextField toAccountField;
    private StyledTextField amountField;
    private StyledButton transferButton;



    public TransferPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBackground(ThemeManager.getBackgroundColor());
        buildForm();
    }

    public void buildForm() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        CardPanel formCard = new CardPanel(AppLanguage.get("transfer.title"), StyledButton.ACCENT_TEAL);
        formCard.setPreferredSize(new Dimension(480, 440));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(20, 35, 20, 35));

        formCard.add(Box.createVerticalStrut(25));

        // From account
        addLabel(formCard, AppLanguage.get("transfer.from"));
        fromAccountSelector = new JComboBox<>();
        fromAccountSelector.setFont(ThemeManager.getFont(14));
        fromAccountSelector.setMaximumSize(new Dimension(400, 40));
        fromAccountSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(fromAccountSelector);
        formCard.add(Box.createVerticalStrut(15));

        // To account
        addLabel(formCard, AppLanguage.get("transfer.to"));
        toAccountField = new StyledTextField("e.g., ACC-001003");
        toAccountField.setMaximumSize(new Dimension(400, 42));
        toAccountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(toAccountField);
        formCard.add(Box.createVerticalStrut(15));

        // Amount
        addLabel(formCard, AppLanguage.get("transfer.amount"));
        amountField = new StyledTextField(AppLanguage.get("dw.enter.amount"));
        amountField.setMaximumSize(new Dimension(400, 42));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(amountField);
        formCard.add(Box.createVerticalStrut(25));

        // Transfer button
        transferButton = new StyledButton(AppLanguage.get("transfer.button"), StyledButton.ACCENT_TEAL);
        transferButton.setMaximumSize(new Dimension(400, 44));
        transferButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferButton.addActionListener(e -> handleTransfer());
        formCard.add(transferButton);

        add(formCard);
        revalidate();
        repaint();
    }

    private void addLabel(JPanel parent, String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.getFont(13));
        label.setForeground(ThemeManager.getTextMutedColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(6));
    }

    private void handleTransfer() {
        if (fromAccountSelector.getSelectedItem() == null) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("transfer.select.source"));
            return;
        }

        String fromAcc = fromAccountSelector.getSelectedItem().toString().split(" ")[0];
        String toAcc = toAccountField.getActualText().trim();
        double amount;

        if (toAcc.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("transfer.enter.target"));
            return;
        }

        String amountText = amountField.getActualText().trim();
        if (amountText.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("transfer.invalid.amount"));
            return;
        }

        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                NotificationPanel.showWarning(parentFrame, AppLanguage.get("transfer.positive.amount"));
                return;
            }
        } catch (NumberFormatException e) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("transfer.invalid.amount"));
            return;
        }

        transferButton.setLoading(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.transfer(fromAcc, toAcc, amount);
            }

            @Override
            protected void done() {
                transferButton.setLoading(false);
                try {
                    String response = get();
                    if (response == null) {
                        NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
                        return;
                    }
                    if (response.startsWith("OK|")) {
                        NotificationPanel.showSuccess(parentFrame, AppLanguage.get("transfer.success"));
                        toAccountField.clearField();
                        amountField.clearField();
                    } else {
                        String error = response.startsWith("ERROR|") ?
                                response.substring(6) : response;
                        NotificationPanel.showError(parentFrame, error);
                    }
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.error") + ": " + e.getMessage());
                }
            }
        }.execute();
    }

    public void setSessionData(String customerId, String[] accountNumbers) {
        this.customerId = customerId;
        this.accountNumbers = accountNumbers;
        fromAccountSelector.removeAllItems();
        if (accountNumbers != null) {
            for (String acc : accountNumbers) fromAccountSelector.addItem(acc);
        }
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
