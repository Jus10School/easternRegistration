package com.example;

import javax.swing.SwingUtilities;

/**
 * Launches the Eastern Registration graphical application.
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Starts the GUI on Swing's event-dispatch thread.
     *
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        EasternSchedule easternSchedule = new EasternSchedule();
        SwingUtilities.invokeLater(
            () -> new MainMenu(easternSchedule).show()
        );
    }
}