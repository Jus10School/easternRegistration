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
 *
 * <p>Registration is limited by total credit hours, section availability,
 * and meeting-time conflicts. Course registration changes are kept in memory
 * and are not persisted by this class.</p>
 */
public class Student {

    /** Maximum number of credits a student may register for. */
    public static final int MAX_CREDITS = 17;

    /** Result of attempting to register for one or more courses. */
    public enum AddCoursesResult {
        /** All requested courses were added. */
        ADDED,
        /** No courses were supplied. */
        NO_COURSES,
        /** The requested courses would exceed the maximum credit load. */
        COURSE_LIMIT_EXCEEDED,
        /** Credit-hour data is missing, so the limit cannot be verified. */
        COURSE_CREDITS_UNKNOWN,
        /** At least one requested section is not open. */
        COURSE_NOT_OPEN,
        /** At least one requested section is already on the student's schedule. */
        ALREADY_REGISTERED,
        /** At least one requested section conflicts with another meeting. */
        TIME_CONFLICT
    }

    private static final DateTimeFormatter COURSE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("HHmm");

    private final String id;
    private final String firstName;
    private final String lastName;
    private final List<Course> courses = new ArrayList<>();

    /**
     * Creates a student with no registered courses.
     *
     * @param id unique student identifier
     * @param firstName student's first name
     * @param lastName student's last name
     * @throws IllegalArgumentException if any value is null or blank
     */
    public Student(String id, String firstName, String lastName) {
        this.id = requireText(id, "id");
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
    }

    /**
     * Returns the student's identifier.
     *
     * @return the student identifier supplied at construction
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the student's first name.
     *
     * @return the first name supplied at construction
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the student's last name.
     *
     * @return the last name supplied at construction
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns an immutable snapshot of the student's registered courses.
     *
     * @return registered course sections
     */
    public List<Course> getCourses() {
        return List.copyOf(courses);
    }

    /**
     * Calculates the student's current registered credit total.
     *
     * @return registered credits, or {@code null} if a course credit value is
     *         missing or negative. For variable-credit courses, the upper credit
     *         value is used when available.
     */
    public BigDecimal getRegisteredCredits() {
        return totalCredits(courses);
    }

    /**
     * Adds all supplied courses atomically and reports the outcome.
     *
     * <p>No courses are added unless every supplied section is open, not
     * already registered, free of meeting conflicts, and within the credit
     * limit. Variable-credit courses count their upper credit value.</p>
     *
     * @param coursesToAdd course sections to register
     * @return the result of the registration attempt
     * @throws NullPointerException if the array itself is {@code null}
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
     * Drops each matching course section from the student's schedule.
     *
     * <p>Sections are matched by their Eastern identifier when both have one;
     * otherwise the complete course records are compared.</p>
     *
     * @param coursesToDrop course sections to drop
     * @return {@code true} if at least one section was dropped
     * @throws NullPointerException if the array or any element is {@code null}
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

    /**
     * Removes all courses from the student's schedule.
     *
     * <p>This operation does nothing when the schedule is already empty.</p>
     */
    public void dropAllCourses() {
        courses.clear();
    }

    /**
     * Checks whether a candidate overlaps any section in the supplied schedule.
     *
     * @param course candidate section
     * @param others sections to compare against
     * @return whether any pair of meetings conflicts
     */
    private static boolean conflictsWithAny(Course course, List<Course> others) {
        for (Course other : others) {
            if (hasTimeConflict(course, other)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for overlapping meeting intervals on a shared weekday.
     *
     * <p>Meetings with missing or invalid time data are ignored.</p>
     *
     * @param first first section
     * @param second second section
     * @return whether the sections have an overlapping meeting
     */
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

    /**
     * Returns the meeting time for a faculty-meeting record.
     *
     * @param meeting meeting record, possibly {@code null}
     * @return meeting time, or {@code null} when the meeting data is absent
     */
    private static Course.MeetingTime meetingTime(Course.MeetingFaculty meeting) {
        return meeting == null ? null : meeting.meetingTime();
    }

    /**
     * Checks whether two meeting schedules share at least one day.
     *
     * @param first first meeting schedule
     * @param second second meeting schedule
     * @return whether both schedules include a common day
     */
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

    /**
     * Parses an Eastern {@code HHmm} time value.
     *
     * @param value time value to parse
     * @return parsed time, or {@code null} when absent or invalid
     */
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

    /**
     * Tests whether the schedule contains a section matching the target.
     *
     * @param list sections to search
     * @param target section to find
     * @return whether a matching section exists
     */
    private static boolean containsCourse(List<Course> list, Course target) {
        return findCourseIndex(list, target) >= 0;
    }

    /**
     * Totals course credits, using the upper bound for variable-credit courses.
     *
     * @param registeredCourses sections whose credits should be counted
     * @return total credits, or {@code null} if any value is unknown or negative
     */
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

    /**
     * Finds a section by Eastern identifier, falling back to record equality.
     *
     * @param list sections to search
     * @param target section to find
     * @return matching index, or {@code -1} when not found
     */
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

    /**
     * Validates that a required text value is present.
     *
     * @param value value to validate
     * @param fieldName field name used in the error message
     * @return the original non-blank value
     * @throws IllegalArgumentException if the value is null or blank
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
