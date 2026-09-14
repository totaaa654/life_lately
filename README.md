# Life Lately

Life Lately is a desktop journal made with JavaFX and MySQL. It lets users write entries, track moods, browse past entries, and view simple journal statistics.

This application was created as a school project. User data stays in the MySQL database configured for the application and is not sent to an external service.

## Features

- Local account creation, login, logout, and remembered sessions
- Separate journal entries and settings for each account
- Create, view, edit, search, favorite, and delete journal entries
- Mood selection using Great, Good, Okay, Low, or Rough
- Tags, date filters, mood filters, and sorting
- Monthly calendar with mood indicators and entries for each day
- Basic insights such as entry totals, streaks, common moods, and popular tags
- Light, dark, and system themes with multiple accent colors
- Saved theme, date format, and first-day-of-week preferences
- Display-name and password changes from Settings
- Password hashing using PBKDF2-HMAC-SHA256 with a random salt

## Tech stack

- Java 21
- JavaFX 21 with FXML and CSS
- Maven
- MySQL 8 / Laragon
- JDBC and MySQL Connector/J
- JUnit 5

## How it works

The interface is defined using FXML and CSS. Controllers handle user actions, services contain validation and application logic, and DAO classes read and write data through JDBC.

```text
FXML and CSS -> Controller -> Service -> DAO -> MySQL
```

The application creates the `life_lately` database and its tables on startup if they do not exist. Default moods and settings are also added automatically.

## Requirements

Install the following before running the project:

- JDK 21
- Maven
- MySQL 8, or Laragon with MySQL enabled

Check the installed versions:

```shell
java -version
mvn -version
```

## Database setup

1. Start MySQL in Laragon or through your local MySQL installation.
2. Copy `config.properties.example` to `config.properties` in the project root.
3. Update the database connection if your MySQL username, password, or port is different.

Default Laragon configuration:

```properties
db.url=jdbc:mysql://localhost:3306/life_lately?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000
db.username=root
db.password=
```

`config.properties` is ignored by Git so local database credentials are not committed.

The app normally handles database setup by itself. The SQL files are available here if manual import is needed:

- `src/main/resources/com/lifelately/database/schema.sql`
- `src/main/resources/com/lifelately/database/seed.sql`

## Run the application

Open a terminal in the project folder and run:

```shell
mvn clean javafx:run
```

Create an account on the first launch. Later sessions will reopen the remembered account until the user logs out.

If the app cannot connect, make sure MySQL is running and that the values in `config.properties` are correct.

## Run the tests

Keep MySQL running, then run:

```shell
mvn clean test
```

The tests check the database operations, account separation, settings, authentication changes, and JavaFX/FXML loading. Temporary test records are removed after the test run.

## Project structure

```text
src/main/java/com/lifelately/
  config/       Application and database configuration
  controller/   JavaFX controllers
  dao/          SQL queries and database mapping
  database/     Database connection and initialization
  model/        Data models
  navigation/   Page navigation
  service/      Validation and application logic
  theme/        Theme handling
  util/         Shared utilities

src/main/resources/com/lifelately/
  css/          Application styles
  database/     Schema and seed SQL
  fxml/         Page and component layouts
  images/       Icons and illustrations
```

## Project note

Life Lately is intended for educational and demonstration use. Information entered into the app is provided voluntarily and stored in the configured MySQL database. The app is not a medical service or a guaranteed backup system, so sensitive information should not be stored in it.
