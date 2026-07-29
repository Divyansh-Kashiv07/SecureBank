package com.securebank.gui;

import com.securebank.client.BankClient;
import com.securebank.gui.components.SidebarPanel;
import com.securebank.gui.components.NotificationPanel;

import javax.swing.*;
import java.awt.*;

/**
 * SecureBankApp — the main JFrame shell of the application.
 *
 * RUBRIC: Unit 5 — Java Swing GUI.
 *
 * Layout structure:
 * ┌─────────┬────────────────────────────────────┐
 * │ Sidebar │ Content Area (CardLayout)           │
 * │  (220px)│  - LoginPanel                       │
 * │         │  - DashboardPanel                   │
 * │  Nav    │  - DepositWithdrawPanel              │
 * │  Items  │  - TransferPanel                     │
 * │         │  - LoanPanel                         │
 * │         │  - TransactionHistoryPanel            │
 * │         │  - ReportsPanel                       │
 * │         │  - AccountsPanel                      │
 * │         │  - SettingsPanel                      │
 * └─────────┴────────────────────────────────────┘
 *
 * CardLayout is used to switch between panels — only one is visible at a time.
 * The sidebar is hidden during login and shown after successful authentication.
 */
public class SecureBankApp extends JFrame {

    /** CardLayout for switching between content panels */
    private final CardLayout cardLayout;

    /** The content panel that holds all screens */
    private final JPanel contentPanel;

    /** The sidebar navigation panel */
    private final SidebarPanel sidebar;

    /** TCP client for server communication */
    private BankClient client;

    /** Current session data */
    private String currentCustomerId;
    private String currentCustomerName;
    private String[] currentAccountNumbers;

    /** All screen panels */
    private LoginPanel loginPanel;
    private DashboardPanel dashboardPanel;
    private DepositWithdrawPanel depositWithdrawPanel;
    private TransferPanel transferPanel;
    private LoanPanel loanPanel;
    private TransactionHistoryPanel historyPanel;
    private ReportsPanel reportsPanel;
    private AccountsPanel accountsPanel;
    private SettingsPanel settingsPanel;

