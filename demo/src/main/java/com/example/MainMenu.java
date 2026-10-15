package com.example;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Interactive menu for looking up courses by subject and search criteria.
 */
public class MainMenu {

    private final EasternSchedule schedule;
    private final Scanner input;

    public MainMenu() {
        this(new EasternSchedule(), new Scanner(System.in));
    }

    MainMenu(EasternSchedule schedule, Scanner input) {
        this.schedule = schedule;
        this.input = input;
    }

    public static void main(String[] args) throws Exception {
        new MainMenu().run();
    }

    public void run() throws Exception {
        System.out.println("Eastern Registration Course Search");

        while (true) {
            System.out.println("\n1. Display all courses for a subject");
            System.out.println("2. Search a subject with additional criteria");
            System.out.print("Select an option (or Q to quit): ");
            if (!input.hasNextLine()) {
                return;
            }

            String choice = input.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) {
                System.out.println("Goodbye.");
                return;
            }
            if (!choice.equals("1") && !choice.equals("2")) {
                System.out.println("Please select 1, 2, or Q.");
            } else {
                search(choice.equals("2"));
            }
        }
    }

    private void search(boolean applyFilters) throws Exception {
        String subject;
        List<Course> courses;
        while (true) {
            System.out.print("Enter a subject code (or B to return to the menu): ");
            if (!input.hasNextLine()) {
                return;
            }
            subject = input.nextLine().trim().toUpperCase(Locale.ROOT);
            if (subject.equals("B")) {
                return;
            }
            if (subject.isEmpty()) {
                System.out.println("Please enter a subject code.");
                continue;
            }

            courses = schedule.queryCourseBySubject(subject);
            if (courses.isEmpty()) {
                System.out.println("No courses found for " + subject + ". Try again.");
                continue;
            }
            break;
        }

        if (applyFilters) {
            String courseNumber;
            while (true) {
                String enteredNumber = prompt(
                    "Course number (blank for all, e.g. 360): "
                ).trim();
                if (enteredNumber.isEmpty() || courses.stream()
                    .anyMatch(course ->
                        course.courseNumber().equalsIgnoreCase(enteredNumber))) {
                    courseNumber = enteredNumber;
                    break;
                }
                System.out.println(
                    "No courses found for " + subject + " " + enteredNumber
                    + ". Please enter another course number."
                );
            }

            courses = filterCourses(
                courses,
                courseNumber,
                promptForOpenOnly(),
                prompt("Instructor name contains (blank for any): ").trim(),
                promptForExcludedDay()
            );
        }

        if (courses.isEmpty()) {
            System.out.println("No courses found for " + subject + ".");
            return;
        }

        for (Course course : courses) {
            displayCourse(course);
        }
    }
    

    private boolean promptForOpenOnly() {
        while (true) {
            String answer = prompt("Only open courses? (y/n): ").trim();
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")
                || answer.isEmpty()) {
                return false;
            }
            System.out.println("Please enter y or n.");
        }
    }

    private String promptForExcludedDay() {
        while (true) {
            String day = prompt(
                "Exclude a day (Monday-Sunday, or blank for any): "
            ).trim();
            if (day.isEmpty() || dayCode(day) != null) {
                return day;
            }
            System.out.println("Please enter a valid day of the week.");
        }
    }

    private String prompt(String message) {
        System.out.print(message);
        return input.hasNextLine() ? input.nextLine() : "";
    }

    static List<Course> filterCourses(
        List<Course> courses,
        String courseNumber,
        boolean openOnly,
        String instructor,
        String excludedDay
    ) {
        String numberFilter = courseNumber.trim();
        String instructorFilter = instructor.toLowerCase(Locale.ROOT);
        String excludedDayCode = dayCode(excludedDay);

        return courses.stream()
            .filter(course -> numberFilter.isEmpty()
                || course.courseNumber().equalsIgnoreCase(numberFilter))
            .filter(course -> !openOnly || Boolean.TRUE.equals(course.openSection()))
            .filter(course -> instructorFilter.isEmpty()
                || hasInstructor(course, instructorFilter))
            .filter(course -> excludedDayCode == null
                || !meetsOnDay(course, excludedDayCode))
            .collect(Collectors.toList());
    }

    private static boolean hasInstructor(Course course, String instructorFilter) {
        if (containsInstructor(course.faculty(), instructorFilter)) {
            return true;
        }
        if (course.meetingsFaculty() == null) {
            return false;
        }
        return course.meetingsFaculty().stream()
            .filter(meeting -> meeting != null)
            .anyMatch(meeting -> containsInstructor(meeting.faculty(), instructorFilter));
    }

    private static boolean containsInstructor(
        List<Course.Faculty> faculty,
        String instructorFilter
    ) {
        return faculty != null && faculty.stream()
            .filter(person -> person != null && person.displayName() != null)
            .anyMatch(person -> person.displayName()
                .toLowerCase(Locale.ROOT)
                .contains(instructorFilter));
    }

    private static boolean meetsOnDay(Course course, String dayCode) {
        if (course.meetingsFaculty() == null) {
            return false;
        }
        return course.meetingsFaculty().stream()
            .filter(meeting -> meeting != null && meeting.meetingTime() != null)
            .map(Course.MeetingFaculty::meetingTime)
            .anyMatch(time -> switch (dayCode) {
                case "M" -> time.monday();
                case "T" -> time.tuesday();
                case "W" -> time.wednesday();
                case "R" -> time.thursday();
                case "F" -> time.friday();
                case "S" -> time.saturday();
                case "U" -> time.sunday();
                default -> false;
            });
    }

    private static String dayCode(String day) {
        if (day == null || day.isBlank()) {
            return null;
        }
        return switch (day.trim().toLowerCase(Locale.ROOT)) {
            case "m", "mon", "monday" -> "M";
            case "t", "tue", "tues", "tuesday" -> "T";
            case "w", "wed", "wednesday" -> "W";
            case "r", "thu", "thur", "thurs", "thursday" -> "R";
            case "f", "fri", "friday" -> "F";
            case "s", "sat", "saturday" -> "S";
            case "u", "sun", "sunday" -> "U";
            default -> null;
        };
    }

    private static void displayCourse(Course course) {
        String title = course.courseTitle() == null || course.courseTitle().isBlank()
            ? "Title TBA"
            : course.courseTitle().replace("&amp;", "&");
        String courseDetails = course + ", " + course.creditsDescription()
            + ", " + title;

        List<Course.MeetingFaculty> meetings = course.meetingsFaculty();
        if (meetings == null || meetings.isEmpty()) {
            System.out.println(courseDetails + ", Meeting time TBA");
            return;
        }

        boolean printedCourseDetails = false;
        for (Course.MeetingFaculty meeting : meetings) {
            Course.MeetingTime time = meeting == null ? null : meeting.meetingTime();
            String meetingDetails = time == null
                ? "Meeting time TBA"
                : meeting.toString();
            if (!printedCourseDetails) {
                System.out.println(courseDetails + ", " + meetingDetails);
                printedCourseDetails = true;
            } else {
                System.out.println("  " + meetingDetails);
            }
        }
    }
}
