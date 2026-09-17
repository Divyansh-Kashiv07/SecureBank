package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;
import com.securebank.gui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * ReportsPanel — analytics and report views with polished styling.
 */
public class ReportsPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String[] accountNumbers;
    private MiniChart balanceChart;
    private JTable reportTable;
    private DefaultTableModel tableModel;

    public ReportsPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));
        buildPanel();
    }

    public void buildPanel() {
        removeAll();

        JLabel header = new JLabel(AppLanguage.get("reports.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 15, 0));
        content.setOpaque(false);

        // Left: Chart
        CardPanel chartCard = new CardPanel();
        chartCard.setLayout(new BorderLayout());
        balanceChart = new MiniChart(AppLanguage.get("reports.balances"));
        chartCard.add(balanceChart, BorderLayout.CENTER);
        content.add(chartCard);

        // Right: Sorted report table
        CardPanel tableCard = new CardPanel(AppLanguage.get("reports.sorted"));
        tableCard.setLayout(new BorderLayout());

        String[] columns = {"Account Number", "Type", "Balance (₹)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable = new JTable(tableModel);
        reportTable.setFont(ThemeManager.getFont(13));
        reportTable.setRowHeight(36);
        reportTable.setSelectionBackground(ThemeManager.getTableSelectionBackground());
        reportTable.setSelectionForeground(ThemeManager.getTableSelectionForeground());
        reportTable.setGridColor(ThemeManager.getTableGridColor());
        reportTable.setShowHorizontalLines(true);
        reportTable.setShowVerticalLines(false);
        reportTable.setIntercellSpacing(new Dimension(0, 1));

        // Custom header
        JTableHeader tableHeader = reportTable.getTableHeader();
        tableHeader.setFont(ThemeManager.getBoldFont(12));
        tableHeader.setBackground(ThemeManager.getTableHeaderBackground());
        tableHeader.setForeground(ThemeManager.getTableHeaderForeground());
        tableHeader.setPreferredSize(new Dimension(0, 40));
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0,
                ThemeManager.getPrimaryAccentColor()));

        // Alternating row colors
        reportTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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
                setForeground(ThemeManager.getTextLightColor());

                // Balance column (2) - color code
                if (column == 2) {
                    setFont(ThemeManager.getBoldFont(13));
                    setForeground(ThemeManager.getPrimaryAccentColor());
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ThemeManager.getCardColor());
        tableCard.add(scrollPane, BorderLayout.CENTER);

        StyledButton refreshBtn = new StyledButton(AppLanguage.get("reports.refresh"),
                ThemeManager.getPrimaryAccentColor());
        refreshBtn.setPreferredSize(new Dimension(150, 35));
        refreshBtn.addActionListener(e -> loadReport());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn);
        tableCard.add(btnPanel, BorderLayout.SOUTH);

        content.add(tableCard);
        add(content, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Loads report data and sorts using TreeMap.
     */
    public void loadReport() {
        if (client == null || !client.isConnected() || accountNumbers == null) return;

        new SwingWorker<Void, Void>() {
            TreeMap<Double, String[]> sortedByBalance = new TreeMap<>(Collections.reverseOrder());
            List<String> accNames = new ArrayList<>();
            List<Double> accBalances = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                for (String accNum : accountNumbers) {
                    String resp = client.getAccountInfo(accNum);
                    if (resp != null && resp.startsWith("OK|")) {
                        String[] parts = resp.substring(3).split("\\|");
                        if (parts.length >= 3) {
                            double balance = Double.parseDouble(parts[2]);
                            double key = balance + (Math.random() * 0.001);
                            sortedByBalance.put(key, new String[]{accNum, parts[1], parts[2]});
                            accNames.add(accNum.substring(accNum.length() - 4));
                            accBalances.add(balance);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                tableModel.setRowCount(0);

                for (Map.Entry<Double, String[]> entry : sortedByBalance.entrySet()) {
                    String[] data = entry.getValue();
                    tableModel.addRow(new Object[]{
                            data[0],
                            data[1] + (AppLanguage.isHindi() ? " खाता" : " Account"),
                            "₹" + String.format("%,.2f", Double.parseDouble(data[2]))
                    });
                }

                if (!accBalances.isEmpty()) {
                    double[] vals = accBalances.stream().mapToDouble(Double::doubleValue).toArray();
                    balanceChart.setData(vals, accNames.toArray(new String[0]));
                }
            }
        }.execute();
    }

    public void setSessionData(String customerId, String[] accountNumbers) {
        this.customerId = customerId;
        this.accountNumbers = accountNumbers;
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
