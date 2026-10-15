# Eastern Registration Java Project

## Student registration

`com.example.Student` represents a student by ID, first name, and last name.
Students can register for courses up to `Student.MAX_CREDITS` total credits
with `addCourse` or `addCourses`. For variable-credit courses the upper credit
value is counted toward the limit. A registration is rejected if a section is
not open, duplicates an existing registration, exceeds the credit limit,
overlaps an existing meeting time, or has unknown credit hours. `addCourses` is
atomic: it adds none of the supplied courses if any one cannot be registered.

Use `dropCourse` or `dropCourses` to remove specific sections, and
`dropAllCourses` to clear the student's schedule. `getCourses` returns an
immutable snapshot of the current registrations.

The desktop app opens a graphical student setup screen. Enter the student's
ID and name, then search by subject to find course sections. Search results can
be filtered by course number, open status, instructor, or an excluded weekday
(Monday through Friday; **None** is the default).
Select one or more open sections to register; select registered sections to
drop them, or use **Drop All** to clear the schedule. The registered-course
panel shows the student's current credit total against the limit. Registration
limits, closed sections, duplicate registrations, unknown credits, and time
conflicts are reported in the status area.

`EasternRegistration` launches the graphical `MainMenu`; there is no separate
console menu.
