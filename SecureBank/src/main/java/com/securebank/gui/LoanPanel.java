package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * LoanPanel — loan application and status view.
 */
public class LoanPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String[] accountNumbers;

    private JComboBox<String> accountSelector;
    private StyledTextField amountField;
    private JComboBox<String> tenureSelector;
    private StyledTextField purposeField;
    private StyledButton applyButton;
    private JPanel loanStatusPanel;



    public LoanPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));
        buildPanel();
    }

    public void buildPanel() {
        removeAll();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // ---- Header ----
        JLabel header = new JLabel(AppLanguage.get("loan.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(header);
        mainPanel.add(Box.createVerticalStrut(20));

        // Two-column layout: Application form + Status
        JPanel columnsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        columnsPanel.setOpaque(false);
        columnsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ---- Left: Application Form ----
        CardPanel formCard = new CardPanel(AppLanguage.get("loan.apply"), StyledButton.ACCENT_TEAL);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        formCard.add(Box.createVerticalStrut(20));

        addLabel(formCard, AppLanguage.get("loan.account"));
        accountSelector = new JComboBox<>();
        accountSelector.setFont(ThemeManager.getFont(14));
        accountSelector.setMaximumSize(new Dimension(350, 40));
        accountSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(accountSelector);
        formCard.add(Box.createVerticalStrut(12));

        addLabel(formCard, AppLanguage.get("loan.amount"));
        amountField = new StyledTextField("5,000 — 5,00,000");
        amountField.setMaximumSize(new Dimension(350, 42));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(amountField);
        formCard.add(Box.createVerticalStrut(12));

        addLabel(formCard, AppLanguage.get("loan.tenure"));
        tenureSelector = new JComboBox<>(new String[]{"6", "12", "24", "36", "48", "60"});
        tenureSelector.setFont(ThemeManager.getFont(14));
        tenureSelector.setSelectedItem("12");
        tenureSelector.setMaximumSize(new Dimension(350, 40));
        tenureSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(tenureSelector);
        formCard.add(Box.createVerticalStrut(12));

        addLabel(formCard, AppLanguage.get("loan.purpose"));
        purposeField = new StyledTextField("e.g., Education, Home Improvement");
        purposeField.setMaximumSize(new Dimension(350, 42));
        purposeField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(purposeField);
        formCard.add(Box.createVerticalStrut(20));

        applyButton = new StyledButton(AppLanguage.get("loan.apply.button"), StyledButton.ACCENT_TEAL);
        applyButton.setMaximumSize(new Dimension(350, 44));
        applyButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyButton.addActionListener(e -> handleApply());
        formCard.add(applyButton);

        columnsPanel.add(formCard);

        // ---- Right: Loan Status ----
        CardPanel statusCard = new CardPanel(AppLanguage.get("loan.your.loans"));
        statusCard.setLayout(new BorderLayout());

        loanStatusPanel = new JPanel();
        loanStatusPanel.setLayout(new BoxLayout(loanStatusPanel, BoxLayout.Y_AXIS));
        loanStatusPanel.setOpaque(false);

        JLabel placeholder = new JLabel(AppLanguage.get("loan.loading"));
        placeholder.setFont(ThemeManager.getItalicFont(13));
        placeholder.setForeground(ThemeManager.getTextMutedColor());
        loanStatusPanel.add(placeholder);

        JScrollPane scrollPane = new JScrollPane(loanStatusPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        statusCard.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        StyledButton refreshBtn = new StyledButton(AppLanguage.get("loan.refresh"), StyledButton.PRIMARY);
        refreshBtn.setPreferredSize(new Dimension(120, 35));
        refreshBtn.addActionListener(e -> loadLoanStatus());

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.setOpaque(false);
        refreshPanel.add(refreshBtn);
        statusCard.add(refreshPanel, BorderLayout.SOUTH);

        columnsPanel.add(statusCard);

        mainPanel.add(columnsPanel);

        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addLabel(JPanel parent, String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeManager.getFont(13));
        label.setForeground(ThemeManager.getTextMutedColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(4));
    }

    private void handleApply() {
        if (accountSelector.getSelectedItem() == null) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("loan.select.account"));
            return;
        }

        String account = accountSelector.getSelectedItem().toString().split(" ")[0];
        String purpose = purposeField.getActualText().trim();
        double amount;
        int tenure;

        try {
            amount = Double.parseDouble(amountField.getActualText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            NotificationPanel.showWarning(parentFrame, AppLanguage.get("loan.invalid.amount"));
            return;
        }

        try {
            tenure = Integer.parseInt(tenureSelector.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            tenure = 12;
        }

        if (purpose.isEmpty()) purpose = "Personal";

        applyButton.setLoading(true);
        final String finalPurpose = purpose;
        final int finalTenure = tenure;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.applyForLoan(customerId, account, amount, finalTenure, finalPurpose);
            }

            @Override
            protected void done() {
                applyButton.setLoading(false);
                try {
                    String response = get();
                    if (response != null && response.startsWith("OK|")) {
                        String[] parts = response.substring(3).split("\\|");
                        String loanId = parts.length > 0 ? parts[0] : "";
                        String emi = parts.length > 1 ? parts[1] : "";
                        NotificationPanel.showSuccess(parentFrame,
                                "Loan approved! ID: " + loanId + ", EMI: ₹" + emi);
                        amountField.clearField();
                        purposeField.clearField();
                        loadLoanStatus();
                    } else {
                        String error = (response != null && response.startsWith("ERROR|")) ?
                                response.substring(6) : "Loan application failed";
                        NotificationPanel.showError(parentFrame, error);
                    }
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, AppLanguage.get("common.error") + ": " + e.getMessage());
                }
            }
        }.execute();
    }

    public void loadLoanStatus() {
        if (client == null || !client.isConnected() || customerId == null) return;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.getLoanStatus(customerId);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    loanStatusPanel.removeAll();

                    if (response == null || response.equals("OK|EMPTY") ||
                            !response.startsWith("OK|")) {
                        JLabel noLoans = new JLabel(AppLanguage.get("loan.no.loans"));
                        noLoans.setFont(ThemeManager.getItalicFont(13));
                        noLoans.setForeground(ThemeManager.getTextMutedColor());
                        loanStatusPanel.add(noLoans);
                    } else {
                        String[] loans = response.substring(3).split(";");
                        for (String loanStr : loans) {
                            String[] parts = loanStr.split("\\|", -1);
                            if (parts.length >= 10) {
                                loanStatusPanel.add(createLoanRow(parts));
                                loanStatusPanel.add(Box.createVerticalStrut(8));
                            }
                        }
                    }

                    loanStatusPanel.revalidate();
                    loanStatusPanel.repaint();
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, "Error loading loans");
                }
            }
        }.execute();
    }

    private JPanel createLoanRow(String[] parts) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorderColor()),
                new EmptyBorder(6, 0, 6, 0)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel idLabel = new JLabel(parts[0] + " — ₹" + parts[3]);
        idLabel.setFont(ThemeManager.getBoldFont(13));
        idLabel.setForeground(ThemeManager.getTextLightColor());

        String statusText = parts.length > 9 ? parts[9] : "Unknown";
        Color statusColor = switch (statusText) {
            case "ACTIVE" -> ThemeManager.getSuccessColor();
            case "APPROVED" -> ThemeManager.getPrimaryAccentColor();
            case "REJECTED" -> ThemeManager.getDangerColor();
            case "CLOSED" -> Color.GRAY;
            default -> ThemeManager.getWarningColor();
        };

        JLabel statusLabel = new JLabel("Status: " + statusText +
                " | EMI: ₹" + (parts.length > 6 ? parts[6] : "N/A"));
        statusLabel.setFont(ThemeManager.getFont(12));
        statusLabel.setForeground(statusColor);

        row.add(idLabel);
        row.add(statusLabel);
        return row;
    }

    public void setSessionData(String customerId, String[] accountNumbers) {
        this.customerId = customerId;
        this.accountNumbers = accountNumbers;
        accountSelector.removeAllItems();
        if (accountNumbers != null) {
            for (String acc : accountNumbers) accountSelector.addItem(acc);
        }
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
