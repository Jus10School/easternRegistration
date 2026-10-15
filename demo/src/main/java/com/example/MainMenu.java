package com.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Graphical main menu for student creation and course registration.
 */
public class MainMenu {

    private static final String CREATE_STUDENT_CARD = "create-student";
    private static final String REGISTRATION_CARD = "registration";

    private final EasternSchedule schedule;
    private final JFrame frame = new JFrame("Eastern Registration");
    private Student student;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JTextField idField = new JTextField(20);
    private final JTextField firstNameField = new JTextField(20);
    private final JTextField lastNameField = new JTextField(20);
    private final JTextField subjectField = new JTextField(12);
    private final JTextField courseNumberField = new JTextField(7);
    private final JTextField instructorField = new JTextField(12);
    private final JComboBox<String> excludedDayCombo = new JComboBox<>(
        new String[] {"None", "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday"}
    );
    private final JCheckBox openOnlyCheckBox = new JCheckBox("Open only");
    private final DefaultListModel<Course> searchResults = new DefaultListModel<>();
    private final DefaultListModel<Course> registeredCourses = new DefaultListModel<>();
    private final JList<Course> searchList = createCourseList(searchResults);
    private final JList<Course> registeredList = createCourseList(registeredCourses);
    private final JLabel studentLabel = new JLabel(" ");
    private final JLabel registeredCreditsLabel = new JLabel("0 / "
        + Student.MAX_CREDITS + " credits");
    private final JLabel statusLabel = new JLabel("Search by subject to find courses.");
    private final JButton searchButton = new JButton("Search Courses");
    private final JButton addButton = new JButton("Add Selected");

    public MainMenu(EasternSchedule schedule) {
        this.schedule = schedule;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setContentPane(cards);
        cards.add(createStudentPanel(), CREATE_STUDENT_CARD);
        cards.add(createRegistrationPanel(), REGISTRATION_CARD);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    private JPanel createStudentPanel() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel title = new JLabel("Eastern Registration", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel subtitle = new JLabel(
            "Enter student information to manage course registration.",
            SwingConstants.CENTER
        );
        JPanel headings = new JPanel(new BorderLayout(4, 4));
        headings.add(title, BorderLayout.NORTH);
        headings.add(subtitle, BorderLayout.SOUTH);
        page.add(headings, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.anchor = GridBagConstraints.LINE_END;
        addFormRow(form, constraints, 0, "Student ID:", idField);
        addFormRow(form, constraints, 1, "First name:", firstNameField);
        addFormRow(form, constraints, 2, "Last name:", lastNameField);

        JButton createButton = new JButton("Continue");
        createButton.addActionListener(event -> createStudent());
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(createButton, constraints);
        page.add(form, BorderLayout.CENTER);
        return page;
    }

    private static void addFormRow(
        JPanel form,
        GridBagConstraints constraints,
        int row,
        String label,
        JTextField field
    ) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(field, constraints);
    }

    private JPanel createRegistrationPanel() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setBorder(BorderFactory.createEmptyBorder(18, 22, 14, 22));

        JLabel title = new JLabel("Student Course Registration");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(studentLabel, BorderLayout.SOUTH);
        page.add(header, BorderLayout.NORTH);

        JPanel courseSearchPanel = new JPanel(new BorderLayout(8, 8));
        courseSearchPanel.setBorder(BorderFactory.createTitledBorder("Find courses"));
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchControls.add(new JLabel("Subject:"));
        searchControls.add(subjectField);
        searchButton.addActionListener(event -> searchCourses());
        searchControls.add(searchButton);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filters.add(new JLabel("Course number:"));
        filters.add(courseNumberField);
        filters.add(openOnlyCheckBox);
        filters.add(new JLabel("Instructor contains:"));
        filters.add(instructorField);
        filters.add(new JLabel("Exclude day:"));
        filters.add(excludedDayCombo);
        JPanel searchHeader = new JPanel(new BorderLayout());
        searchHeader.add(searchControls, BorderLayout.NORTH);
        searchHeader.add(filters, BorderLayout.SOUTH);
        courseSearchPanel.add(searchHeader, BorderLayout.NORTH);
        courseSearchPanel.add(new JScrollPane(searchList), BorderLayout.CENTER);
        addButton.addActionListener(event -> addSelectedCourses());
        JPanel addControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addControls.add(addButton);
        courseSearchPanel.add(addControls, BorderLayout.SOUTH);

        JPanel registeredPanel = new JPanel(new BorderLayout(8, 8));
        registeredPanel.setBorder(BorderFactory.createTitledBorder("Registered courses"));
        registeredPanel.add(new JScrollPane(registeredList), BorderLayout.CENTER);
        JButton dropButton = new JButton("Drop Selected");
        dropButton.addActionListener(event -> dropSelectedCourses());
        JButton dropAllButton = new JButton("Drop All");
        dropAllButton.addActionListener(event -> dropAllCourses());
        JPanel dropControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        dropControls.add(dropButton);
        dropControls.add(dropAllButton);
        JPanel registeredFooter = new JPanel(new BorderLayout());
        registeredFooter.add(registeredCreditsLabel, BorderLayout.WEST);
        registeredFooter.add(dropControls, BorderLayout.EAST);
        registeredPanel.add(registeredFooter, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.add(courseSearchPanel, BorderLayout.CENTER);
        content.add(registeredPanel, BorderLayout.EAST);
        registeredPanel.setPreferredSize(new Dimension(390, 400));
        page.add(content, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        page.add(statusLabel, BorderLayout.SOUTH);
        return page;
    }

    private void createStudent() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                "Enter a student ID, first name, and last name.",
                "Student information required",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            student = new Student(id, firstName, lastName);
        } catch (IllegalArgumentException exception) {
            showError("Could not create student", exception);
            return;
        }
        studentLabel.setText(
            student.getFirstName() + " " + student.getLastName()
                + "  |  Student ID: " + student.getId()
        );
        cardLayout.show(cards, REGISTRATION_CARD);
    }

