package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interactive menu for looking up courses by subject and search criteria.
 */
public class MainMenu {

    private final EasternSchedule schedule;
    private final Scanner input;
    private Student student;

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
            System.out.println("1. Display all courses for a subject");
            System.out.println("2. Search a subject with additional criteria");
            System.out.println("3. Manage student registration");
            System.out.print("Select an option (or Q to quit): ");
            if (!input.hasNextLine()) {
                return;
            }

            String choice = input.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) {
                System.out.println("Goodbye.");
                return;
            }
            if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
                System.out.println("Please select 1, 2, 3, or Q.");
            } else if (choice.equals("3")) {
                manageStudent();
            } else {
                search(choice.equals("2"));
            }
        }
    }

    private void manageStudent() throws Exception {
        if (student == null) {
            String id = promptNonBlank("Student ID: ");
            if (id == null) {
                return;
            }
            String firstName = promptNonBlank("First name: ");
            if (firstName == null) {
                return;
            }
            String lastName = promptNonBlank("Last name: ");
            if (lastName == null) {
                return;
            }
            student = new Student(id, firstName, lastName);
            System.out.println(
                "Created student " + student.getFirstName() + " "
                    + student.getLastName() + " (ID: " + student.getId() + ")."
            );
        }

        while (true) {
            System.out.println("\nStudent: " + student.getFirstName() + " "
                + student.getLastName() + " (ID: " + student.getId() + ")");
            System.out.println("1. Add courses");
            System.out.println("2. Drop specific courses");
            System.out.println("3. Drop all courses");
            System.out.println("4. View registered courses");
            String choice = prompt("Select an option (or B to return): ");
            if (choice.isEmpty()) {
                return;
            }
            choice = choice.trim();
            if (choice.equalsIgnoreCase("b")) {
                return;
            }

            switch (choice) {
                case "1" -> addCourses();
                case "2" -> dropCourses();
                case "3" -> {
                    student.dropAllCourses();
                    System.out.println("All courses dropped.");
                }
                case "4" -> displayRegisteredCourses();
                default -> System.out.println("Please select 1, 2, 3, 4, or B.");
            }
        }
    }

    private String promptNonBlank(String message) {
        while (true) {
            System.out.print(message);
            System.out.flush();
            if (!input.hasNextLine()) {
                return null;
            }
            String value = input.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be blank.");
        }
    }

    private void addCourses() throws Exception {
        String subject = prompt("Enter the subject code (or B to return): ");
        if (subject.isEmpty() || subject.trim().equalsIgnoreCase("b")) {
            return;
        }
        List<Course> courses = schedule.queryCourseBySubject(
            subject.trim().toUpperCase(Locale.ROOT)
        );
        if (courses.isEmpty()) {
            System.out.println("No courses found for " + subject.trim() + ".");
            return;
        }

        displaySelectableCourses(courses);
        String selection = prompt(
            "Enter course number(s) to add, separated by commas (or B to cancel): "
        ).trim();
        if (selection.equalsIgnoreCase("b") || selection.isEmpty()) {
            return;
        }
        List<Integer> indexes = parseCourseSelections(selection, courses.size());
        if (indexes == null) {
            System.out.println("Invalid course selection.");
            return;
        }

        List<Course> selectedCourses = indexes.stream()
            .map(courses::get)
            .toList();
        Student.AddCoursesResult result = student.addCoursesWithResult(
            selectedCourses.toArray(Course[]::new)
        );
        switch (result) {
            case ADDED -> System.out.println("Course(s) added.");
            case COURSE_LIMIT_EXCEEDED -> System.out.println(
                "Could not add course(s): registration is limited to "
                    + Student.MAX_CREDITS + " course sections."
            );
            case COURSE_NOT_OPEN -> System.out.println(
                "Could not add course(s): at least one selected section is not "
                    + "open; it may be full."
            );
            case ALREADY_REGISTERED -> System.out.println(
                "Could not add course(s): at least one selected section is "
                    + "already on your schedule."
            );
            case TIME_CONFLICT -> System.out.println(
                "Could not add course(s): at least one selected section has a "
                    + "meeting-time conflict with your schedule or another "
                    + "selected section."
            );
            case NO_COURSES -> System.out.println("No courses were selected.");
        }
    }

    private void dropCourses() {
        List<Course> registeredCourses = student.getCourses();
        if (registeredCourses.isEmpty()) {
            System.out.println("No courses are currently registered.");
            return;
        }
        displaySelectableCourses(registeredCourses);
        String selection = prompt(
            "Enter course number(s) to drop, separated by commas (or B to cancel): "
        ).trim();
        if (selection.equalsIgnoreCase("b") || selection.isEmpty()) {
            return;
        }
        List<Integer> indexes = parseCourseSelections(
            selection,
            registeredCourses.size()
        );
        if (indexes == null) {
            System.out.println("Invalid course selection.");
            return;
        }

        List<Course> selectedCourses = indexes.stream()
            .map(registeredCourses::get)
            .toList();
        student.dropCourses(selectedCourses.toArray(Course[]::new));
        System.out.println("Selected course(s) dropped.");
    }

    private void displayRegisteredCourses() {
        List<Course> registeredCourses = student.getCourses();
        if (registeredCourses.isEmpty()) {
            System.out.println("No courses are currently registered.");
            return;
        }
        displaySelectableCourses(registeredCourses);
    }

    private static void displaySelectableCourses(List<Course> courses) {
        for (int index = 0; index < courses.size(); index++) {
            System.out.print((index + 1) + ". ");
            displayCourse(courses.get(index));
        }
    }

    private static List<Integer> parseCourseSelections(
        String selection,
        int courseCount
    ) {
        List<Integer> indexes = new ArrayList<>();
        Set<Integer> uniqueIndexes = new HashSet<>();
        for (String value : selection.split(",")) {
            try {
                int index = Integer.parseInt(value.trim()) - 1;
                if (index < 0 || index >= courseCount || !uniqueIndexes.add(index)) {
                    return null;
                }
                indexes.add(index);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return indexes;
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
        System.out.flush();
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
