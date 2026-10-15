package com.example;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
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
 * Main window for student accounts and course registration.
 *
 * <p>The interface loads available terms, searches and filters course
 * sections, and lets a student add or drop sections in the current in-memory
 * schedule. Student accounts and their saved terms remain available until the
 * application closes; login matches the student ID and names after trimming
 * surrounding whitespace.</p>
 */
public class MainMenu {

    private static final String CREATE_STUDENT_CARD = "create-student";
    private static final String REGISTRATION_CARD = "registration";

    private final EasternSchedule schedule;
    private final JFrame frame = new JFrame("Eastern Registration");
    private final ImageIcon logoIcon = loadLogoIcon();
    private final Map<String, Student> studentAccounts = new HashMap<>();
    private final Map<String, String> studentTerms = new HashMap<>();
    private Student student;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JTextField idField = new JTextField(20);
    private final JTextField firstNameField = new JTextField(20);
    private final JTextField lastNameField = new JTextField(20);
    private final JTextField subjectField = new JTextField(12);
    private final JTextField courseNumberField = new JTextField(7);
    private final JTextField instructorField = new JTextField(12);
    private final JComboBox<Term> termCombo = new JComboBox<>();
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
    private final JButton backButton = new JButton("Back");
    private boolean termsLoading;
    private boolean termsLoaded;

    /**
     * Creates the registration window backed by the supplied schedule service.
     *
     * @param schedule service used to search Eastern's course schedule
     */
    public MainMenu(EasternSchedule schedule) {
        this.schedule = schedule;
        termCombo.setEnabled(false);
        termCombo.setToolTipText(
            "The selected term cannot be changed while courses are registered."
        );
        termCombo.addActionListener(event -> {
            searchResults.clear();
            searchList.clearSelection();
        });
        searchButton.setEnabled(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setContentPane(cards);
        cards.add(createStudentPanel(), CREATE_STUDENT_CARD);
        cards.add(createRegistrationPanel(), REGISTRATION_CARD);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /** Displays the registration window. */
    public void show() {
        frame.setVisible(true);
        loadTerms();
    }

    /**
     * Loads available terms and enables course search after success.
     *
     * <p>The network or cache operation runs off the Swing event-dispatch
     * thread; any failure is reported in the window.</p>
     */
    private void loadTerms() {
        if (termsLoaded || termsLoading) {
            return;
        }

        termsLoading = true;
        statusLabel.setText("Loading academic terms...");
        new SwingWorker<List<Term>, Void>() {
            @Override
            protected List<Term> doInBackground() throws Exception {
                return schedule.getAvailableTerms();
            }

            @Override
            protected void done() {
                termsLoading = false;
                try {
                    List<Term> terms = get();
                    if (terms.isEmpty()) {
                        throw new IllegalStateException(
                            "Eastern returned no available academic terms."
                        );
                    }
                    for (Term term : terms) {
                        termCombo.addItem(term);
                    }
                    for (int index = 0; index < termCombo.getItemCount(); index++) {
                        if (schedule.getTerm().equals(
                            termCombo.getItemAt(index).code()
                        )) {
                            termCombo.setSelectedIndex(index);
                            break;
                        }
                    }
                    selectSavedTermForCurrentStudent();
                    termsLoaded = true;
                    termCombo.setEnabled(
                        student == null || student.getCourses().isEmpty()
                    );
                    searchButton.setEnabled(true);
                    statusLabel.setText("Search by subject to find courses.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Loading terms was interrupted", exception);
                } catch (ExecutionException exception) {
                    showError("Could not load academic terms", exception.getCause());
                } catch (RuntimeException exception) {
                    showError("Could not load academic terms", exception);
                }
            }
        }.execute();
    }

    private JPanel createStudentPanel() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));

