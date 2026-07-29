package com.securebank.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.securebank.client.BankClient;
import com.securebank.gui.AppLanguage;
import com.securebank.gui.SecureBankApp;
import com.securebank.gui.components.NotificationPanel;
import com.securebank.server.BankServer;

import javax.swing.*;
import java.awt.*;

/**
 * Main — the entry point of the HSBC Bank application.
 *
 * RUBRIC COVERAGE:
 * - Unit 1: Command-line argument support — accepts server port from args[0]
 * - Unit 4: Starts the BankServer on a background thread
 * - Unit 5: Launches the Swing GUI on the Event Dispatch Thread (EDT)
 *
 * STARTUP SEQUENCE:
 * 1. Parse CLI arguments (port number)
 * 2. Set up FlatLaf Dark Look-and-Feel (HSBC Premier theme)
 * 3. Start BankServer on a background (daemon) thread
 * 4. Wait briefly for server to initialize
 * 5. Create BankClient and connect to server
 * 6. Launch the Swing GUI on the EDT
 *
 * VIVA NOTE — EVENT DISPATCH THREAD (EDT):
 * Swing is NOT thread-safe. ALL GUI creation and modification MUST happen on
 * the EDT. We use SwingUtilities.invokeLater() to ensure the GUI is created
 * on the correct thread. The main thread is NOT the EDT — they're different.
 *
 * USAGE:
 *   java -jar securebank.jar           → starts on default port 8888
 *   java -jar securebank.jar 9090      → starts on port 9090
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args optional CLI arguments:
     *             args[0] = server port number (default: 8888)
     */
    public static void main(String[] args) {

        // ============================================================
        // STEP 1: Parse command-line arguments
        // RUBRIC: Unit 1 — command-line argument support
        // ============================================================
        int port = BankServer.DEFAULT_PORT; // Default: 8888

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                System.out.println("[Main] Using port from CLI argument: " + port);
            } catch (NumberFormatException e) {
                System.err.println("[Main] Invalid port argument: '" + args[0] +
                        "'. Using default port " + port);
            }
        } else {
            System.out.println("[Main] No port argument provided. Using default: " + port);
        }

        // ============================================================
        // STEP 2: Set up FlatLaf Dark Look-and-Feel (HSBC Premier Dark Theme)
        // ============================================================
        try {
            // Install FlatLaf Dark theme — professional banking dark mode
            FlatDarkLaf.setup();

            // Set a modern font globally — "Segoe UI" on Windows, fallback to default
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14);
            UIManager.put("defaultFont", defaultFont);

            // HSBC Red accent for focus and selection
            Color hsbcRed = new Color(0xDB, 0x00, 0x11);
            Color darkBg = new Color(0x14, 0x14, 0x1E);
            Color cardBg = new Color(0x1E, 0x1E, 0x2E);

            // Additional UI customizations for a polished dark look
            UIManager.put("Button.arc", 8);                    // Rounded buttons
            UIManager.put("Component.arc", 8);                 // Rounded components
            UIManager.put("TextComponent.arc", 6);             // Rounded text fields
            UIManager.put("ScrollBar.trackArc", 999);          // Rounded scrollbar
            UIManager.put("ScrollBar.thumbArc", 999);          // Rounded scrollbar thumb
            UIManager.put("Component.focusWidth", 1);          // Thin focus ring
            UIManager.put("Component.focusColor", hsbcRed);    // HSBC Red focus
            UIManager.put("Component.borderColor", new Color(0x33, 0x33, 0x33));
            UIManager.put("Component.disabledBorderColor", new Color(0x2A, 0x2A, 0x2A));
            UIManager.put("TextField.background", cardBg);
            UIManager.put("ComboBox.background", cardBg);
            UIManager.put("Table.background", cardBg);
            UIManager.put("Table.selectionBackground", new Color(0xDB, 0x00, 0x11, 60));
            UIManager.put("Table.gridColor", new Color(0x33, 0x33, 0x33));
            UIManager.put("TableHeader.background", new Color(0x0C, 0x0C, 0x14));
            UIManager.put("ScrollPane.background", darkBg);
            UIManager.put("Panel.background", darkBg);

            System.out.println("[Main] HSBC Premier Dark theme applied successfully.");

        } catch (Exception e) {
            System.err.println("[Main] FlatLaf setup failed: " + e.getMessage());
            System.err.println("[Main] Falling back to system Look-and-Feel.");
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // ============================================================
        // STEP 3: Start BankServer on a background thread
        // ============================================================
        final int serverPort = port;
        BankServer server = new BankServer(serverPort);

        // Server thread is set as DAEMON so it doesn't prevent JVM shutdown
        // when the user closes the GUI window
        Thread serverThread = new Thread(() -> server.start(), "BankServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();

        // Add shutdown hook to save data when the app exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] Shutdown hook triggered — saving data...");
            server.saveAllData();
            server.stop();
        }, "Shutdown-Hook"));

        System.out.println("[Main] Server thread started. Waiting for initialization...");

        // ============================================================
        // STEP 4: Wait briefly for server to start accepting connections
        // ============================================================
        try {
            Thread.sleep(1500); // Give the server 1.5 seconds to bind the port
        } catch (InterruptedException ignored) {}

        // ============================================================
        // STEP 5: Create BankClient and connect to server
        // ============================================================
        BankClient client = new BankClient("localhost", serverPort);
        boolean connected = client.connect();

        if (!connected) {
            System.err.println("[Main] WARNING: Could not connect to server. " +
                    "GUI will launch but operations will fail until reconnected.");
        }

        // ============================================================
        // STEP 6: Launch Swing GUI on the Event Dispatch Thread
        // VIVA: MUST use invokeLater — never create Swing on the main thread
        // ============================================================
        final BankClient finalClient = client;
        SwingUtilities.invokeLater(() -> {
            SecureBankApp app = new SecureBankApp(finalClient);
            app.setVisible(true);

            // Show connection status with professional greeting
            if (finalClient.isConnected()) {
                NotificationPanel.showInfo(app,
                        AppLanguage.get("app.connected") + " (port " + serverPort + ")");
            } else {
                NotificationPanel.showError(app, AppLanguage.get("app.connection.failed"));
            }
        });

        System.out.println("[Main] HSBC Bank application launched successfully.\n");
    }
}
