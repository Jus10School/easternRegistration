package com.example;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Eastern Registration system.
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Main entry point of application.
     * @param args not used
     */
    public static void main(String[] args) {
        EasternSchedule easternSchedule = new EasternSchedule();
        SwingUtilities.invokeLater(
            () -> new MainMenu(easternSchedule).show()
        );
    }
}