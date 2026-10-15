package com.example;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course-section data returned by Eastern's registration system.
 *
 * <p>Credit hours may be reported as a fixed value or as a low/high range.
 * Meeting and faculty data may be absent from the service response.</p>
 *
 * @param id Eastern's identifier for the section
 * @param subject course subject code
 * @param courseNumber course number
 * @param sequenceNumber section number
 * @param courseTitle course title
 * @param creditHours fixed credit-hour value, when available
 * @param creditHourLow minimum credit-hour value, when applicable
 * @param creditHourHigh maximum credit-hour value, when applicable
 * @param openSection whether the section is open for registration
 * @param faculty instructors assigned to the section
 * @param meetingsFaculty meeting times and any faculty assigned to them
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
    String id,
    String subject,
    String courseNumber,
    String sequenceNumber,
    String courseTitle,
    BigDecimal creditHours,
    BigDecimal creditHourLow,
    BigDecimal creditHourHigh,
    Boolean openSection,
    List<Faculty> faculty,
    List<MeetingFaculty> meetingsFaculty
) {

    /**
     * Instructor information provided for a course section.
     *
     * @param displayName instructor's display name
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {}

    /**
     * A course meeting and any instructors assigned to that meeting.
     *
     * @param meetingTime meeting schedule, or {@code null} when unavailable
     * @param faculty instructors assigned to this meeting
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime, List<Faculty> faculty) {
        /**
         * Returns a readable meeting-time description.
         *
         * @return formatted meeting time, or {@code Meeting time TBA}
         */
        @Override
        public String toString() {
            return meetingTime == null ? "Meeting time TBA" : meetingTime.toString();
        }
    }

    /**
     * Days and time range for a course meeting.
     *
     * @param beginTime start time in {@code HHmm} format
     * @param endTime end time in {@code HHmm} format
     * @param monday whether the meeting occurs Monday
     * @param tuesday whether the meeting occurs Tuesday
     * @param wednesday whether the meeting occurs Wednesday
     * @param thursday whether the meeting occurs Thursday
     * @param friday whether the meeting occurs Friday
     * @param saturday whether the meeting occurs Saturday
     * @param sunday whether the meeting occurs Sunday
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        String beginTime,
        String endTime,
        boolean monday,
        boolean tuesday,
        boolean wednesday,
        boolean thursday,
        boolean friday,
        boolean saturday,
        boolean sunday
    ) {
        private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm");
        private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("a");

        /**
         * Returns meeting days in Monday-through-Sunday order using the
         * standard initials M, T, W, R, F, S, U.
         *
         * @return ordered meeting-day initials, or {@code Days TBD}
         */
        public String days() {
            HashMap<String, Boolean> daysMap = new HashMap<>();
            daysMap.put("M", monday);
            daysMap.put("T", tuesday);
            daysMap.put("W", wednesday);
            daysMap.put("R", thursday);
            daysMap.put("F", friday);
            daysMap.put("S", saturday);
            daysMap.put("U", sunday);

            ArrayList<String> dayOrder = new ArrayList<>(
                List.of("M", "T", "W", "R", "F", "S", "U")
            );
            StringBuilder days = new StringBuilder();
            for (String day : dayOrder) {
                if (Boolean.TRUE.equals(daysMap.get(day))) {
                    days.append(day);
                }
            }
            return days.isEmpty() ? "Days TBD" : days.toString();
        }

        /**
         * Returns the meeting time range in a human-readable 12-hour format.
         *
         * @return formatted time range, or {@code Time TBD} if either value is
         *         missing or invalid
         */
        public String timeRange() {
            LocalTime start = parseTime(beginTime);
            LocalTime end = parseTime(endTime);
            if (start == null || end == null) {
                return "Time TBD";
            }

            String startTime = start.format(TIME_FORMAT);
            String endTime = end.format(TIME_FORMAT) + " "
                + end.format(PERIOD_FORMAT);
            if (!start.format(PERIOD_FORMAT).equals(end.format(PERIOD_FORMAT))) {
                startTime += " " + start.format(PERIOD_FORMAT);
            }
            return startTime + " \u2013 " + endTime;
        }

        /**
         * Returns both the meeting days and time range.
         *
         * @return formatted meeting schedule
         */
        @Override
        public String toString() {
            return days() + ", " + timeRange();
        }

        /**
         * Parses a four-digit {@code HHmm} value when it represents a valid time.
         *
         * @param value time value to parse
         * @return parsed time, or {@code null} when absent or invalid
         */
        private static LocalTime parseTime(String value) {
            if (value == null || !value.matches("\\d{4}")) {
                return null;
            }
            return LocalTime.of(
                Integer.parseInt(value.substring(0, 2)),
                Integer.parseInt(value.substring(2, 4))
            );
        }
    }

    /**
     * Returns the course's credit-hour value or range as display text.
     *
     * <p>Uses the fixed credit value when present; otherwise displays the
     * minimum and maximum values when available.</p>
     *
     * @return credit-hour description, or {@code Credit hours TBA}
     */
    public String creditsDescription() {
        BigDecimal low = creditHours != null ? creditHours : creditHourLow;
        BigDecimal high = creditHours != null ? creditHours : creditHourHigh;
        if (low == null) {
            return "Credit hours TBA";
        }

        String description = formatCredits(low);
        if (high != null && high.compareTo(low) != 0) {
            description += "\u2013" + formatCredits(high);
        }
        return description + (low.compareTo(BigDecimal.ONE) == 0
            && (high == null || high.compareTo(BigDecimal.ONE) == 0)
            ? " credit"
            : " credits");
    }

    private static String formatCredits(BigDecimal credits) {
        return credits.stripTrailingZeros().toPlainString();
    }

    /**
     * Returns the subject, course number, and section number.
     *
     * @return concise course-section label
     */
    @Override
    public String toString() {
        String section = sequenceNumber == null || sequenceNumber.isBlank()
            ? ""
            : "-" + sequenceNumber;
        return subject + " " + courseNumber + section;
    }
}
