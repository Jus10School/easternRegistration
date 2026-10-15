package com.example;

import java.util.Scanner;

/**
 * Entry point for the Eastern Registration system.
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Main entry point of application
     * @param args not used
     * @throws Exception if a course lookup fails
     */
    public static void main(String[] args) throws Exception {
        EasternSchedule easternSchedule = new EasternSchedule();
        new MainMenu(easternSchedule, new Scanner(System.in)).run();
    }
}