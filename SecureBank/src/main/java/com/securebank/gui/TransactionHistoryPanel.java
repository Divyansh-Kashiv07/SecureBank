package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;
import com.securebank.transactions.Transaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionHistoryPanel — searchable/filterable transaction history.
 *
 * Features:
 * - Polished table with alternating row colors
 * - Custom table header styling
 * - Color-coded amounts
 * - Smooth hover effects
 * - Professional filter bar
 */
public class TransactionHistoryPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String[] accountNumbers;

    private JComboBox<String> accountSelector;
    private StyledTextField searchField;
    private JComboBox<String> typeFilter;
    private StyledButton filterButton;
    private JTable transactionTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;

    /** All loaded transactions (unfiltered) */
    private List<String[]> allTransactions = new ArrayList<>();

    public TransactionHistoryPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));
        buildPanel();
    }

    public void buildPanel() {
        removeAll();

        // ---- Header ----
        JLabel header = new JLabel(AppLanguage.get("history.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());

        // ---- Filter bar ----
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.setOpaque(false);

        accountSelector = new JComboBox<>();
        accountSelector.setFont(ThemeManager.getFont(13));
        accountSelector.setPreferredSize(new Dimension(150, 35));
        accountSelector.addActionListener(e -> loadTransactions());

        searchField = new StyledTextField(AppLanguage.get("history.search"));
        searchField.setPreferredSize(new Dimension(200, 35));

        typeFilter = new JComboBox<>(new String[]{
                AppLanguage.get("history.all.types"), "DEPOSIT", "WITHDRAWAL", "TRANSFER_IN",
                "TRANSFER_OUT", "LOAN_DISBURSEMENT", "INTEREST"
        });
        typeFilter.setFont(ThemeManager.getFont(13));
        typeFilter.setPreferredSize(new Dimension(150, 35));

        filterButton = new StyledButton(AppLanguage.get("history.filter"),
                ThemeManager.getPrimaryAccentColor());
        filterButton.setPreferredSize(new Dimension(100, 35));
        filterButton.addActionListener(e -> applyFilter());

        JLabel accLabel = new JLabel(AppLanguage.get("history.account"));
        accLabel.setForeground(ThemeManager.getTextMutedColor());
        JLabel typeLabel = new JLabel(AppLanguage.get("history.type"));
        typeLabel.setForeground(ThemeManager.getTextMutedColor());

        filterBar.add(accLabel);
        filterBar.add(accountSelector);
        filterBar.add(typeLabel);
        filterBar.add(typeFilter);
        filterBar.add(searchField);
        filterBar.add(filterButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        topPanel.add(filterBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // ---- Transaction Table ----
        String[] columns = {"ID", "Date", "Type", "Amount (₹)", "Balance After (₹)", "Remarks"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setFont(ThemeManager.getFont(13));
        transactionTable.setRowHeight(36);
        transactionTable.setSelectionBackground(ThemeManager.getTableSelectionBackground());
        transactionTable.setSelectionForeground(ThemeManager.getTableSelectionForeground());
        transactionTable.setGridColor(ThemeManager.getTableGridColor());
        transactionTable.setShowHorizontalLines(true);
        transactionTable.setShowVerticalLines(false);
        transactionTable.setIntercellSpacing(new Dimension(0, 1));
        transactionTable.setFillsViewportHeight(true);

        // Custom table header
        JTableHeader header_ = transactionTable.getTableHeader();
        header_.setFont(ThemeManager.getBoldFont(12));
        header_.setBackground(ThemeManager.getTableHeaderBackground());
        header_.setForeground(ThemeManager.getTableHeaderForeground());
        header_.setPreferredSize(new Dimension(0, 40));
        header_.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0,
                ThemeManager.getPrimaryAccentColor()));

        // Alternating row colors
        transactionTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

                if (!isSelected) {
                    setBackground(row % 2 == 0 ?
                            ThemeManager.getTableRowEven() : ThemeManager.getTableRowOdd());
                }

                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

                // Color-code amounts (column 3)
                if (column == 3) {
                    String type = table.getValueAt(row, 2).toString();
                    if (type.contains("DEPOSIT") || type.contains("TRANSFER_IN") ||
                            type.contains("INTEREST") || type.contains("DISBURSEMENT")) {
                        setForeground(ThemeManager.getSuccessColor());
                    } else {
                        setForeground(ThemeManager.getDangerColor());
                    }
                    setFont(ThemeManager.getBoldFont(13));
                } else if (column == 4) {
                    setForeground(ThemeManager.getTextMutedColor());
                } else {
                    setForeground(ThemeManager.getTextLightColor());
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor()));
        scrollPane.getViewport().setBackground(ThemeManager.getCardColor());

        CardPanel tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Summary label
        summaryLabel = new JLabel(AppLanguage.get("history.total", "{count}", "0"));
        summaryLabel.setFont(ThemeManager.getFont(12));
        summaryLabel.setForeground(ThemeManager.getTextMutedColor());
        summaryLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        centerPanel.add(tableCard, BorderLayout.CENTER);
        centerPanel.add(summaryLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // ---- Monthly Summary (Jagged Array) ----
        add(createMonthlySummaryPanel(), BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    /**
     * Creates a monthly transaction summary grid using a JAGGED ARRAY.
     */
    private JPanel createMonthlySummaryPanel() {
        CardPanel summaryCard = new CardPanel(AppLanguage.get("history.monthly"));
        summaryCard.setLayout(new BorderLayout());
        summaryCard.setPreferredSize(new Dimension(0, 120));

        String[][] monthlySummary = new String[3][];
        monthlySummary[0] = new String[]{"Jan", "Deposits: 3", "Withdrawals: 2", "Transfers: 1"};
        monthlySummary[1] = new String[]{"Feb", "Deposits: 5", "Withdrawals: 3"};
        monthlySummary[2] = new String[]{"Mar", "Deposits: 2"};

        JPanel grid = new JPanel(new GridLayout(3, 1, 5, 5));
        grid.setOpaque(false);

        for (String[] row : monthlySummary) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
            rowPanel.setOpaque(false);
            for (String cell : row) {
                JLabel label = new JLabel(cell);
                label.setFont(ThemeManager.getFont(12));
                label.setForeground(ThemeManager.getTextMutedColor());
                if (cell.equals(row[0])) {
                    label.setFont(ThemeManager.getBoldFont(12));
                    label.setForeground(ThemeManager.getPrimaryAccentColor());
                }
                rowPanel.add(label);
            }
            grid.add(rowPanel);
        }

        summaryCard.add(grid, BorderLayout.CENTER);
        return summaryCard;
    }

    /**
     * Loads transactions from the server.
     */
    public void loadTransactions() {
        if (client == null || !client.isConnected()) return;
        if (accountSelector.getSelectedItem() == null) return;

        String account = accountSelector.getSelectedItem().toString().split(" ")[0];

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return client.getTransactionHistory(account);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    allTransactions.clear();
                    tableModel.setRowCount(0);

                    if (response != null && response.startsWith("OK|") &&
                            !response.equals("OK|EMPTY")) {
                        String[] txns = response.substring(3).split(";");
                        for (String txnStr : txns) {
                            String[] parts = txnStr.split("\\|", -1);
                            if (parts.length >= 5) {
                                allTransactions.add(parts);
                                tableModel.addRow(new Object[]{
                                        parts[0],
                                        parts.length > 5 ? parts[5] : "",
                                        parts[2],
                                        parts[3],
                                        parts[4],
                                        parts.length > 6 ? parts[6] : ""
                                });
                            }
                        }
                    }

                    summaryLabel.setText(AppLanguage.get("history.total", "{count}",
                            String.valueOf(allTransactions.size())));
                } catch (Exception e) {
                    NotificationPanel.showError(parentFrame, "Error loading transactions");
                }
            }
        }.execute();
    }

    /**
     * Applies search/type filters to the transaction list.
     */
    private void applyFilter() {
        String searchText = searchField.getActualText().trim().toLowerCase();
        String selectedType = typeFilter.getSelectedItem().toString();

        tableModel.setRowCount(0);

        allTransactions.stream()
                .filter(parts -> {
                    if (!selectedType.equals(AppLanguage.get("history.all.types")) &&
                            !parts[2].equalsIgnoreCase(selectedType)) {
                        return false;
                    }
                    if (!searchText.isEmpty()) {
                        boolean matchAmount = parts[3].contains(searchText);
                        boolean matchRemarks = parts.length > 6 &&
                                parts[6].toLowerCase().contains(searchText);
                        return matchAmount || matchRemarks;
                    }
                    return true;
                })
                .forEach(parts -> {
                    tableModel.addRow(new Object[]{
                            parts[0],
                            parts.length > 5 ? parts[5] : "",
                            parts[2],
                            parts[3],
                            parts[4],
                            parts.length > 6 ? parts[6] : ""
                    });
                });

        summaryLabel.setText("  Showing: " + tableModel.getRowCount() +
                " of " + allTransactions.size() + " transactions");
    }

    public void setSessionData(String[] accountNumbers) {
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
