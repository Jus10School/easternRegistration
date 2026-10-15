package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StudentTest {

    @Test
    public void createsStudentAndRegistersAvailableCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course first = course("1", true, meeting("0900", "0950", true, false));
        Course second = course("2", true, meeting("1000", "1050", true, false));

        assertEquals("s123", student.getId());
        assertEquals("Ada", student.getFirstName());
        assertEquals("Lovelace", student.getLastName());
        assertTrue(student.addCourses(first, second));
        assertEquals(List.of(first, second), student.getCourses());
    }

    @Test
    public void rejectsFullDuplicateAndConflictingCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course enrolled = course("1", true, meeting("0900", "1000", true, false));

        assertTrue(student.addCourse(enrolled));
        assertEquals(
            Student.AddCoursesResult.COURSE_NOT_OPEN,
            student.addCoursesWithResult(course("2", false))
        );
        assertEquals(
            Student.AddCoursesResult.ALREADY_REGISTERED,
            student.addCoursesWithResult(enrolled)
        );
        assertEquals(
            Student.AddCoursesResult.TIME_CONFLICT,
            student.addCoursesWithResult(course(
                "3", true, meeting("0930", "1030", true, false)
            ))
        );
        assertEquals(List.of(enrolled), student.getCourses());
    }

    @Test
    public void allowsBackToBackAndDifferentDayCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        assertTrue(student.addCourses(
            course("1", true, meeting("0900", "1000", true, false)),
            course("2", true, meeting("1000", "1100", true, false)),
            course("3", true, meeting("0930", "1030", false, true))
        ));
    }

    @Test
    public void rejectsConflictingBatchWithoutAddingAnyCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        assertEquals(
            Student.AddCoursesResult.TIME_CONFLICT,
            student.addCoursesWithResult(
                course("1", true, meeting("0900", "1000", true, false)),
                course("2", true, meeting("0930", "1030", true, false))
            )
        );
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void enforcesCreditLimitAndAllowsRegistrationAfterDrop() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course[] courses = new Course[6];
        for (int index = 0; index < 5; index++) {
            courses[index] = course(Integer.toString(index), true, 3);
        }
        courses[5] = course("5", true, 3);

        assertTrue(student.addCourses(Arrays.copyOf(courses, 5)));
        assertEquals(new BigDecimal("15"), student.getRegisteredCredits());
        assertEquals(Student.AddCoursesResult.COURSE_LIMIT_EXCEEDED,
            student.addCoursesWithResult(courses[5]));
        assertEquals(5, student.getCourses().size());
        assertEquals(new BigDecimal("15"), student.getRegisteredCredits());

        assertTrue(student.dropCourse(courses[0]));
        assertTrue(student.addCourse(courses[5]));
        assertEquals(new BigDecimal("15"), student.getRegisteredCredits());
        assertEquals(5, student.getCourses().size());
    }

    @Test
    public void usesMaximumCreditsForVariableCreditCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course fifteenCredits = course("1", true, 15);
        Course variableCourse = new Course(
            "2", "CSC", "200", "02", "Variable",
            null, new BigDecimal("1"), new BigDecimal("3"),
            true, List.of(), List.of()
        );
        assertTrue(student.addCourse(fifteenCredits));
        assertEquals(Student.AddCoursesResult.COURSE_LIMIT_EXCEEDED,
            student.addCoursesWithResult(variableCourse));
        assertEquals(new BigDecimal("15"), student.getRegisteredCredits());
    }

    @Test
    public void rejectsCoursesWithUnknownCreditHours() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course unknownCredits = new Course(
            "1", "CSC", "200", "01", "Unknown credits",
            null, null, null, true, List.of(), List.of()
        );

        assertEquals(Student.AddCoursesResult.COURSE_CREDITS_UNKNOWN,
            student.addCoursesWithResult(unknownCredits));
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    public void dropsSpecificCoursesOrAllCourses() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course first = course("1", true);
        Course second = course("2", true);
        assertTrue(student.addCourses(first, second));

        assertTrue(student.dropCourses(first, course("missing", true)));
        assertEquals(List.of(second), student.getCourses());
        assertFalse(student.dropCourse(first));
        student.dropAllCourses();
        assertTrue(student.getCourses().isEmpty());
    }

    private static Course course(
        String id,
        boolean open,
        Course.MeetingFaculty... meetings
    ) {
        return course(id, open, 3, meetings);
    }

    private static Course course(
        String id,
        boolean open,
        int credits,
        Course.MeetingFaculty... meetings
    ) {
        return new Course(
            id,
            "CSC",
            "100",
            id,
            "Course " + id,
            BigDecimal.valueOf(credits),
            null,
            null,
            open,
            List.of(),
            List.of(meetings)
        );
    }

    private static Course.MeetingFaculty meeting(
        String start,
        String end,
        boolean monday,
        boolean tuesday
    ) {
        return new Course.MeetingFaculty(
            new Course.MeetingTime(
                start, end, monday, tuesday, false, false, false, false, false
            ),
            List.of()
        );
    }
}
