package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardPanel — the main landing screen after login.
 *
 * Features:
 * - Balance card (primary account)
 * - Recent transactions card
 * - Quick action buttons (Deposit, Withdraw, Transfer)
 * - Transaction trend mini chart (Java2D bar chart)
 * - Customer greeting
 *
 * Layout: 2-column grid of cards.
 */
public class DashboardPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String customerName;
    private String[] accountNumbers;

    // Dashboard cards — stored as fields for data refresh
    private JLabel balanceLabel;
    private JLabel accountTypeLabel;
    private JLabel accountNumberLabel;
    private JPanel recentTxnPanel;
    private MiniChart trendChart;

    /** Callback for quick action button clicks (navigates to another screen) */
    public interface DashboardActionListener {
        void onNavigate(String screenName);
    }

    private DashboardActionListener actionListener;

    public DashboardPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));

        // Build initial empty dashboard
        buildDashboard();
    }

    /**
     * Builds the dashboard layout with cards.
     */
    public void buildDashboard() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        // ---- Header ----
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // Professional greeting based on time of day
        int hour = LocalDateTime.now().getHour();
        String timeGreeting;
        if (hour < 12) timeGreeting = AppLanguage.get("dashboard.greeting.morning");
        else if (hour < 17) timeGreeting = AppLanguage.get("dashboard.greeting.afternoon");
        else timeGreeting = AppLanguage.get("dashboard.greeting.evening");

        String greetingText = timeGreeting + (customerName != null ? ", " + customerName : "") + "!";
        JLabel greeting = new JLabel(greetingText);
        greeting.setFont(ThemeManager.getBoldFont(22));
        greeting.setForeground(ThemeManager.getTextLightColor());

        JLabel dateLabel = new JLabel(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        dateLabel.setFont(ThemeManager.getFont(13));
        dateLabel.setForeground(ThemeManager.getTextMutedColor());

        headerPanel.add(greeting, BorderLayout.WEST);
        headerPanel.add(dateLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // ---- Cards Grid ----
        JPanel cardsGrid = new JPanel(new GridLayout(2, 2, 18, 18));
        cardsGrid.setOpaque(false);

        // Card 1: Account Balance
        cardsGrid.add(createBalanceCard());

        // Card 2: Quick Actions
        cardsGrid.add(createQuickActionsCard());

        // Card 3: Recent Transactions
        cardsGrid.add(createRecentTransactionsCard());

        // Card 4: Transaction Trend Chart
        cardsGrid.add(createChartCard());

        add(cardsGrid, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Creates the account balance card.
     */
    private CardPanel createBalanceCard() {
        CardPanel card = new CardPanel(AppLanguage.get("dashboard.balance"), StyledButton.ACCENT_TEAL);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Spacer for title
        card.add(Box.createVerticalStrut(30));

        // Balance amount
        balanceLabel = new JLabel("₹0.00");
        balanceLabel.setFont(ThemeManager.getBoldFont(32));
        balanceLabel.setForeground(ThemeManager.getTextLightColor());
        balanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Account type
        accountTypeLabel = new JLabel(AppLanguage.get("dashboard.loading"));
        accountTypeLabel.setFont(ThemeManager.getFont(13));
        accountTypeLabel.setForeground(ThemeManager.getTextMutedColor());
        accountTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Account number
        accountNumberLabel = new JLabel("");
        accountNumberLabel.setFont(ThemeManager.getFont(12));
        accountNumberLabel.setForeground(ThemeManager.getTextMutedColor());
        accountNumberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(balanceLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(accountTypeLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(accountNumberLabel);

        return card;
    }

    /**
     * Creates the quick actions card with Deposit/Withdraw/Transfer buttons.
     */
    private CardPanel createQuickActionsCard() {
        CardPanel card = new CardPanel(AppLanguage.get("dashboard.quick.actions"), StyledButton.ACCENT_AMBER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(25, 0, 0, 0));

        StyledButton depositBtn = new StyledButton(AppLanguage.get("dashboard.deposit"), StyledButton.SUCCESS);
        depositBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("DepositWithdraw");
        });

        StyledButton withdrawBtn = new StyledButton(AppLanguage.get("dashboard.withdraw"), StyledButton.DANGER);
        withdrawBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("DepositWithdraw");
        });

        StyledButton transferBtn = new StyledButton(AppLanguage.get("dashboard.transfer"), StyledButton.ACCENT_TEAL);
        transferBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("Transfer");
        });

        StyledButton historyBtn = new StyledButton(AppLanguage.get("dashboard.history"), StyledButton.PRIMARY);
        historyBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("History");
        });

        buttonPanel.add(depositBtn);
        buttonPanel.add(withdrawBtn);
        buttonPanel.add(transferBtn);
        buttonPanel.add(historyBtn);

        card.add(buttonPanel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the recent transactions card.
     */
    private CardPanel createRecentTransactionsCard() {
        CardPanel card = new CardPanel(AppLanguage.get("dashboard.recent.txn"));

        recentTxnPanel = new JPanel();
        recentTxnPanel.setLayout(new BoxLayout(recentTxnPanel, BoxLayout.Y_AXIS));
        recentTxnPanel.setOpaque(false);
        recentTxnPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        // Placeholder
        JLabel placeholder = new JLabel(AppLanguage.get("dashboard.loading"));
        placeholder.setFont(ThemeManager.getItalicFont(13));
        placeholder.setForeground(ThemeManager.getTextMutedColor());
        recentTxnPanel.add(placeholder);

        JScrollPane scrollPane = new JScrollPane(recentTxnPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the chart card with a mini bar chart.
     */
    private CardPanel createChartCard() {
        CardPanel card = new CardPanel();

        trendChart = new MiniChart(AppLanguage.get("dashboard.trend"));
        trendChart.setData(
                new double[]{0, 0, 0, 0, 0, 0, 0},
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}
        );

        card.add(trendChart, BorderLayout.CENTER);
        return card;
    }

    // ==================== DATA LOADING ====================

    /**
     * Sets session data and refreshes the dashboard.
     */
    public void setSessionData(String customerId, String customerName, String[] accountNumbers) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.accountNumbers = accountNumbers;

        buildDashboard();
        refreshData();
    }

    /**
     * Refreshes all dashboard data from the server.
     */
    public void refreshData() {
        if (client == null || !client.isConnected()) return;
        if (accountNumbers == null || accountNumbers.length == 0) return;

        // Load data on a background thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String balanceStr = "₹0.00";
            String accType = "Account";
            String accNum = "";
            List<String[]> recentTxns = new ArrayList<>();
            double[] chartValues = new double[7];

            @Override
            protected Void doInBackground() {
                // Get first account info
                String primaryAcc = accountNumbers[0];
                accNum = primaryAcc;

                // Get account info
                String infoResp = client.getAccountInfo(primaryAcc);
                if (infoResp != null && infoResp.startsWith("OK|")) {
                    String[] parts = infoResp.substring(3).split("\\|");
                    if (parts.length >= 3) {
                        accType = parts[1] + (AppLanguage.isHindi() ? " खाता" : " Account");
                        balanceStr = "₹" + formatAmount(parts[2]);
                    }
                }

                // Get transaction history
                String histResp = client.getTransactionHistory(primaryAcc);
                if (histResp != null && histResp.startsWith("OK|") && !histResp.equals("OK|EMPTY")) {
                    String[] txns = histResp.substring(3).split(";");
                    // Show last 5 transactions
                    int start = Math.max(0, txns.length - 5);
                    for (int i = start; i < txns.length; i++) {
                        String[] txnParts = txns[i].split("\\|", -1);
                        if (txnParts.length >= 5) {
                            recentTxns.add(txnParts);
                        }
                    }

                    // Build chart data (sum amounts by day for last 7 entries)
                    String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                    int chartEntries = Math.min(7, txns.length);
                    for (int i = 0; i < chartEntries; i++) {
                        String[] txnParts = txns[txns.length - chartEntries + i].split("\\|", -1);
                        if (txnParts.length >= 4) {
                            try {
                                chartValues[i] = Double.parseDouble(txnParts[3]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                // Update UI on EDT
                if (balanceLabel != null) balanceLabel.setText(balanceStr);
                if (accountTypeLabel != null) accountTypeLabel.setText(accType);
                if (accountNumberLabel != null) accountNumberLabel.setText(accNum);

                // Update recent transactions
                if (recentTxnPanel != null) {
                    recentTxnPanel.removeAll();
                    if (recentTxns.isEmpty()) {
                        JLabel empty = new JLabel(AppLanguage.get("dashboard.no.txn"));
                        empty.setFont(ThemeManager.getItalicFont(13));
                        empty.setForeground(ThemeManager.getTextMutedColor());
                        recentTxnPanel.add(empty);
                    } else {
                        for (String[] txn : recentTxns) {
                            recentTxnPanel.add(createTransactionRow(txn));
                            recentTxnPanel.add(Box.createVerticalStrut(5));
                        }
                    }
                    recentTxnPanel.revalidate();
                    recentTxnPanel.repaint();
                }

                // Update chart
                if (trendChart != null) {
                    String[] labels = new String[7];
                    for (int i = 0; i < 7; i++) labels[i] = "T" + (i + 1);
                    trendChart.setData(chartValues, labels);
                }
            }
        };

        worker.execute();
    }

    /**
     * Creates a single transaction row for the recent transactions card.
     */
    private JPanel createTransactionRow(String[] txnParts) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Type and time
        String type = txnParts.length > 2 ? txnParts[2] : "Unknown";
        String time = txnParts.length > 5 ? txnParts[5] : "";

        // Shorten the timestamp
        if (time.length() > 10) time = time.substring(5, 16);

        JLabel typeLabel = new JLabel(type);
        typeLabel.setFont(ThemeManager.getFont(12));
        typeLabel.setForeground(ThemeManager.getTextMutedColor());

        // Amount
        String amountStr = txnParts.length > 3 ? txnParts[3] : "0.00";
        boolean isCredit = type.contains("DEPOSIT") || type.contains("TRANSFER_IN") ||
                type.contains("INTEREST") || type.contains("LOAN_DISBURSEMENT");

        JLabel amountLabel = new JLabel((isCredit ? "+" : "-") + "₹" + amountStr);
        amountLabel.setFont(ThemeManager.getBoldFont(12));
        amountLabel.setForeground(isCredit ? ThemeManager.getSuccessColor() : ThemeManager.getDangerColor());

        row.add(typeLabel, BorderLayout.WEST);
        row.add(amountLabel, BorderLayout.EAST);

        return row;
    }

    /**
     * Formats a numeric string with commas.
     */
    private String formatAmount(String amount) {
        try {
            double val = Double.parseDouble(amount);
            return String.format("%,.2f", val);
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    // ==================== SETTERS ====================

    public void setClient(BankClient client) {
        this.client = client;
    }

    public void setActionListener(DashboardActionListener listener) {
        this.actionListener = listener;
    }
}