    /**
     * Creates the main application window.
     *
     * @param client the BankClient connected to the server
     */
    public SecureBankApp(BankClient client) {
        this.client = client;

        // Window setup
        setTitle(AppLanguage.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(null); // Center on screen

        // Main layout
        setLayout(new BorderLayout());

        // Create sidebar
        sidebar = new SidebarPanel();
        sidebar.setVisible(false); // Hidden until login
        sidebar.setNavigationListener(this::handleNavigation);

        // Create content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(ThemeManager.getBackgroundColor()); // Dark background

        // Initialize all panels
        initializePanels();

        // Add panels to CardLayout
        contentPanel.add(loginPanel, "Login");
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(accountsPanel, "Accounts");
        contentPanel.add(depositWithdrawPanel, "DepositWithdraw");
        contentPanel.add(transferPanel, "Transfer");
        contentPanel.add(loanPanel, "Loans");
        contentPanel.add(historyPanel, "History");
        contentPanel.add(reportsPanel, "Reports");
        contentPanel.add(settingsPanel, "Settings");

        // Assemble the frame
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Show login screen first
        cardLayout.show(contentPanel, "Login");

        // Handle window closing — disconnect from server
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (client != null) {
                    client.disconnect();
                }
            }
        });
    }

    /**
     * Initializes all screen panels and wires up the BankClient.
     */
    private void initializePanels() {
        // Login
        loginPanel = new LoginPanel(this);
        loginPanel.setClient(client);
        loginPanel.setLoginListener(this::handleLoginSuccess);

        // Dashboard
        dashboardPanel = new DashboardPanel(this);
        dashboardPanel.setClient(client);
        dashboardPanel.setActionListener(this::handleNavigation);

        // Accounts
        accountsPanel = new AccountsPanel(this);
        accountsPanel.setClient(client);

        // Deposit/Withdraw
        depositWithdrawPanel = new DepositWithdrawPanel(this);
        depositWithdrawPanel.setClient(client);

        // Transfer
        transferPanel = new TransferPanel(this);
        transferPanel.setClient(client);

        // Loans
        loanPanel = new LoanPanel(this);
        loanPanel.setClient(client);

        // Transaction History
        historyPanel = new TransactionHistoryPanel(this);
        historyPanel.setClient(client);

        // Reports
        reportsPanel = new ReportsPanel(this);
        reportsPanel.setClient(client);

        // Settings
        settingsPanel = new SettingsPanel(this);
    }

    /**
     * Called when login succeeds.
     * Shows the sidebar and navigates to the Dashboard.
     */
    private void handleLoginSuccess(String customerId, String customerName,
                                     String[] accountNumbers) {
        this.currentCustomerId = customerId;
        this.currentCustomerName = customerName;
        this.currentAccountNumbers = accountNumbers;

        // Pass session data to all panels
        dashboardPanel.setSessionData(customerId, customerName, accountNumbers);
        depositWithdrawPanel.setSessionData(customerId, accountNumbers);
        transferPanel.setSessionData(customerId, accountNumbers);
        loanPanel.setSessionData(customerId, accountNumbers);
        historyPanel.setSessionData(accountNumbers);
        reportsPanel.setSessionData(customerId, accountNumbers);
        accountsPanel.setSessionData(customerId, accountNumbers);

        // Show sidebar and navigate to dashboard
        sidebar.setVisible(true);
        sidebar.selectItem(0); // Highlight Dashboard
        cardLayout.show(contentPanel, "Dashboard");

        // Load dashboard data
        dashboardPanel.refreshData();
    }

    /**
     * Handles sidebar navigation clicks.
     *
     * @param screenName the screen identifier to navigate to
     */
    private void handleNavigation(String screenName) {
        if ("Logout".equals(screenName)) {
            handleLogout();
            return;
        }

        cardLayout.show(contentPanel, screenName);

        // Refresh data when navigating to certain screens
        switch (screenName) {
            case "Dashboard" -> dashboardPanel.refreshData();
            case "History" -> historyPanel.loadTransactions();
            case "Loans" -> loanPanel.loadLoanStatus();
            case "Reports" -> reportsPanel.loadReport();
            case "Accounts" -> accountsPanel.loadAccounts();
        }
    }

    /**
     * Handles logout — clears session and returns to login screen.
     */
    private void handleLogout() {
        currentCustomerId = null;
        currentCustomerName = null;
        currentAccountNumbers = null;

        sidebar.setVisible(false);
        loginPanel.reset();
        cardLayout.show(contentPanel, "Login");

        NotificationPanel.showInfo(this, AppLanguage.get("logout.done"));
    }

    /**
     * Updates the BankClient (e.g., after reconnection).
     */
    public void setClient(BankClient client) {
        this.client = client;
        loginPanel.setClient(client);
        dashboardPanel.setClient(client);
        depositWithdrawPanel.setClient(client);
        transferPanel.setClient(client);
        loanPanel.setClient(client);
        historyPanel.setClient(client);
        reportsPanel.setClient(client);
        accountsPanel.setClient(client);
    }

    /**
     * Refreshes all panels and the sidebar with the current language.
     * Called when the user switches language in SettingsPanel.
     *
     * This ensures the ENTIRE application interface updates:
     * - Window title
     * - Sidebar (navigation labels)
     * - All content panels (labels, buttons, placeholders)
     */
    public void refreshAllPanelsForLanguage() {
        // Update window title
        setTitle(AppLanguage.get("app.title"));
        contentPanel.setBackground(ThemeManager.getBackgroundColor());

        // Rebuild sidebar with fresh language strings
        sidebar.rebuild();
        // Re-wire the navigation listener (rebuild clears it internally but we keep the reference)
        sidebar.setNavigationListener(this::handleNavigation);

        // Rebuild every content panel
        loginPanel.rebuild();
        dashboardPanel.buildDashboard();
        depositWithdrawPanel.buildForm();
        transferPanel.buildForm();
        loanPanel.buildPanel();
        historyPanel.buildPanel();
        reportsPanel.buildPanel();
        accountsPanel.buildPanel();
        settingsPanel.buildPanel();

        // Re-select Settings tab in sidebar (index 7) since we're on Settings when changing language
        sidebar.selectItem(7);

        // Force full repaint
        revalidate();
        repaint();
    }
}

