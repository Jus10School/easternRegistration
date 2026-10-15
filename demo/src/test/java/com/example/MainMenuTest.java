package com.example;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

public class MainMenuTest {

    @Test
    public void filtersCoursesByNumberAvailabilityInstructorAndExcludedDay() {
        Course matchingCourse = course(
            "1",
            "360",
            true,
            "Smith, Alex",
            meeting(true, false)
        );
        Course closedCourse = course(
            "2",
            "360",
            false,
            "Smith, Alex",
            meeting(false, true)
        );
        Course otherNumber = course(
            "3",
            "361",
            true,
            "Jones, Pat",
            meeting(false, true)
        );

        assertEquals(
            List.of(matchingCourse),
            MainMenu.filterCourses(
                List.of(matchingCourse, closedCourse, otherNumber),
                "360",
                true,
                "smith",
                "Tuesday"
            )
        );
    }

    @Test
    public void blankFiltersIncludeAllCourses() {
        Course first = course("1", "360", true, "Smith, Alex");
        Course second = course("2", "361", false, "Jones, Pat");

        assertEquals(
            List.of(first, second),
            MainMenu.filterCourses(List.of(first, second), "", false, "", "")
        );
    }

    private static Course course(
        String id,
        String number,
        boolean open,
        String instructor,
        Course.MeetingFaculty... meetings
    ) {
        return new Course(
            id,
            "CSC",
            number,
            id,
            "Course " + number,
            BigDecimal.valueOf(3),
            null,
            null,
            open,
            List.of(new Course.Faculty(instructor)),
            List.of(meetings)
        );
    }

    private static Course.MeetingFaculty meeting(
        boolean monday,
        boolean tuesday
    ) {
        return new Course.MeetingFaculty(
            new Course.MeetingTime(
                "0900", "0950", monday, tuesday, false, false, false, false, false
            ),
            List.of()
        );
    }
}
