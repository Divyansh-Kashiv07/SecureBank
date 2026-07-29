package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * DepositWithdrawPanel — form for depositing and withdrawing money.
 *
 * Features:
 * - Account selector dropdown
 * - Amount input with validation
 * - Optional remarks field (for deposit)
 * - Toggle between Deposit and Withdraw modes
 * - Loading state during server communication
 */
public class DepositWithdrawPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String[] accountNumbers;

    private JComboBox<String> accountSelector;
    private StyledTextField amountField;
    private StyledTextField remarksField;
    private StyledButton depositButton;
    private StyledButton withdrawButton;
    private JLabel currentBalanceLabel;



    public DepositWithdrawPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new GridBagLayout());
        setBackground(ThemeManager.getBackgroundColor());
        buildForm();
    }

    public void buildForm() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        CardPanel formCard = new CardPanel(AppLanguage.get("dw.title"));
        formCard.setPreferredSize(new Dimension(480, 500));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(20, 35, 20, 35));

        // Title spacer
        formCard.add(Box.createVerticalStrut(25));

        // Account selector
        JLabel accLabel = new JLabel(AppLanguage.get("dw.select.account"));
        accLabel.setFont(ThemeManager.getFont(13));
        accLabel.setForeground(ThemeManager.getTextMutedColor());
        accLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(accLabel);
        formCard.add(Box.createVerticalStrut(6));

        accountSelector = new JComboBox<>();
        accountSelector.setFont(ThemeManager.getFont(14));
        accountSelector.setMaximumSize(new Dimension(400, 40));
        accountSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountSelector.addActionListener(e -> loadBalance());
        formCard.add(accountSelector);
        formCard.add(Box.createVerticalStrut(8));

        // Current balance display
        currentBalanceLabel = new JLabel(AppLanguage.get("dw.balance") + ": --");
        currentBalanceLabel.setFont(ThemeManager.getBoldFont(14));
        currentBalanceLabel.setForeground(ThemeManager.getPrimaryAccentColor());
        currentBalanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(currentBalanceLabel);
        formCard.add(Box.createVerticalStrut(20));

        // Amount field
        JLabel amountLabel = new JLabel(AppLanguage.get("dw.amount"));
        amountLabel.setFont(ThemeManager.getFont(13));
        amountLabel.setForeground(ThemeManager.getTextMutedColor());
        amountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(amountLabel);
        formCard.add(Box.createVerticalStrut(6));

        amountField = new StyledTextField(AppLanguage.get("dw.enter.amount"));
        amountField.setMaximumSize(new Dimension(400, 42));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(amountField);
        formCard.add(Box.createVerticalStrut(15));

        // Remarks field (for deposit)
        JLabel remarksLabel = new JLabel(AppLanguage.get("dw.remarks"));
        remarksLabel.setFont(ThemeManager.getFont(13));
        remarksLabel.setForeground(ThemeManager.getTextMutedColor());
        remarksLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(remarksLabel);
        formCard.add(Box.createVerticalStrut(6));

        remarksField = new StyledTextField(AppLanguage.get("dw.remarks.placeholder"));
        remarksField.setMaximumSize(new Dimension(400, 42));
        remarksField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(remarksField);
        formCard.add(Box.createVerticalStrut(25));

        // Buttons row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(400, 50));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        depositButton = new StyledButton(AppLanguage.get("dw.deposit"), StyledButton.SUCCESS);
        depositButton.setPreferredSize(new Dimension(180, 44));
        depositButton.addActionListener(e -> handleDeposit());

        withdrawButton = new StyledButton(AppLanguage.get("dw.withdraw"), StyledButton.DANGER);
        withdrawButton.setPreferredSize(new Dimension(180, 44));
        withdrawButton.addActionListener(e -> handleWithdraw());

        buttonPanel.add(depositButton);
        buttonPanel.add(withdrawButton);
        formCard.add(buttonPanel);

        add(formCard);
        revalidate();
        repaint();
    }

    private void handleDeposit() {
        String account = getSelectedAccount();
        if (account == null) return;
        
        double amount = getAmount();
        if (amount <= 0) return;

        String remarks = remarksField.getActualText();
        depositButton.setLoading(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.deposit(account, amount, remarks);
            }

            @Override
            protected void done() {
                depositButton.setLoading(false);
                try {
                    String response = get();
                    handleResponse(response, AppLanguage.get("dashboard.deposit"));
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.error") + ": " + e.getMessage());
                }
            }
        }.execute();
    }

    private void handleWithdraw() {
        String account = getSelectedAccount();
        if (account == null) return;
        
        double amount = getAmount();
        if (amount <= 0) return;

        withdrawButton.setLoading(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.withdraw(account, amount);
            }

            @Override
            protected void done() {
                withdrawButton.setLoading(false);
                try {
                    String response = get();
                    handleResponse(response, AppLanguage.get("dashboard.withdraw"));
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.error") + ": " + e.getMessage());
                }
            }
        }.execute();
    }

    private void handleResponse(String response, String action) {
        if (response == null) {
            NotificationPanel.showError(parentFrame, AppLanguage.get("common.server.error"));
            return;
        }
        if (response.startsWith("OK|")) {
            String[] parts = response.substring(3).split("\\|");
            String newBalance = parts.length > 0 ? parts[0] : "N/A";
            NotificationPanel.showSuccess(parentFrame, AppLanguage.get("dw.success"));
            amountField.clearField();
            remarksField.clearField();
            loadBalance();
        } else {
            String error = response.startsWith("ERROR|") ? response.substring(6) : response;
            NotificationPanel.showError(parentFrame, error);
        }
    }

    private String getSelectedAccount() {
        if (accountSelector.getSelectedItem() == null) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("dw.select.account.warn"));
            return null;
        }
        return accountSelector.getSelectedItem().toString().split(" ")[0];
    }

    private double getAmount() {
        String text = amountField.getActualText().trim();
        if (text.isEmpty()) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("dw.invalid.amount"));
            return -1;
        }
        try {
            double amount = Double.parseDouble(text);
            if (amount <= 0) {
                NotificationPanel.showWarning(parentFrame, AppLanguage.get("dw.positive.amount"));
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("dw.invalid.amount"));
            return -1;
        }
    }

    private void loadBalance() {
        if (client == null || !client.isConnected()) return;
        String account = getSelectedAccount();
        if (account == null) return;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.getBalance(account);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("OK|")) {
                        currentBalanceLabel.setText(AppLanguage.get("dw.balance") + ": ₹" +
                                String.format("%,.2f", Double.parseDouble(resp.substring(3))));
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void setSessionData(String customerId, String[] accountNumbers) {
        this.customerId = customerId;
        this.accountNumbers = accountNumbers;
        accountSelector.removeAllItems();
        if (accountNumbers != null) {
            for (String acc : accountNumbers) {
                accountSelector.addItem(acc);
            }
        }
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
