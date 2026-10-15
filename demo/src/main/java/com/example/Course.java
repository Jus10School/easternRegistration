package com.example;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course data returned by Eastern's registration system.
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Faculty(String displayName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime, List<Faculty> faculty) {
        @Override
        public String toString() {
            return meetingTime == null ? "Meeting time TBA" : meetingTime.toString();
        }
    }

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

        @Override
        public String toString() {
            return days() + ", " + timeRange();
        }

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

    @Override
    public String toString() {
        String section = sequenceNumber == null || sequenceNumber.isBlank()
            ? ""
            : "-" + sequenceNumber;
        return subject + " " + courseNumber + section;
    }
}