    private Student currentStudent() {
        return student;
    }

    private void searchCourses() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            statusLabel.setText("Enter a subject code to search.");
            subjectField.requestFocusInWindow();
            return;
        }

        setSearchBusy(true);
        statusLabel.setText("Searching courses...");
        String normalizedSubject = subject.toUpperCase(java.util.Locale.ROOT);
        new SwingWorker<List<Course>, Void>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                return schedule.queryCourseBySubject(normalizedSubject);
            }

            @Override
            protected void done() {
                setSearchBusy(false);
                try {
                    List<Course> courses = filterCourses(
                        get(),
                        courseNumberField.getText(),
                        openOnlyCheckBox.isSelected(),
                        instructorField.getText(),
                        "None".equals(excludedDayCombo.getSelectedItem())
                            ? ""
                            : (String) excludedDayCombo.getSelectedItem()
                    );
                    searchResults.clear();
                    for (Course course : courses) {
                        searchResults.addElement(course);
                    }
                    statusLabel.setText(courses.isEmpty()
                        ? "No courses found for " + normalizedSubject + "."
                        : courses.size() + " course section(s) found.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Course search was interrupted", exception);
                } catch (ExecutionException exception) {
                    showError("Could not search courses", exception.getCause());
                }
            }
        }.execute();
    }

    private void addSelectedCourses() {
        List<Course> selected = searchList.getSelectedValuesList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select at least one course section to add.");
            return;
        }

        Student.AddCoursesResult result = currentStudent().addCoursesWithResult(
            selected.toArray(Course[]::new)
        );
        statusLabel.setText(messageFor(result));
        if (result == Student.AddCoursesResult.ADDED) {
            refreshRegisteredCourses();
        }
    }

    private void dropSelectedCourses() {
        List<Course> selected = registeredList.getSelectedValuesList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select at least one registered course to drop.");
            return;
        }

        currentStudent().dropCourses(selected.toArray(Course[]::new));
        refreshRegisteredCourses();
        statusLabel.setText("Selected course(s) dropped.");
    }

    private void dropAllCourses() {
        if (registeredCourses.isEmpty()) {
            statusLabel.setText("There are no registered courses to drop.");
            return;
        }
        currentStudent().dropAllCourses();
        refreshRegisteredCourses();
        statusLabel.setText("All courses dropped.");
    }

    private void refreshRegisteredCourses() {
        registeredCourses.clear();
        for (Course course : currentStudent().getCourses()) {
            registeredCourses.addElement(course);
        }
        registeredCreditsLabel.setText(
            currentStudent().getRegisteredCredits().stripTrailingZeros().toPlainString()
                + " / " + Student.MAX_CREDITS + " credits"
        );
    }

    private void setSearchBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        subjectField.setEnabled(!busy);
        addButton.setEnabled(!busy);
    }

    private void showError(String title, Throwable error) {
        String message = error == null || error.getMessage() == null
            ? "An unexpected error occurred."
            : error.getMessage();
        JOptionPane.showMessageDialog(
            frame,
            message,
            title,
            JOptionPane.ERROR_MESSAGE
        );
        statusLabel.setText(title + ": " + message);
    }

    private static String messageFor(Student.AddCoursesResult result) {
        return switch (result) {
            case ADDED -> "Course(s) added.";
            case NO_COURSES -> "No courses were selected.";
            case COURSE_LIMIT_EXCEEDED -> "Could not add course(s): registration "
                + "would exceed the " + Student.MAX_CREDITS + "-credit limit.";
            case COURSE_CREDITS_UNKNOWN -> "Could not add course(s): credit "
                + "hours are unavailable for a selected course, so the credit "
                + "limit cannot be verified.";
            case COURSE_NOT_OPEN -> "Could not add course(s): at least one "
                + "selected section is not open; it may be full.";
            case ALREADY_REGISTERED -> "Could not add course(s): at least one "
                + "selected section is already on your schedule.";
            case TIME_CONFLICT -> "Could not add course(s): at least one "
                + "selected section has a meeting-time conflict.";
        };
    }

    private static JList<Course> createCourseList(DefaultListModel<Course> model) {
        JList<Course> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );
                label.setText(courseDescription((Course) value));
                label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                return label;
            }
        });
        return list;
    }

    private static String courseDescription(Course course) {
        String title = course.courseTitle() == null || course.courseTitle().isBlank()
            ? "Title TBA"
            : course.courseTitle().replace("&amp;", "&");
        List<String> meetings = new ArrayList<>();
        if (course.meetingsFaculty() != null) {
            course.meetingsFaculty().stream()
                .filter(meeting -> meeting != null)
                .map(Course.MeetingFaculty::toString)
                .forEach(meetings::add);
        }
        String meetingDescription = meetings.isEmpty()
            ? "Meeting time TBA"
            : String.join("; ", meetings);
        String availability = Boolean.TRUE.equals(course.openSection())
            ? "Open"
            : "Closed / full";
        return "<html><b>" + escapeHtml(course.toString()) + "</b> &mdash; "
            + escapeHtml(title) + "<br>"
            + escapeHtml(course.creditsDescription()) + " | "
            + escapeHtml(meetingDescription) + " | " + availability + "</html>";
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
            .anyMatch(meeting -> containsInstructor(
                meeting.faculty(),
                instructorFilter
            ));
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
}
