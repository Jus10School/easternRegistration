# Eastern Registration Java Project

## Student registration

`com.example.Student` represents a student by ID, first name, and last name.
Students can register for up to `Student.MAX_CREDITS` course sections with
`addCourse` or `addCourses`. A registration is rejected if a section is not
open, duplicates an existing registration, exceeds the limit, or overlaps an
existing meeting time. `addCourses` is atomic: it adds none of the supplied
courses if any one cannot be registered.

Use `dropCourse` or `dropCourses` to remove specific sections, and
`dropAllCourses` to clear the student's schedule. `getCourses` returns an
immutable snapshot of the current registrations.

From the console menu, choose **3. Manage student registration** to create a
student for the current session and add, drop, or view course sections. Enter
section numbers as displayed; multiple selections can be comma-separated.
