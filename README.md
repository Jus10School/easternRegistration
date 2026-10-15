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
