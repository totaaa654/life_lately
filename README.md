# Life Lately

Life Lately is a desktop journal made with JavaFX and MySQL. It lets users write entries, track moods, browse past entries, and view simple journal statistics.

Life Lately is currently in development. User data stays in the MySQL database configured for the application and is not sent to an external service.

Repository: [github.com/totaaa654/life_lately](https://github.com/totaaa654/life_lately)

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

## Setup and run

The steps below use Windows and Laragon, which is the setup used during development.

### 1. Get the project

Clone the repository:

```shell
git clone https://github.com/totaaa654/life_lately.git
cd life_lately
```

You can also use **Code > Download ZIP** on GitHub, extract the ZIP, and open a terminal inside the extracted folder.

### 2. Install the requirements

Install:

- JDK 21
- Maven
- Laragon with MySQL, or another MySQL 8 installation

Check that Java and Maven are available:

```shell
java -version
mvn -version
```

Both commands should finish successfully. The Java version should be 21.

### 3. Start MySQL

1. Open Laragon.
2. Start MySQL or click **Start All**.
3. Leave Laragon running while using the app.

The MySQL account must be allowed to create databases and tables. A default Laragon installation normally uses the `root` account with no password.

### 4. Create the local configuration

In PowerShell, run this from the project folder:

```powershell
Copy-Item config.properties.example config.properties
```

You can also copy the example file manually and rename the copy to `config.properties`.

The default file contains:

```properties
db.url=jdbc:mysql://localhost:3306/life_lately?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000
db.username=root
db.password=
```

Change the port, username, or password if your MySQL setup is different. `config.properties` is ignored by Git, so local database credentials are not committed.

### 5. Run the application

```shell
mvn clean javafx:run
```

Maven downloads the required dependencies during the first run, so it may take longer than later launches.

The app automatically creates the `life_lately` database, tables, moods, and default settings. You do not need to import the SQL files manually.

### 6. Create an account

Create a local account when the login window opens. Accounts have separate journal entries and settings. A successful login is remembered until the user logs out.

## Common setup problems

### MySQL is not available

- Confirm that Laragon and MySQL are running.
- Check the port, username, and password in `config.properties`.
- Restart the application after starting MySQL.

### `java` or `mvn` is not recognized

- Install JDK 21 and Maven.
- Add their `bin` folders to the Windows `PATH`.
- Open a new terminal and run `java -version` and `mvn -version` again.

### Manual database import

Manual import is normally unnecessary. If needed, import these files in order:

1. `src/main/resources/com/lifelately/database/schema.sql`
2. `src/main/resources/com/lifelately/database/seed.sql`

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

Life Lately is currently in development, so bugs and incomplete features may still be present. Information entered into the app is provided voluntarily and stored in the configured MySQL database. Keep your own backups and avoid storing sensitive information.
