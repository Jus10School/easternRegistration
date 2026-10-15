package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
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
    public void enforcesCourseLimitAndAllowsRegistrationAfterDrop() {
        Student student = new Student("s123", "Ada", "Lovelace");
        Course[] courses = new Course[Student.MAX_CREDITS + 1];
        for (int index = 0; index < courses.length; index++) {
            courses[index] = course(Integer.toString(index), true);
        }

        assertEquals(
            Student.AddCoursesResult.COURSE_LIMIT_EXCEEDED,
            student.addCoursesWithResult(courses)
        );
        assertTrue(student.getCourses().isEmpty());
        assertTrue(student.addCourses(
            java.util.Arrays.copyOf(courses, Student.MAX_CREDITS)
        ));
        assertTrue(student.dropCourse(courses[0]));
        assertTrue(student.addCourse(courses[Student.MAX_CREDITS]));
        assertEquals(Student.MAX_CREDITS, student.getCourses().size());
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
        return new Course(
            id,
            "CSC",
            "100",
            id,
            "Course " + id,
            BigDecimal.valueOf(3),
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
