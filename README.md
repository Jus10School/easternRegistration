# Eastern Registration

A Java desktop app for searching Eastern course sections and managing a student schedule.

## Requirements

- Java 17 or later
- Maven

## Build

From the project root:

```bash
mvn -f demo/pom.xml package
```

## Run

From the `demo` directory:

```bash
mvn exec:java -Dexec.mainClass=com.example.EasternRegistration
```

Course data is read from the `cache` directory when available. The app can fetch data from Eastern when it is not cached.

Students can create an account or log in using their student ID, first name,
and last name. Surrounding whitespace is trimmed, and the resulting values
must match the account exactly. Accounts and registered schedules are kept in
memory only and are cleared when the app closes. Returning to the account
screen does not discard a schedule; logging in again restores the registered
courses and the last term saved when a course was successfully added. This
demo checks the entered ID and name but does not provide password-based or
secure authentication.
