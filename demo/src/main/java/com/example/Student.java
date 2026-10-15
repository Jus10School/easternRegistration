package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A student and the courses they are currently registered for.
 */
public class Student {

    public static final int MAX_CREDITS = 17;

    public enum AddCoursesResult {
        ADDED,
        NO_COURSES,
        COURSE_LIMIT_EXCEEDED,
        COURSE_CREDITS_UNKNOWN,
        COURSE_NOT_OPEN,
        ALREADY_REGISTERED,
        TIME_CONFLICT
    }

    private static final DateTimeFormatter COURSE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");

    private final String id;
    private final String firstName;
    private final String lastName;
    private final List<Course> courses = new ArrayList<>();

    public Student(String id, String firstName, String lastName) {
        this.id = requireText(id, "id");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public List<Course> getCourses() {
        return List.copyOf(courses);
    }

    public BigDecimal getRegisteredCredits() {
        return totalCredits(courses);
    }

    /**
     * Adds one course if it is available, fits the registration limit, and
     * does not conflict with an existing course.
     */
    public boolean addCourse(Course course) {
        return addCourses(course);
    }

    /**
     * Adds all supplied courses, or adds none if any course cannot be added.
     */
    public boolean addCourses(Course... coursesToAdd) {
        return addCoursesWithResult(coursesToAdd) == AddCoursesResult.ADDED;
    }

    /**
     * Adds all supplied courses, or adds none and returns the reason for
     * rejecting the batch.
     */
    public AddCoursesResult addCoursesWithResult(Course... coursesToAdd) {
        Objects.requireNonNull(coursesToAdd, "coursesToAdd");
        if (coursesToAdd.length == 0) {
            return AddCoursesResult.NO_COURSES;
        }
        List<Course> candidates = new ArrayList<>(courses);
        for (Course course : coursesToAdd) {
            if (course == null || !Boolean.TRUE.equals(course.openSection())) {
                return AddCoursesResult.COURSE_NOT_OPEN;
            }
            if (containsCourse(candidates, course)) {
                return AddCoursesResult.ALREADY_REGISTERED;
            }
            if (conflictsWithAny(course, candidates)) {
                return AddCoursesResult.TIME_CONFLICT;
            }
            candidates.add(course);
            BigDecimal candidateCredits = totalCredits(candidates);
            if (candidateCredits == null) {
                return AddCoursesResult.COURSE_CREDITS_UNKNOWN;
            }
            if (candidateCredits.compareTo(BigDecimal.valueOf(MAX_CREDITS)) > 0) {
                return AddCoursesResult.COURSE_LIMIT_EXCEEDED;
            }
        }

        courses.addAll(Arrays.asList(coursesToAdd));
        return AddCoursesResult.ADDED;
    }

    /**
     * Drops a course, returning whether the student was registered for it.
     */
    public boolean dropCourse(Course course) {
        return dropCourses(course);
    }

    /**
     * Drops each matching course. Returns true if at least one was dropped.
     */
    public boolean dropCourses(Course... coursesToDrop) {
        Objects.requireNonNull(coursesToDrop, "coursesToDrop");
        boolean dropped = false;
        for (Course courseToDrop : coursesToDrop) {
            Objects.requireNonNull(courseToDrop, "courseToDrop");
            int index = findCourseIndex(courses, courseToDrop);
            if (index >= 0) {
                courses.remove(index);
                dropped = true;
            }
        }
        return dropped;
    }

    public void dropAllCourses() {
        courses.clear();
    }

    private static boolean conflictsWithAny(Course course, List<Course> others) {
        for (Course other : others) {
            if (hasTimeConflict(course, other)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimeConflict(Course first, Course second) {
        if (first.meetingsFaculty() == null || second.meetingsFaculty() == null) {
            return false;
        }
        for (Course.MeetingFaculty firstMeeting : first.meetingsFaculty()) {
            Course.MeetingTime firstTime = meetingTime(firstMeeting);
            if (firstTime == null) {
                continue;
            }
            LocalTime firstStart = parseTime(firstTime.beginTime());
            LocalTime firstEnd = parseTime(firstTime.endTime());
            if (firstStart == null || firstEnd == null) {
                continue;
            }

            for (Course.MeetingFaculty secondMeeting : second.meetingsFaculty()) {
                Course.MeetingTime secondTime = meetingTime(secondMeeting);
                if (secondTime == null || !sharesMeetingDay(firstTime, secondTime)) {
                    continue;
                }
                LocalTime secondStart = parseTime(secondTime.beginTime());
                LocalTime secondEnd = parseTime(secondTime.endTime());
                if (secondStart != null && secondEnd != null
                    && firstStart.isBefore(secondEnd)
                    && secondStart.isBefore(firstEnd)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Course.MeetingTime meetingTime(Course.MeetingFaculty meeting) {
        return meeting == null ? null : meeting.meetingTime();
    }

    private static boolean sharesMeetingDay(
        Course.MeetingTime first,
        Course.MeetingTime second
    ) {
        return (first.monday() && second.monday())
            || (first.tuesday() && second.tuesday())
            || (first.wednesday() && second.wednesday())
            || (first.thursday() && second.thursday())
            || (first.friday() && second.friday())
            || (first.saturday() && second.saturday())
            || (first.sunday() && second.sunday());
    }

    private static LocalTime parseTime(String value) {
        if (value == null || !value.matches("\\d{4}")) {
            return null;
        }
        try {
            return LocalTime.parse(value, COURSE_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static boolean containsCourse(List<Course> list, Course target) {
        return findCourseIndex(list, target) >= 0;
    }

    private static BigDecimal totalCredits(List<Course> registeredCourses) {
        BigDecimal total = BigDecimal.ZERO;
        for (Course course : registeredCourses) {
            BigDecimal credits = course.creditHours();
            if (credits == null) {
                credits = course.creditHourHigh() != null
                    ? course.creditHourHigh()
                    : course.creditHourLow();
            }
            if (credits == null || credits.signum() < 0) {
                return null;
            }
            total = total.add(credits);
        }
        return total;
    }

    private static int findCourseIndex(List<Course> list, Course target) {
        for (int index = 0; index < list.size(); index++) {
            Course current = list.get(index);
            if (current.id() != null && target.id() != null
                ? current.id().equals(target.id())
                : current.equals(target)) {
                return index;
            }
        }
        return -1;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
