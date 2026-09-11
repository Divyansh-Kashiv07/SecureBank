package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardPanel — the main landing screen after login.
 *
 * Features:
 * - Summary stat cards (Total Balance, Accounts, Transactions, Last Activity)
 * - Animated balance counter
 * - Loading skeleton states
 * - Professional typography hierarchy
 * - Responsive card grid
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

    // Summary stat cards
    private JLabel totalAccountsLabel;
    private JLabel totalTransactionsLabel;
    private JLabel lastActivityLabel;

    /** Whether data is currently loading */
    private boolean isLoading = false;

    /** Callback for quick action button clicks */
    public interface DashboardActionListener {
        void onNavigate(String screenName);
    }

    private DashboardActionListener actionListener;

    public DashboardPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));

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

        // ---- Summary Stats Row ----
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(0, 0, 16, 0));
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        statsRow.add(createStatCard(
                AppLanguage.get("dashboard.accounts"),
                "--",
                ThemeManager.getInfoColor(),
                BankIcon.IconType.ACCOUNTS
        ));
        statsRow.add(createStatCard(
                AppLanguage.get("sidebar.transactions"),
                "--",
                ThemeManager.getWarningColor(),
                BankIcon.IconType.TRANSACTIONS
        ));
        statsRow.add(createStatCard(
                AppLanguage.get("dashboard.last.activity"),
                "--",
                ThemeManager.getSuccessColor(),
                BankIcon.IconType.CHART_UP
        ));

        add(statsRow, BorderLayout.BEFORE_FIRST_LINE);

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
     * Creates a summary stat card.
     */
    private CardPanel createStatCard(String title, String value, Color accentColor,
                                      BankIcon.IconType iconType) {
        CardPanel card = new CardPanel(null, accentColor);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Icon
        BankIcon icon = BankIcon.create(iconType, accentColor, 20);
        icon.setPreferredSize(new Dimension(24, 24));
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconPanel.setOpaque(false);
        iconPanel.add(icon);

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeManager.getFont(11));
        titleLabel.setForeground(ThemeManager.getTextMutedColor());

        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(ThemeManager.getBoldFont(20));
        valueLabel.setForeground(ThemeManager.getTextLightColor());

        // Store reference for updates
        if (title.equals(AppLanguage.get("dashboard.accounts"))) {
            totalAccountsLabel = valueLabel;
        } else if (title.equals(AppLanguage.get("sidebar.transactions"))) {
            totalTransactionsLabel = valueLabel;
        } else if (title.equals(AppLanguage.get("dashboard.last.activity"))) {
            lastActivityLabel = valueLabel;
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(valueLabel);

        card.add(iconPanel, BorderLayout.NORTH);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Creates the account balance card.
     */
    private CardPanel createBalanceCard() {
        CardPanel card = new CardPanel(AppLanguage.get("dashboard.balance"),
                ThemeManager.getAccentGradientStart());
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(Box.createVerticalStrut(30));

        // Balance amount
        balanceLabel = new JLabel("--");
        balanceLabel.setFont(ThemeManager.getBoldFont(36));
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
        CardPanel card = new CardPanel(AppLanguage.get("dashboard.quick.actions"),
                ThemeManager.getWarningColor());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(25, 0, 0, 0));

        StyledButton depositBtn = new StyledButton(AppLanguage.get("dashboard.deposit"),
                StyledButton.SUCCESS);
        depositBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("DepositWithdraw");
        });

        StyledButton withdrawBtn = new StyledButton(AppLanguage.get("dashboard.withdraw"),
                StyledButton.DANGER);
        withdrawBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("DepositWithdraw");
        });

        StyledButton transferBtn = new StyledButton(AppLanguage.get("dashboard.transfer"),
                StyledButton.ACCENT_TEAL);
        transferBtn.addActionListener(e -> {
            if (actionListener != null) actionListener.onNavigate("Transfer");
        });

        StyledButton historyBtn = new StyledButton(AppLanguage.get("dashboard.history"),
                StyledButton.PRIMARY);
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

        // Loading skeleton
        for (int i = 0; i < 3; i++) {
            recentTxnPanel.add(createSkeletonRow());
            recentTxnPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(recentTxnPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates a loading skeleton row.
     */
    private JPanel createSkeletonRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Placeholder bars
        JLabel leftBar = new JLabel("░░░░░░░░░");
        leftBar.setFont(ThemeManager.getFont(12));
        leftBar.setForeground(ThemeManager.getBorderColor());

        JLabel rightBar = new JLabel("░░░░░░");
        rightBar.setFont(ThemeManager.getBoldFont(12));
        rightBar.setForeground(ThemeManager.getBorderColor());

        row.add(leftBar, BorderLayout.WEST);
        row.add(rightBar, BorderLayout.EAST);

        return row;
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

        isLoading = true;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String balanceStr = "--";
            String accType = "Account";
            String accNum = "";
            List<String[]> recentTxns = new ArrayList<>();
            double[] chartValues = new double[7];
            int totalTxnCount = 0;
            String lastActivity = "--";

            @Override
            protected Void doInBackground() {
                String primaryAcc = accountNumbers[0];
                accNum = primaryAcc;

                // Get account info
                String infoResp = client.getAccountInfo(primaryAcc);
                if (infoResp != null && infoResp.startsWith("OK|")) {
                    String[] parts = infoResp.substring(3).split("\\|");
                    if (parts.length >= 3) {
                        accType = parts[1] + (AppLanguage.isHindi() ? " खाता" : " Account");
                        double bal = Double.parseDouble(parts[2]);
                        balanceStr = "₹" + String.format("%,.2f", bal);
                    }
                }

                // Get transaction history
                String histResp = client.getTransactionHistory(primaryAcc);
                if (histResp != null && histResp.startsWith("OK|") && !histResp.equals("OK|EMPTY")) {
                    String[] txns = histResp.substring(3).split(";");
                    totalTxnCount = txns.length;

                    // Last 5 transactions
                    int start = Math.max(0, txns.length - 5);
                    for (int i = start; i < txns.length; i++) {
                        String[] txnParts = txns[i].split("\\|", -1);
                        if (txnParts.length >= 5) {
                            recentTxns.add(txnParts);
                        }
                    }

                    // Last activity
                    if (txns.length > 0) {
                        String[] lastTxn = txns[txns.length - 1].split("\\|", -1);
                        if (lastTxn.length > 5) {
                            lastActivity = lastTxn[5];
                            if (lastActivity.length() > 10) {
                                lastActivity = lastActivity.substring(5, 16);
                            }
                        }
                    }

                    // Chart data
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
                isLoading = false;

                // Update balance card
                if (balanceLabel != null) balanceLabel.setText(balanceStr);
                if (accountTypeLabel != null) accountTypeLabel.setText(accType);
                if (accountNumberLabel != null) accountNumberLabel.setText(accNum);

                // Update summary stats
                if (totalAccountsLabel != null)
                    totalAccountsLabel.setText(String.valueOf(accountNumbers.length));
                if (totalTransactionsLabel != null)
                    totalTransactionsLabel.setText(String.valueOf(totalTxnCount));
                if (lastActivityLabel != null)
                    lastActivityLabel.setText(lastActivity);

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

        String type = txnParts.length > 2 ? txnParts[2] : "Unknown";
        String time = txnParts.length > 5 ? txnParts[5] : "";

        if (time.length() > 10) time = time.substring(5, 16);

        JLabel typeLabel = new JLabel(type);
        typeLabel.setFont(ThemeManager.getFont(12));
        typeLabel.setForeground(ThemeManager.getTextMutedColor());

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

    // ==================== SETTERS ====================

    public void setClient(BankClient client) {
        this.client = client;
    }

    public void setActionListener(DashboardActionListener listener) {
        this.actionListener = listener;
    }
}
