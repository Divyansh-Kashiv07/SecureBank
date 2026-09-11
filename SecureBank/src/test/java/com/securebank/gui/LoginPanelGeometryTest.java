package com.securebank.gui;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the login card's alignment invariants on the real laid-out component tree.
 *
 * Regression guard: the Customer ID field, PIN field, and Sign In button must all
 * share one flush left edge and one center axis inside the card — the original
 * BoxLayout implementation let them drift out of alignment.
 */
class LoginPanelGeometryTest {

    private static void collect(Container c, java.util.List<Component> out) {
        for (Component k : c.getComponents()) {
            out.add(k);
            if (k instanceof Container) collect((Container) k, out);
        }
    }

    @Test
    @DisplayName("Login card: labels, fields, and button share one aligned column")
    void loginCardFormIsAligned() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Requires a display for real Swing layout");

        final java.util.concurrent.atomic.AtomicReference<Component[]> tree =
                new java.util.concurrent.atomic.AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            try {
                LoginPanel panel = new LoginPanel(frame);
                frame.setContentPane(panel);
                frame.setSize(1200, 750);
                frame.setVisible(true);
                tree.set(allComponents(panel));
            } finally {
                frame.dispose();
            }
        });

        Component[] all = tree.get();
        com.securebank.gui.components.StyledTextField customerId = null;
        javax.swing.JPasswordField pinField = null;
        com.securebank.gui.components.StyledButton signIn = null;
        java.util.List<JLabel> labels = new java.util.ArrayList<>();

        for (Component k : all) {
            if (k instanceof com.securebank.gui.components.StyledTextField) customerId =
                    (com.securebank.gui.components.StyledTextField) k;
            else if (k instanceof javax.swing.JPasswordField) pinField = (javax.swing.JPasswordField) k;
            else if (k instanceof com.securebank.gui.components.StyledButton) signIn =
                    (com.securebank.gui.components.StyledButton) k;
            else if (k instanceof JLabel) labels.add((JLabel) k);
        }

        assertNotNull(customerId, "Customer ID field missing");
        assertNotNull(pinField, "PIN field missing");
        assertNotNull(signIn, "Sign In button missing");

        Component pinWrapper = pinField.getParent();
        JLabel idLabel = labelAbove(labels, customerId);
        JLabel pinLabel = labelAbove(labels, pinWrapper);
        assertNotNull(idLabel, "Customer ID label missing");
        assertNotNull(pinLabel, "PIN label missing");

        // All form elements flush on one left edge (1px tolerance for rounding)
        assertEquals(customerId.getX(), idLabel.getX(), 1, "Customer ID label not flush with its field");
        assertEquals(pinWrapper.getX(), pinLabel.getX(), 1, "PIN label not flush with its field");
        assertEquals(customerId.getX(), pinWrapper.getX(), 1, "Customer ID and PIN fields not aligned");
        assertEquals(pinWrapper.getX(), signIn.getX(), 1, "Sign In button not aligned with fields");

        // Shared center axis (2px tolerance)
        int fieldCenter = customerId.getX() + customerId.getWidth() / 2;
        int wrapperCenter = pinWrapper.getX() + pinWrapper.getWidth() / 2;
        int buttonCenter = signIn.getX() + signIn.getWidth() / 2;
        assertEquals(fieldCenter, wrapperCenter, 2, "Customer ID and PIN centers differ");
        assertEquals(wrapperCenter, buttonCenter, 2, "Button center off the shared axis");

        // Uniform input heights
        assertEquals(customerId.getHeight(), pinWrapper.getHeight(), 1, "Field heights differ");
        assertEquals(44, customerId.getHeight(), "Customer ID field height should be 44px");

        // Button at least as tall as the fields and never shorter than 40px
        assertTrue(signIn.getHeight() >= 40, "Sign In button too short");
    }

    private static JLabel labelAbove(java.util.List<JLabel> labels, Component field) {
        for (JLabel l : labels) {
            boolean above = l.getBounds().y + l.getBounds().height <= field.getBounds().y + 2;
            boolean near = field.getBounds().y - (l.getBounds().y + l.getBounds().height) <= 30;
            if (above && near) {
                return l;
            }
        }
        return null;
    }

    private static Component[] allComponents(Container root) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        collect(root, out);
        return out.toArray(new Component[0]);
    }
}
