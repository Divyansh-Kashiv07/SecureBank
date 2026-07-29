package com.securebank.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * AppLanguage — centralized internationalization (i18n) support.
 *
 * Provides English and Hindi translations for all user-facing strings.
 * Uses a HashMap<String, String> for fast key-based lookup.
 *
 * RUBRIC: Unit 2 — Collections framework (HashMap usage).
 *
 * Usage:
 *   AppLanguage.setLanguage("hi");  // Switch to Hindi
 *   String label = AppLanguage.get("sidebar.dashboard"); // "डैशबोर्ड"
 */
public class AppLanguage {

    /** Current language code: "en" or "hi" */
    private static String currentLanguage = "en";

    /** English strings */
    private static final Map<String, String> EN = new HashMap<>();

    /** Hindi strings */
    private static final Map<String, String> HI = new HashMap<>();

    // ==================== STATIC INITIALIZER ====================
    static {
        // ---- App-level ----
        EN.put("app.title", "HSBC Bank — Premier Banking System");
        HI.put("app.title", "HSBC बैंक — प्रीमियर बैंकिंग सिस्टम");

        EN.put("app.connected", "Connected to HSBC Bank server");
        HI.put("app.connected", "HSBC बैंक सर्वर से कनेक्ट किया गया");

        EN.put("app.connection.failed", "Server connection failed. Please try again.");
        HI.put("app.connection.failed", "सर्वर कनेक्शन विफल। कृपया पुनः प्रयास करें।");

        // ---- Login ----
        EN.put("login.title", "HSBC Bank");
        HI.put("login.title", "HSBC बैंक");

        EN.put("login.subtitle", "Welcome to Premier Banking. Sign in to continue.");
        HI.put("login.subtitle", "प्रीमियर बैंकिंग में आपका स्वागत है। जारी रखने के लिए साइन इन करें।");

        EN.put("login.customer.id", "Customer ID");
        HI.put("login.customer.id", "ग्राहक आईडी");

        EN.put("login.pin", "PIN");
        HI.put("login.pin", "पिन");

        EN.put("login.signin", "Sign In");
        HI.put("login.signin", "साइन इन करें");

        EN.put("login.demo.hint", "Demo: CUSTOMER-1 / PIN: 1234");
        HI.put("login.demo.hint", "डेमो: CUSTOMER-1 / पिन: 1234");

        EN.put("login.tagline", "Your trusted banking partner since 1865");
        HI.put("login.tagline", "1865 से आपका विश्वसनीय बैंकिंग साथी");

        EN.put("login.enter.id", "Please enter your Customer ID");
        HI.put("login.enter.id", "कृपया अपनी ग्राहक आईडी दर्ज करें");

        EN.put("login.enter.pin", "Please enter your PIN");
        HI.put("login.enter.pin", "कृपया अपना पिन दर्ज करें");

        EN.put("login.server.error", "Cannot connect to server. Is it running?");
        HI.put("login.server.error", "सर्वर से कनेक्ट नहीं हो सका। क्या सर्वर चल रहा है?");

        EN.put("login.welcome", "Welcome to HSBC Bank, {name}! Your accounts are ready.");
        HI.put("login.welcome", "HSBC बैंक में आपका स्वागत है, {name}! आपके खाते तैयार हैं।");

        // ---- Sidebar ----
        EN.put("sidebar.bank.name", "HSBC Bank");
        HI.put("sidebar.bank.name", "HSBC बैंक");

        EN.put("sidebar.tagline", "Premier Banking");
        HI.put("sidebar.tagline", "प्रीमियर बैंकिंग");

        EN.put("sidebar.dashboard", "Dashboard");
        HI.put("sidebar.dashboard", "डैशबोर्ड");

        EN.put("sidebar.accounts", "Accounts");
        HI.put("sidebar.accounts", "खाते");

        EN.put("sidebar.deposit.withdraw", "Deposit / Withdraw");
        HI.put("sidebar.deposit.withdraw", "जमा / निकासी");

        EN.put("sidebar.transfer", "Fund Transfer");
        HI.put("sidebar.transfer", "फंड ट्रांसफर");

        EN.put("sidebar.loans", "Loans");
        HI.put("sidebar.loans", "ऋण");

        EN.put("sidebar.transactions", "Transactions");
        HI.put("sidebar.transactions", "लेनदेन");

        EN.put("sidebar.reports", "Reports");
        HI.put("sidebar.reports", "रिपोर्ट");

        EN.put("sidebar.settings", "Settings");
        HI.put("sidebar.settings", "सेटिंग्स");

        EN.put("sidebar.logout", "Logout");
        HI.put("sidebar.logout", "लॉगआउट");

        // ---- Dashboard ----
        EN.put("dashboard.greeting.morning", "Good Morning");
        HI.put("dashboard.greeting.morning", "सुप्रभात");

        EN.put("dashboard.greeting.afternoon", "Good Afternoon");
        HI.put("dashboard.greeting.afternoon", "नमस्कार");

        EN.put("dashboard.greeting.evening", "Good Evening");
        HI.put("dashboard.greeting.evening", "शुभ संध्या");

        EN.put("dashboard.welcome.message", "Welcome to your HSBC Bank dashboard.");
        HI.put("dashboard.welcome.message", "आपके HSBC बैंक डैशबोर्ड में आपका स्वागत है।");

        EN.put("dashboard.balance", "Account Balance");
        HI.put("dashboard.balance", "खाता शेष");

        EN.put("dashboard.quick.actions", "Quick Actions");
        HI.put("dashboard.quick.actions", "त्वरित कार्य");

        EN.put("dashboard.recent.txn", "Recent Transactions");
        HI.put("dashboard.recent.txn", "हाल के लेनदेन");

        EN.put("dashboard.trend", "Transaction Trend (Last 7 Days)");
        HI.put("dashboard.trend", "लेनदेन रुझान (पिछले 7 दिन)");

        EN.put("dashboard.deposit", "💰 Deposit");
        HI.put("dashboard.deposit", "💰 जमा");

        EN.put("dashboard.withdraw", "💳 Withdraw");
        HI.put("dashboard.withdraw", "💳 निकासी");

        EN.put("dashboard.transfer", "⇄ Transfer");
        HI.put("dashboard.transfer", "⇄ ट्रांसफर");

        EN.put("dashboard.history", "📊 History");
        HI.put("dashboard.history", "📊 इतिहास");

        EN.put("dashboard.no.txn", "No transactions yet");
        HI.put("dashboard.no.txn", "अभी तक कोई लेनदेन नहीं");

        EN.put("dashboard.loading", "Loading...");
        HI.put("dashboard.loading", "लोड हो रहा है...");

        // ---- Deposit / Withdraw ----
        EN.put("dw.title", "Deposit / Withdraw");
        HI.put("dw.title", "जमा / निकासी");

        EN.put("dw.select.account", "Select Account");
        HI.put("dw.select.account", "खाता चुनें");

        EN.put("dw.amount", "Amount (₹)");
        HI.put("dw.amount", "राशि (₹)");

        EN.put("dw.remarks", "Remarks (optional)");
        HI.put("dw.remarks", "टिप्पणी (वैकल्पिक)");

        EN.put("dw.deposit", "💰 Deposit");
        HI.put("dw.deposit", "💰 जमा करें");

        EN.put("dw.withdraw", "💳 Withdraw");
        HI.put("dw.withdraw", "💳 निकासी करें");

        EN.put("dw.success", "Transaction successful! Thank you for banking with HSBC.");
        HI.put("dw.success", "लेनदेन सफल! HSBC बैंक के साथ बैंकिंग के लिए धन्यवाद।");

        EN.put("dw.select.account.warn", "Please select an account");
        HI.put("dw.select.account.warn", "कृपया एक खाता चुनें");

        EN.put("dw.invalid.amount", "Please enter a valid amount (numbers only, e.g. 1000)");
        HI.put("dw.invalid.amount", "कृपया एक मान्य राशि दर्ज करें (केवल संख्या, जैसे 1000)");

        EN.put("dw.positive.amount", "Amount must be greater than zero");
        HI.put("dw.positive.amount", "राशि शून्य से अधिक होनी चाहिए");

        EN.put("dw.balance", "Balance");
        HI.put("dw.balance", "शेष");

        EN.put("dw.enter.amount", "Enter amount");
        HI.put("dw.enter.amount", "राशि दर्ज करें");

        EN.put("dw.remarks.placeholder", "e.g., Salary credit");
        HI.put("dw.remarks.placeholder", "जैसे, वेतन जमा");

        // ---- Transfer ----
        EN.put("transfer.title", "Fund Transfer");
        HI.put("transfer.title", "फंड ट्रांसफर");

        EN.put("transfer.from", "From Account");
        HI.put("transfer.from", "खाते से");

        EN.put("transfer.to", "To Account Number");
        HI.put("transfer.to", "खाता संख्या में");

        EN.put("transfer.amount", "Amount (₹)");
        HI.put("transfer.amount", "राशि (₹)");

        EN.put("transfer.button", "⇄ Transfer Funds");
        HI.put("transfer.button", "⇄ फंड ट्रांसफर करें");

        EN.put("transfer.success", "Transfer successful! Thank you for banking with HSBC.");
        HI.put("transfer.success", "ट्रांसफर सफल! HSBC बैंक के साथ बैंकिंग के लिए धन्यवाद।");

        EN.put("transfer.enter.target", "Enter the target account number");
        HI.put("transfer.enter.target", "लक्ष्य खाता संख्या दर्ज करें");

        EN.put("transfer.select.source", "Select a source account");
        HI.put("transfer.select.source", "स्रोत खाता चुनें");

        EN.put("transfer.invalid.amount", "Please enter a valid amount (numbers only, e.g. 1000)");
        HI.put("transfer.invalid.amount", "कृपया एक मान्य राशि दर्ज करें (केवल संख्या, जैसे 1000)");

        EN.put("transfer.positive.amount", "Amount must be greater than zero");
        HI.put("transfer.positive.amount", "राशि शून्य से अधिक होनी चाहिए");

        // ---- Loans ----
        EN.put("loan.title", "Loan Management");
        HI.put("loan.title", "ऋण प्रबंधन");

        EN.put("loan.apply", "Apply for Loan");
        HI.put("loan.apply", "ऋण के लिए आवेदन करें");

        EN.put("loan.your.loans", "Your Loans");
        HI.put("loan.your.loans", "आपके ऋण");

        EN.put("loan.account", "Link to Account");
        HI.put("loan.account", "खाते से लिंक करें");

        EN.put("loan.amount", "Loan Amount (₹)");
        HI.put("loan.amount", "ऋण राशि (₹)");

        EN.put("loan.tenure", "Tenure (Months)");
        HI.put("loan.tenure", "अवधि (महीने)");

        EN.put("loan.purpose", "Purpose");
        HI.put("loan.purpose", "उद्देश्य");

        EN.put("loan.apply.button", "📋 Apply for Loan");
        HI.put("loan.apply.button", "📋 ऋण के लिए आवेदन करें");

        EN.put("loan.refresh", "🔄 Refresh");
        HI.put("loan.refresh", "🔄 रिफ्रेश");

        EN.put("loan.no.loans", "No loans found");
        HI.put("loan.no.loans", "कोई ऋण नहीं मिला");

        EN.put("loan.select.account", "Select an account");
        HI.put("loan.select.account", "एक खाता चुनें");

        EN.put("loan.invalid.amount", "Enter a valid loan amount");
        HI.put("loan.invalid.amount", "एक मान्य ऋण राशि दर्ज करें");

        EN.put("loan.loading", "Loading loan status...");
        HI.put("loan.loading", "ऋण स्थिति लोड हो रही है...");

        // ---- Transaction History ----
        EN.put("history.title", "📊 Transaction History");
        HI.put("history.title", "📊 लेनदेन इतिहास");

        EN.put("history.account", "Account:");
        HI.put("history.account", "खाता:");

        EN.put("history.type", "Type:");
        HI.put("history.type", "प्रकार:");

        EN.put("history.all.types", "All Types");
        HI.put("history.all.types", "सभी प्रकार");

        EN.put("history.filter", "🔍 Filter");
        HI.put("history.filter", "🔍 फ़िल्टर");

        EN.put("history.search", "Search by amount/remark");
        HI.put("history.search", "राशि/टिप्पणी से खोजें");

        EN.put("history.total", "  Total: {count} transactions");
        HI.put("history.total", "  कुल: {count} लेनदेन");

        EN.put("history.monthly", "Monthly Summary (Jagged Array Demo)");
        HI.put("history.monthly", "मासिक सारांश (जैग्ड एरे डेमो)");

        // ---- Reports ----
        EN.put("reports.title", "📈 Reports & Analytics");
        HI.put("reports.title", "📈 रिपोर्ट और विश्लेषण");

        EN.put("reports.balances", "Account Balances");
        HI.put("reports.balances", "खाता शेष");

        EN.put("reports.sorted", "Accounts by Balance (TreeMap Sorted)");
        HI.put("reports.sorted", "शेष द्वारा खाते (TreeMap क्रमबद्ध)");

        EN.put("reports.refresh", "🔄 Refresh Report");
        HI.put("reports.refresh", "🔄 रिपोर्ट रिफ्रेश");

        // ---- Accounts ----
        EN.put("accounts.title", "💰 Your Accounts");
        HI.put("accounts.title", "💰 आपके खाते");

        EN.put("accounts.loading", "Loading accounts...");
        HI.put("accounts.loading", "खाते लोड हो रहे हैं...");

        EN.put("accounts.none", "No accounts found");
        HI.put("accounts.none", "कोई खाता नहीं मिला");

        // ---- Settings ----
        EN.put("settings.title", "⚙ Settings");
        HI.put("settings.title", "⚙ सेटिंग्स");

        EN.put("settings.dark.mode", "Dark Mode");
        HI.put("settings.dark.mode", "डार्क मोड");

        EN.put("settings.font.size", "Font Size");
        HI.put("settings.font.size", "फ़ॉन्ट आकार");

        EN.put("settings.language", "Language");
        HI.put("settings.language", "भाषा");

        EN.put("settings.version", "HSBC Bank v1.0 — Core Java Capstone Project");
        HI.put("settings.version", "HSBC बैंक v1.0 — कोर जावा कैपस्टोन प्रोजेक्ट");

        EN.put("settings.lang.changed", "Language changed successfully!");
        HI.put("settings.lang.changed", "भाषा सफलतापूर्वक बदल दी गई!");

        // ---- Common ----
        EN.put("common.processing", "Processing...");
        HI.put("common.processing", "प्रोसेसिंग...");

        EN.put("common.server.error", "Server not responding");
        HI.put("common.server.error", "सर्वर जवाब नहीं दे रहा");

        EN.put("common.error", "Error");
        HI.put("common.error", "त्रुटि");

        EN.put("logout.message", "Thank you for banking with HSBC. See you soon!");
        HI.put("logout.message", "HSBC बैंक के साथ बैंकिंग के लिए धन्यवाद। जल्दी मिलेंगे!");

        EN.put("logout.done", "You have been logged out.");
        HI.put("logout.done", "आपने सफलतापूर्वक लॉगआउट कर लिया है।");
    }

    /**
     * Gets a translated string by key.
     *
     * @param key the translation key (e.g., "login.title")
     * @return the translated string, or the key itself if not found
     */
    public static String get(String key) {
        Map<String, String> map = "hi".equals(currentLanguage) ? HI : EN;
        return map.getOrDefault(key, EN.getOrDefault(key, key));
    }

    /**
     * Gets a translated string with a placeholder replaced.
     *
     * @param key         the translation key
     * @param placeholder the placeholder text (e.g., "{name}")
     * @param value       the replacement value
     * @return the translated string with placeholder replaced
     */
    public static String get(String key, String placeholder, String value) {
        return get(key).replace(placeholder, value);
    }

    /**
     * Sets the current language.
     *
     * @param langCode "en" for English, "hi" for Hindi
     */
    public static void setLanguage(String langCode) {
        currentLanguage = langCode;
    }

    /**
     * Gets the current language code.
     *
     * @return "en" or "hi"
     */
    public static String getLanguage() {
        return currentLanguage;
    }

    /**
     * Returns true if current language is Hindi.
     */
    public static boolean isHindi() {
        return "hi".equals(currentLanguage);
    }
}
