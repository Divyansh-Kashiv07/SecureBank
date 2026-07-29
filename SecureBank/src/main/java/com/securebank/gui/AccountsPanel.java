package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * AccountsPanel — displays account details and interest calculation.
 */
public class AccountsPanel extends JPanel {

    private final JFrame parentFrame;
    private BankClient client;
    private String customerId;
    private String[] accountNumbers;
    private JPanel accountCardsPanel;



    public AccountsPanel(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getBackgroundColor());
        setBorder(new EmptyBorder(20, 25, 20, 25));
        buildPanel();
    }

    public void buildPanel() {
        removeAll();
        setBackground(ThemeManager.getBackgroundColor());

        JLabel header = new JLabel(AppLanguage.get("accounts.title"));
        header.setFont(ThemeManager.getBoldFont(22));
        header.setForeground(ThemeManager.getTextLightColor());
        header.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(header, BorderLayout.NORTH);

        accountCardsPanel = new JPanel();
        accountCardsPanel.setLayout(new BoxLayout(accountCardsPanel, BoxLayout.Y_AXIS));
        accountCardsPanel.setOpaque(false);

        JLabel placeholder = new JLabel(AppLanguage.get("accounts.loading"));
        placeholder.setFont(ThemeManager.getItalicFont(14));
        placeholder.setForeground(ThemeManager.getTextMutedColor());
        accountCardsPanel.add(placeholder);

        JScrollPane scrollPane = new JScrollPane(accountCardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public void loadAccounts() {
        if (client == null || !client.isConnected() || accountNumbers == null) return;

        new SwingWorker<Void, Void>() {
            java.util.List<String[]> accountInfoList = new java.util.ArrayList<>();
            java.util.List<String> interestList = new java.util.ArrayList<>();

            @Override
            protected Void doInBackground() {
                for (String accNum : accountNumbers) {
                    String infoResp = client.getAccountInfo(accNum);
                    String interestResp = client.getInterest(accNum);

                    if (infoResp != null && infoResp.startsWith("OK|")) {
                        accountInfoList.add(infoResp.substring(3).split("\\|"));
                    } else {
                        accountInfoList.add(null);
                    }

                    if (interestResp != null && interestResp.startsWith("OK|")) {
                        interestList.add(interestResp.substring(3));
                    } else {
                        interestList.add("0.00");
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                accountCardsPanel.removeAll();

                for (int i = 0; i < accountNumbers.length; i++) {
                    String[] info = i < accountInfoList.size() ? accountInfoList.get(i) : null;
                    String interest = i < interestList.size() ? interestList.get(i) : "0.00";

                    if (info != null && info.length >= 3) {
                        accountCardsPanel.add(createAccountCard(
                                accountNumbers[i], info[0], info[1], info[2],
                                info.length > 3 ? info[3] : "0.0", interest));
                        accountCardsPanel.add(Box.createVerticalStrut(15));
                    }
                }

                if (accountCardsPanel.getComponentCount() == 0) {
                    JLabel empty = new JLabel(AppLanguage.get("accounts.none"));
                    empty.setFont(ThemeManager.getItalicFont(14));
                    empty.setForeground(ThemeManager.getTextMutedColor());
                    accountCardsPanel.add(empty);
                }

                accountCardsPanel.revalidate();
                accountCardsPanel.repaint();
            }
        }.execute();
    }

    private CardPanel createAccountCard(String accNum, String holderName, String type,
                                         String balance, String rate, String interest) {
        Color accent = type.equals("Savings") ?
                ThemeManager.getPrimaryAccentColor() : new Color(0xF4, 0xA2, 0x61);

        CardPanel card = new CardPanel(null, accent);
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Left: Account info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(holderName);
        nameLabel.setFont(ThemeManager.getBoldFont(16));
        nameLabel.setForeground(ThemeManager.getTextLightColor());

        JLabel accLabel = new JLabel(accNum + " • " + type + (AppLanguage.isHindi() ? " खाता" : " Account"));
        accLabel.setFont(ThemeManager.getFont(13));
        accLabel.setForeground(ThemeManager.getTextMutedColor());

        leftPanel.add(nameLabel);
        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(accLabel);

        // Right: Balance and interest
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        JLabel balLabel = new JLabel("₹" + String.format("%,.2f", Double.parseDouble(balance)));
        balLabel.setFont(ThemeManager.getBoldFont(22));
        balLabel.setForeground(ThemeManager.getTextLightColor());
        balLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        balLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel intLabel = new JLabel("Interest: ₹" + interest + " @ " + rate + "% p.a.");
        intLabel.setFont(ThemeManager.getFont(12));
        intLabel.setForeground(ThemeManager.getSuccessColor());
        intLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPanel.add(balLabel);
        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(intLabel);

        card.add(leftPanel, BorderLayout.WEST);
        card.add(rightPanel, BorderLayout.EAST);

        return card;
    }

    public void setSessionData(String customerId, String[] accountNumbers) {
        this.customerId = customerId;
        this.accountNumbers = accountNumbers;
    }

    public void setClient(BankClient client) {
        this.client = client;
    }
}