        JLabel title = new JLabel("Eastern Registration", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel subtitle = new JLabel("Create an account or log in", SwingConstants.CENTER);
        JPanel textHeadings = new JPanel();
        textHeadings.setLayout(new BoxLayout(textHeadings, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        textHeadings.add(title);
        textHeadings.add(Box.createVerticalStrut(8));
        textHeadings.add(subtitle);

        JPanel headings = new JPanel();
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        JLabel logo = createLogoLabel(190, 150);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        headings.add(logo);
        headings.add(Box.createVerticalStrut(12));
        headings.add(textHeadings);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.anchor = GridBagConstraints.LINE_END;
        addFormRow(form, constraints, 0, "Student ID:", idField);
        addFormRow(form, constraints, 1, "First name:", firstNameField);
        addFormRow(form, constraints, 2, "Last name:", lastNameField);

        JButton createAccountButton = new JButton("Create Account");
        createAccountButton.addActionListener(event -> createStudentAccount());
        JButton loginButton = new JButton("Log In");
        loginButton.addActionListener(event -> logInStudent());
        JPanel accountActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accountActions.add(createAccountButton);
        accountActions.add(loginButton);
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(accountActions, constraints);

        JPanel studentSetup = new JPanel(new GridBagLayout());
        GridBagConstraints setupConstraints = new GridBagConstraints();
        setupConstraints.gridx = 0;
        setupConstraints.anchor = GridBagConstraints.CENTER;
        setupConstraints.gridy = 0;
        setupConstraints.insets = new Insets(0, 0, 12, 0);
        studentSetup.add(headings, setupConstraints);
        setupConstraints.gridy = 1;
        setupConstraints.insets = new Insets(0, 0, 0, 0);
        studentSetup.add(form, setupConstraints);
        page.add(studentSetup, BorderLayout.CENTER);
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
        JPanel headerText = new JPanel(new BorderLayout(8, 8));
        headerText.add(title, BorderLayout.NORTH);
        headerText.add(studentLabel, BorderLayout.SOUTH);
        JPanel header = new JPanel(new BorderLayout(16, 4));
        header.add(createLogoLabel(130, 100), BorderLayout.WEST);
        header.add(headerText, BorderLayout.CENTER);
        page.add(header, BorderLayout.NORTH);

        JPanel courseSearchPanel = new JPanel(new BorderLayout(8, 8));
        courseSearchPanel.setBorder(BorderFactory.createTitledBorder("Find courses"));
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchControls.add(new JLabel("Term:"));
        searchControls.add(termCombo);
        searchControls.add(new JLabel("Subject:"));
        searchControls.add(subjectField);
        searchButton.addActionListener(event -> searchCourses());
        searchControls.add(searchButton);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filters.add(new JLabel("Course number:"));
        filters.add(courseNumberField);
        filters.add(openOnlyCheckBox);
        filters.add(new JLabel("Instructor name:"));
        filters.add(instructorField);
        filters.add(new JLabel("Exclude day:"));
        filters.add(excludedDayCombo);
        JPanel searchHeader = new JPanel(new BorderLayout());
        searchHeader.add(searchControls, BorderLayout.NORTH);
        searchHeader.add(filters, BorderLayout.SOUTH);
        courseSearchPanel.add(searchHeader, BorderLayout.NORTH);
        courseSearchPanel.add(new JScrollPane(searchList), BorderLayout.CENTER);
        addButton.addActionListener(event -> addSelectedCourses());
        backButton.addActionListener(event -> returnToLogin());
        JPanel courseActions = new JPanel(new BorderLayout());
        courseActions.add(backButton, BorderLayout.WEST);
        JPanel addControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addControls.add(addButton);
        courseActions.add(addControls, BorderLayout.EAST);
        courseSearchPanel.add(courseActions, BorderLayout.SOUTH);

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

    /** Creates and stores a new student account for this application session. */
    private void createStudentAccount() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        if (!hasValidStudentDetails(id, firstName, lastName)) {
            return;
        }
        if (studentAccounts.containsKey(id)) {
            showStudentDetailsError(
                "An account already exists for this student ID. Log in using "
                    + "the exact first and last name."
            );
            return;
        }

        try {
            Student newStudent = new Student(id, firstName, lastName);
            studentAccounts.put(id, newStudent);
            openStudentSession(newStudent);
        } catch (IllegalArgumentException exception) {
            showError("Could not create student", exception);
        }
    }

    /** Opens an existing account when the ID and both names match exactly. */
    private void logInStudent() {
        String id = idField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        if (!hasValidStudentDetails(id, firstName, lastName)) {
            return;
        }

        Student existingStudent = studentAccounts.get(id);
        if (existingStudent == null) {
            showStudentDetailsError(
                "No account exists for this student ID. Create an account first."
            );
            return;
        }
        if (!existingStudent.getFirstName().equals(firstName)
            || !existingStudent.getLastName().equals(lastName)) {
            showStudentDetailsError(
                "The student ID and name do not match the account."
            );
            return;
        }
        openStudentSession(existingStudent);
    }

    private boolean hasValidStudentDetails(
        String id,
        String firstName,
        String lastName
    ) {
        if (!id.isEmpty() && !firstName.isEmpty() && !lastName.isEmpty()) {
            return true;
        }
        showStudentDetailsError("Enter a student ID, first name, and last name.");
        return false;
    }

    private void showStudentDetailsError(String message) {
        JOptionPane.showMessageDialog(
            frame,
            message,
            "Student account",
            JOptionPane.WARNING_MESSAGE
        );
    }

    private void openStudentSession(Student selectedStudent) {
        student = selectedStudent;
        studentLabel.setText(
            student.getFirstName() + " " + student.getLastName()
                + "  |  Student ID: " + student.getId()
        );
        selectSavedTermForCurrentStudent();
        searchResults.clear();
        searchList.clearSelection();
        registeredList.clearSelection();
        refreshRegisteredCourses();
        statusLabel.setText("Search by subject to find courses.");
        cardLayout.show(cards, REGISTRATION_CARD);
    }

    private void selectSavedTermForCurrentStudent() {
        if (student == null) {
            return;
        }
        String savedTermCode = studentTerms.get(student.getId());
        if (savedTermCode == null) {
            return;
        }
        for (int index = 0; index < termCombo.getItemCount(); index++) {
            if (savedTermCode.equals(termCombo.getItemAt(index).code())) {
                termCombo.setSelectedIndex(index);
                return;
            }
        }
    }

    /**
     * Returns to student setup while retaining registered courses in memory.
     */
    private void returnToLogin() {
        student = null;
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        searchResults.clear();
        registeredCourses.clear();
        searchList.clearSelection();
        registeredList.clearSelection();
        registeredCreditsLabel.setText("0 / " + Student.MAX_CREDITS + " credits");
        statusLabel.setText("Search by subject to find courses.");
        cardLayout.show(cards, CREATE_STUDENT_CARD);
        idField.requestFocusInWindow();
    }

    /**
     * Searches the selected term and subject, then applies the active filters.
     */
    private void searchCourses() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            statusLabel.setText("Enter a subject code to search.");
            subjectField.requestFocusInWindow();
            return;
        }

        setSearchBusy(true);
        statusLabel.setText("Searching courses...");
        Term selectedTerm = (Term) termCombo.getSelectedItem();
        if (selectedTerm == null) {
            setSearchBusy(false);
            statusLabel.setText("Select an academic term before searching.");
            return;
        }
        String normalizedSubject = subject.toUpperCase(Locale.ROOT);
        String selectedTermCode = selectedTerm.code();
        new SwingWorker<List<Course>, Void>() {
            @Override
            protected List<Course> doInBackground() throws Exception {
                return schedule.queryCourseBySubject(
                    selectedTermCode,
                    normalizedSubject
                );
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

    /** Adds the selected search results if the student's schedule allows them. */
    private void addSelectedCourses() {
        List<Course> selected = searchList.getSelectedValuesList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select at least one course section to add.");
            return;
        }

        Student.AddCoursesResult result = student.addCoursesWithResult(
            selected.toArray(Course[]::new)
        );
        statusLabel.setText(messageFor(result));
        if (result == Student.AddCoursesResult.ADDED) {
            Term selectedTerm = (Term) termCombo.getSelectedItem();
            if (selectedTerm != null) {
                studentTerms.put(student.getId(), selectedTerm.code());
            }
            refreshRegisteredCourses();
        }
    }

    /** Drops the selected sections from the student's registered schedule. */
    private void dropSelectedCourses() {
        List<Course> selected = registeredList.getSelectedValuesList();
        if (selected.isEmpty()) {
            statusLabel.setText("Select at least one registered course to drop.");
            return;
        }

        student.dropCourses(selected.toArray(Course[]::new));
        refreshRegisteredCourses();
        statusLabel.setText("Selected course(s) dropped.");
    }

    /** Drops every registered section when the schedule is nonempty. */
    private void dropAllCourses() {
        if (registeredCourses.isEmpty()) {
            statusLabel.setText("There are no registered courses to drop.");
            return;
        }
        student.dropAllCourses();
        refreshRegisteredCourses();
        statusLabel.setText("All courses dropped.");
    }

    /** Refreshes the registered-section list, credit total, and term control. */
    private void refreshRegisteredCourses() {
        registeredCourses.clear();
        for (Course course : student.getCourses()) {
            registeredCourses.addElement(course);
        }
        registeredCreditsLabel.setText(
            student.getRegisteredCredits().stripTrailingZeros().toPlainString()
                + " / " + Student.MAX_CREDITS + " credits"
        );
        termCombo.setEnabled(student.getCourses().isEmpty() && termsLoaded);
    }

    /**
     * Updates the enabled state of search, add, and term controls.
     *
     * @param busy whether a search is currently running
     */
    private void setSearchBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        subjectField.setEnabled(!busy);
        addButton.setEnabled(!busy);
        backButton.setEnabled(!busy);
        termCombo.setEnabled(
            !busy && termsLoaded && student != null
                && student.getCourses().isEmpty()
        );
    }

    /**
     * Shows an error dialog and mirrors its message in the status area.
     *
     * @param title dialog and status title
     * @param error cause to display
     */
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

    /**
     * Converts a registration result into user-facing status text.
     *
     * @param result outcome returned by the student registration operation
     * @return status message for the outcome
     */
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

    /**
     * Creates a multi-select course list with the application's course renderer.
     *
     * @param model list model containing the course sections
     * @return configured course list
     */
    private JList<Course> createCourseList(DefaultListModel<Course> model) {
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
                label.setText(courseDescription(
                    (Course) value,
                    (Term) termCombo.getSelectedItem()
                ));
                label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                return label;
            }
        });
        return list;
    }

    /**
     * Builds the HTML summary displayed for a course section.
     *
     * @param course course section to describe
     * @param term selected term, or {@code null} if unavailable
     * @return escaped, user-facing course summary
     */
    private static String courseDescription(Course course, Term term) {
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
        String instructorDescription = instructorNames(course);
        String availability = Boolean.TRUE.equals(course.openSection())
            ? "Open"
            : "Closed / full";
        String termDescription = term == null
            ? "Term unavailable"
            : term.description();
        return "<html><b>" + escapeHtml(course.toString()) + "</b> &mdash; "
            + escapeHtml(title) + "<br>"
            + escapeHtml(termDescription) + " | "
            + escapeHtml(course.creditsDescription()) + " | "
            + escapeHtml(meetingDescription) + " | "
            + escapeHtml(instructorDescription) + " | "
            + availability + "</html>";
    }

    /**
     * Collects distinct instructor names from section- and meeting-level data.
     *
     * @param course section whose instructor data is displayed
     * @return instructor label, or {@code Instructor TBA} when none are present
     */
    private static String instructorNames(Course course) {
        List<String> instructors = new ArrayList<>();
        if (course.faculty() != null) {
            course.faculty().stream()
                .filter(faculty -> faculty != null)
                .map(Course.Faculty::displayName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(instructors::add);
        }
        if (course.meetingsFaculty() != null) {
            course.meetingsFaculty().stream()
                .filter(meeting -> meeting != null && meeting.faculty() != null)
                .flatMap(meeting -> meeting.faculty().stream())
                .filter(faculty -> faculty != null)
                .map(Course.Faculty::displayName)
                .filter(name -> name != null && !name.isBlank())
                .forEach(instructors::add);
        }
        List<String> uniqueInstructors = instructors.stream().distinct().toList();
        return uniqueInstructors.isEmpty()
            ? "Instructor TBA"
            : "Instructor: " + String.join(", ", uniqueInstructors);
    }

    /**
     * Escapes characters that have special meaning in an HTML label.
     *
     * @param value text to escape
     * @return HTML-safe text
     */
    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /**
     * Loads and validates the university logo resource.
     *
     * @return loaded logo icon
     * @throws IllegalStateException if the resource is missing or unreadable
     */
    private static ImageIcon loadLogoIcon() {
        URL logoUrl = MainMenu.class.getResource("/images/ecsu-logo.png");
        if (logoUrl == null) {
            throw new IllegalStateException(
                "Missing GUI logo resource: /images/ecsu-logo.png"
            );
        }
        ImageIcon icon = new ImageIcon(logoUrl);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            throw new IllegalStateException("Could not load GUI logo image.");
        }
        return icon;
    }

    /**
     * Creates a proportionally scaled logo with an accessible name.
     *
     * @param maxWidth maximum rendered width in pixels
     * @param maxHeight maximum rendered height in pixels
     * @return label containing the scaled logo
     */
    private JLabel createLogoLabel(int maxWidth, int maxHeight) {
        double scale = Math.min(
            (double) maxWidth / logoIcon.getIconWidth(),
            (double) maxHeight / logoIcon.getIconHeight()
        );
        Image scaledLogo = logoIcon.getImage().getScaledInstance(
            (int) Math.round(logoIcon.getIconWidth() * scale),
            (int) Math.round(logoIcon.getIconHeight() * scale),
            Image.SCALE_SMOOTH
        );
        JLabel logo = new JLabel(new ImageIcon(scaledLogo));
        logo.getAccessibleContext().setAccessibleName(
            "Eastern Connecticut State University logo"
        );
        return logo;
    }

    /**
     * Filters course sections using the criteria shown in the GUI.
     *
     * @param courses sections to filter
     * @param courseNumber course number to match, or blank for any number
     * @param openOnly whether to exclude closed sections
     * @param instructor instructor-name substring to match, or blank for any
     * @param excludedDay weekday to exclude, or blank or unrecognized for no
     *        day restriction
     * @return the matching course sections in their original order
     */
    private static List<Course> filterCourses(
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

    /**
     * Checks section-level and meeting-level faculty for a matching substring.
     *
     * @param course section to inspect
     * @param instructorFilter lowercase instructor-name substring
     * @return whether any faculty name contains the filter
     */
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

    /**
     * Checks a faculty list for a matching display-name substring.
     *
     * @param faculty faculty list, possibly {@code null}
     * @param instructorFilter lowercase instructor-name substring
     * @return whether any display name contains the filter
     */
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

    /**
     * Checks whether a section has a meeting on the specified day code.
     *
     * @param course section to inspect
     * @param dayCode standard single-letter weekday code
     * @return whether any meeting occurs on that day
     */
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

    /**
     * Normalizes supported weekday names and abbreviations to standard codes.
     *
     * @param day day name or abbreviation
     * @return standard day code, or {@code null} for blank or unrecognized input
     */
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
