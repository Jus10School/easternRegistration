package com.example;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

public class MainMenuTest {

    private static final String DISPLAY_COURSES_JSON = """
        {
          "success": true,
          "data": [
            {
              "id": 123,
              "subject": "CSC",
              "courseNumber": "360",
              "sequenceNumber": "02",
              "courseTitle": "Software Development with AI",
              "creditHours": null,
              "creditHourLow": 3,
              "creditHourHigh": null,
              "openSection": true,
              "faculty": [],
              "meetingsFaculty": [
                {
                  "meetingTime": {
                    "beginTime": "1400",
                    "endTime": "1515",
                    "monday": false,
                    "tuesday": true,
                    "wednesday": false,
                    "thursday": true,
                    "friday": false,
                    "saturday": false,
                    "sunday": false
                  },
                  "faculty": []
                },
                {
                  "meetingTime": {
                    "beginTime": "0900",
                    "endTime": "0950",
                    "monday": true,
                    "tuesday": false,
                    "wednesday": false,
                    "thursday": false,
                    "friday": false,
                    "saturday": false,
                    "sunday": false
                  },
                  "faculty": []
                }
              ]
            }
          ]
        }
        """;

    private static final String FILTER_COURSES_JSON = """
        {
          "success": true,
          "data": [
            {
              "id": 1,
              "subject": "CSC",
              "courseNumber": "360",
              "sequenceNumber": "02",
              "courseTitle": "Software Development with AI",
              "creditHourLow": 3,
              "openSection": true,
              "faculty": [{"displayName": "Smith, Alex"}],
              "meetingsFaculty": [
                {
                  "meetingTime": {
                    "beginTime": "1400",
                    "endTime": "1515",
                    "monday": false,
                    "tuesday": false,
                    "wednesday": true,
                    "thursday": false,
                    "friday": false,
                    "saturday": false,
                    "sunday": false
                  },
                  "faculty": []
                }
              ]
            },
            {
              "id": 2,
              "subject": "CSC",
              "courseNumber": "360",
              "sequenceNumber": "01",
              "courseTitle": "Another Section",
              "creditHourLow": 3,
              "openSection": false,
              "faculty": [{"displayName": "Smith, Alex"}],
              "meetingsFaculty": []
            },
            {
              "id": 4,
              "subject": "CSC",
              "courseNumber": "360",
              "sequenceNumber": "03",
              "courseTitle": "Tuesday Section",
              "creditHourLow": 3,
              "openSection": true,
              "faculty": [{"displayName": "Smith, Alex"}],
              "meetingsFaculty": [
                {
                  "meetingTime": {
                    "beginTime": "1400",
                    "endTime": "1515",
                    "monday": false,
                    "tuesday": true,
                    "wednesday": false,
                    "thursday": false,
                    "friday": false,
                    "saturday": false,
                    "sunday": false
                  },
                  "faculty": []
                }
              ]
            },
            {
              "id": 3,
              "subject": "CSC",
              "courseNumber": "361",
              "sequenceNumber": "01",
              "courseTitle": "Different Course",
              "creditHourLow": 3,
              "openSection": true,
              "faculty": [{"displayName": "Jones, Pat"}],
              "meetingsFaculty": []
            }
          ]
        }
        """;

    @Test
    public void displaysCourseDetailsAndEveryMeeting() throws Exception {
        EasternSchedule schedule = scheduleFromJson(DISPLAY_COURSES_JSON);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput =
                 new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            new MainMenu(schedule, new Scanner("1\ncsc\nq\n")).run();
        } finally {
            System.setOut(originalOutput);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains(
            "CSC 360-02, 3 credits, Software Development with AI, TR, 2:00 \u2013 3:15 PM"
        ));
        assertTrue(result.contains("  M, 9:00 \u2013 9:50 AM"));
    }

    @Test
    public void filtersByNumberOpenInstructorAndExcludedDay() throws Exception {
        List<Course> courses = coursesFromJson(FILTER_COURSES_JSON);
        List<Course> results = MainMenu.filterCourses(
            courses,
            "360",
            true,
            "smith",
            "Tuesday"
        );

        assertEquals(List.of(courses.get(0)), results);
    }

    @Test
    public void retriesSubjectWhenNoCoursesAreFound() throws Exception {
        EasternSchedule schedule = scheduleFromJsonForSubject(
            "CSC", FILTER_COURSES_JSON
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput =
                 new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            new MainMenu(schedule, new Scanner("2\nINVALID\nCSC\nq\n")).run();
        } finally {
            System.setOut(originalOutput);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("No courses found for INVALID."));
        assertTrue(result.contains("CSC 360-02"));
    }

    @Test
    public void retriesCourseNumberWhenNoCoursesAreFound() throws Exception {
        EasternSchedule schedule = scheduleFromJson(FILTER_COURSES_JSON);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput =
                 new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            new MainMenu(schedule, new Scanner("2\nCSC\n999\n360\nn\n\n\nq\n")).run();
        } finally {
            System.setOut(originalOutput);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains(
            "No courses found for CSC 999. Please enter another course number."
        ));
        assertTrue(result.contains("Only open courses?"));
        assertTrue(result.contains("CSC 360-02"));
    }

    @Test
    public void returnsToMainMenuWhenUserEntersBack() throws Exception {
        EasternSchedule schedule = scheduleFromJson(DISPLAY_COURSES_JSON);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream capturedOutput =
                 new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOutput);
            new MainMenu(schedule, new Scanner("2\nb\n1\nCSC\nq\n")).run();
        } finally {
            System.setOut(originalOutput);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Enter a subject code (or B to return to the menu):"));
        assertTrue(result.contains("CSC 360-02"));
    }

    private static EasternSchedule scheduleFromJson(String json) throws IOException {
        List<Course> courses = coursesFromJson(json);
        return new EasternSchedule() {
            @Override
            public ArrayList<Course> queryCourseBySubject(String subject) {
                assertEquals("CSC", subject);
                return new ArrayList<>(courses);
            }
        };
    }

    private static EasternSchedule scheduleFromJsonForSubject(
        String expectedSubject,
        String json
    ) throws IOException {
        List<Course> courses = coursesFromJson(json);
        return new EasternSchedule() {
            @Override
            public ArrayList<Course> queryCourseBySubject(String subject) {
                if (!expectedSubject.equals(subject)) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(courses);
            }
        };
    }

    private static List<Course> coursesFromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.readTree(json).get("data");
        return mapper.convertValue(data, new TypeReference<ArrayList<Course>>() {});
    }
}
