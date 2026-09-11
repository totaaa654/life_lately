# Life Lately

Life Lately is a cozy, single-user desktop journal for capturing everyday moments without turning
journaling into a chore. It uses one JavaFX window, a calm bear companion, quick mood check-ins,
searchable entries, tags, calendar browsing, lightweight insights, and persistent appearance settings.

## Phase 1 features

- One-stage JavaFX shell with Home, Journal, Calendar, Insights, and Settings navigation
- Create, read, edit, search, favorite, and soft-delete journal entries
- Five seeded moods: Great, Good, Okay, Low, and Rough
- Comma-separated tags that are reused automatically
- Search plus mood, tag, favorite, date, and sort filters
- Monthly calendar with entry indicators and entries for the selected day
- Total/monthly entry counts, streaks, mood counts, and popular tags
- Light and dark themes with five accent choices
- Database-backed theme, accent, date-format, and first-day-of-week settings
- Friendly offline state when Laragon/MySQL is not running
- Replaceable mascot and illustration asset locations

## Technologies

- Java 21
- JavaFX 21 (Controls and FXML)
- Maven
- JDBC with MySQL Connector/J
- MySQL 8 / Laragon
- FXML for layouts and modular CSS for styling
- JUnit 5 for the Phase 1 integration and FXML smoke checks

## Architecture

The application uses a deliberately small layered architecture:

```text
FXML + CSS -> Controller -> Service -> DAO -> MySQL
                         ↘ Model ↗
```

- `model` contains application data only.
- `dao` owns SQL and JDBC work.
- `service` owns validation, saving rules, tag resolution, and insight calculations.
- `controller` translates JavaFX events and fields into service calls.
- `navigation` loads page FXML into the center of the one primary Stage.
- `theme` applies shared stylesheets and theme/accent classes.
- `database` creates the database and runs the schema/seed resources.
- `config` loads configuration and wires the application services together.

## Folder structure

```text
src/main/java/com/lifelately/
├── App.java
├── config/        # application wiring and database properties
├── controller/    # small JavaFX page/component controllers
├── dao/           # all SQL queries and row mapping
├── database/      # connections and repeatable initialization
├── model/         # Entry, Mood, Tag, AppSettings
├── navigation/    # single-stage page switching
├── service/       # validation and journal business rules
├── theme/         # theme and accent application
└── util/          # focused date and validation helpers

src/main/resources/com/lifelately/
├── css/
│   ├── base.css
│   ├── light-theme.css
│   ├── dark-theme.css
│   ├── components/
│   └── pages/
├── database/      # schema.sql and seed.sql
├── fxml/          # main shell, pages, and reusable components
├── fonts/
├── icons/
└── images/
```

## Database setup with Laragon

1. Open Laragon and start **MySQL**.
2. Confirm the MySQL user you plan to use can create databases and tables.
3. Copy `config.properties.example` to `config.properties` in the project root.
4. Update the username, password, port, or URL if your Laragon setup differs.

Default local configuration:

```properties
db.url=jdbc:mysql://localhost:3306/life_lately?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=3000
db.username=root
db.password=
```

On startup, Life Lately creates the `life_lately` database when necessary, applies
`schema.sql`, and idempotently seeds moods and default settings. The schema uses InnoDB,
`utf8mb4`, primary and foreign keys, useful indexes, timestamps, and `deleted_at` soft deletion.

`config.properties` is ignored by Git. Do not put real credentials in the example file.

## Run the app

From the project directory:

```shell
mvn clean javafx:run
```

If MySQL is stopped, the interface still opens and explains how to reconnect. Start MySQL and restart
the application to enable data features.

## Verify Phase 1

With Laragon MySQL running:

```shell
mvn clean test
```

The integration test initializes the schema, exercises create/read/search/update/soft-delete/restore,
and removes its temporary verification data. The JavaFX smoke test loads every page and reusable FXML
resource so broken controller bindings or malformed layouts fail the build.

## Where do I edit things?

| What you want to change | File or folder |
|---|---|
| Home layout | `src/main/resources/com/lifelately/fxml/home/home.fxml` |
| Home styling | `src/main/resources/com/lifelately/css/pages/home.css` |
| Journal layouts | `src/main/resources/com/lifelately/fxml/journal/` |
| Journal styling | `src/main/resources/com/lifelately/css/pages/journal.css` |
| Reusable buttons, cards, and forms | `src/main/resources/com/lifelately/css/components/` |
| Entry database queries | `src/main/java/com/lifelately/dao/EntryDAO.java` |
| Entry validation and logic | `src/main/java/com/lifelately/service/EntryService.java` |
| Entry editor UI behavior | `src/main/java/com/lifelately/controller/journal/EntryEditorController.java` |
| Models and stored fields | `src/main/java/com/lifelately/model/` |
| Database tables or seed values | `src/main/resources/com/lifelately/database/` |
| Navigation destinations | `src/main/java/com/lifelately/navigation/NavigationManager.java` and `View.java` |
| Shared theme colors | `src/main/resources/com/lifelately/css/light-theme.css` and `dark-theme.css` |
| Theme application behavior | `src/main/java/com/lifelately/theme/ThemeManager.java` |
| Bear mascot files | `src/main/resources/com/lifelately/images/bear/` |
| Application wiring | `src/main/java/com/lifelately/config/AppConfig.java` |

## Notes for extending the app

- Keep SQL in DAO classes and business rules in services.
- Keep controllers focused on reading controls, calling services, and updating the visible state.
- Add page-specific CSS to `css/pages` and reusable control styling to `css/components`.
- Add new pages to `View` and load them through `NavigationManager`; do not create a Stage per page.
- Soft-deleted entries remain in MySQL and can already be restored through `EntryService.restore`.
  A future Trash screen can expose that operation in the interface.
