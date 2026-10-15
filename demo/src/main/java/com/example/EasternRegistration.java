package com.example;

import java.util.ArrayList;
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

        System.out.println("Current Term Code: " + easternSchedule.getTerm());
        System.out.println();

        ArrayList<Term> terms = easternSchedule.getAvailableTerms();
        if (!terms.isEmpty()) {
            Term term = terms.get(0);
            System.out.println("Term Code: " + term.code());
            System.out.println("Term Description: " + term.description());
            System.out.println();
        }

        ArrayList<Course> courses = easternSchedule.queryCourseBySubject("CSC");
        System.out.println();
        System.out.println("List of courses (max = 3)");
        for (int i = 0; i < Math.min(3, courses.size()); i++) {
            System.out.println(courses.get(i));
        }

        System.out.println();
        if (!courses.isEmpty()) {
            Course firstCourse = courses.get(0);
            System.out.println("Subject, number, and meetingFaculty for first course:");
            System.out.println("Subject: " + firstCourse.subject());
            System.out.println("Course Number: " + firstCourse.courseNumber());
            if (firstCourse.meetingsFaculty() != null
                && !firstCourse.meetingsFaculty().isEmpty()) {
                System.out.println(firstCourse.meetingsFaculty().get(0));
            }
        }

        new MainMenu(easternSchedule, new Scanner(System.in)).run();
    }
}