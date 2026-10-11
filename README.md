# Threat Trace

Threat Trace is a local, synthetic-data cyber-forensics workbench. It reconstructs a timeline, compares two cases with an explainable five-part Attack DNA score, models a hypothetical asset graph, reviews competing hypotheses, and prepares a redacted Evidence Passport that must pass a review state before JSON export.

The Java application owns workflow and persistence. Python receives minimized behavioral features for comparison. MySQL stores cases, source events, hypotheses, comparisons, simulations, packages, approvals, and audit records. All example incidents and outputs are synthetic; no real system is scanned or contained.

## Requirements

- Windows PowerShell 5.1 or newer
- Java 17 or newer
- The checked-in Maven Wrapper scripts (`mvnw.cmd` and `.mvn/wrapper/maven-wrapper.properties`); no global Maven installation is required
- MySQL Server 8.0.16 or newer, installed and running locally
- MySQL command-line client (`mysql.exe`), normally installed with MySQL Server
- Python 3.10 or newer
- Node.js 18 or newer (only needed for the JavaScript syntax check)

Docker and Docker Compose are not used by the startup script or required to run the application.

## One-time local MySQL setup

1. Start the **MySQL80** (or equivalent) Windows service from Services or MySQL Installer. Confirm the local server accepts TCP connections on port `3306`.
2. Connect to that local server as an administrator in MySQL Workbench or with `mysql.exe` and create the project schema and application account. Choose a strong local password and use the same value in `.env`:

```sql
CREATE DATABASE IF NOT EXISTS threattrace
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'threattrace'@'localhost'
  IDENTIFIED BY 'choose-a-local-password';
GRANT ALL PRIVILEGES ON threattrace.* TO 'threattrace'@'localhost';
```

If the MySQL account already exists with another password, update it with `ALTER USER ... IDENTIFIED BY 'choose-a-local-password';`. The account needs privileges on the `threattrace` database to create tables, views, triggers, and stored procedures. If `MYSQL_HOST` is set to `127.0.0.1` or another host, make the MySQL account host part match that connection.

### MySQL binary logging and trigger creation

With binary logging enabled and `log_bin_trust_function_creators=0`, MySQL rejects creation of triggers unless the server configuration trusts trigger creators. In this schema, line 27 creates `trg_event_parent_insert_guard` (a trigger, not a stored function); subsequent trigger declarations need the same setting. A trigger has no `DETERMINISTIC` declaration to add, so the schema cannot resolve this restriction by changing the trigger declaration.

As a MySQL administrator (for example, in MySQL Workbench connected as your local server administrator), enable the setting once:

```sql
SET PERSIST log_bin_trust_function_creators = 1;
SELECT @@GLOBAL.log_bin_trust_function_creators;
```

The query should return `1`. `SET PERSIST` requires the administrator account to have `SYSTEM_VARIABLES_ADMIN` (or the deprecated `SUPER`) privilege. This is a server-level setting and permits users who already have the relevant schema-scoped routine/trigger creation privileges to create those objects; it does **not** grant `SUPER` to the Threat Trace account. The setting relaxes MySQL's binary-log safety checks for trigger and stored-function creators, so use it only for a local development instance you control, not a production or replication source without reviewing the replication implications. MySQL documents this behavior in [Stored Program Binary Logging](https://dev.mysql.com/doc/refman/8.0/en/stored-programs-logging.html) and [System Variable Privileges](https://dev.mysql.com/doc/refman/8.0/en/system-variable-privileges.html).

If you want the change to last only until the next MySQL restart, an administrator can use `SET GLOBAL log_bin_trust_function_creators = 1;` instead; repeat it after each restart before initializing Threat Trace. If you cannot use `SET PERSIST`, add `log_bin_trust_function_creators=1` under `[mysqld]` in the MySQL Server `my.ini` file and restart the MySQL Windows service. Do not grant global `SUPER` to the application account.

3. In the project folder, create `.env` and set the local connection values:

```powershell
Copy-Item .env.example .env
notepad .env
```

Set at least `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE=threattrace`, `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_CLIENT`. `MYSQL_CLIENT` can be `mysql` if `mysql.exe` is on `PATH`, or its full path, for example `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`.

## Start the application

From the project directory, run:

```powershell
./scripts/start.ps1
```

The script reads `.env`, connects to the already-running local MySQL Server, applies `database/schema.sql` and `database/seed.sql`, starts the local Python analytics worker, and runs the Java application using `mvnw.cmd`. The wrapper downloads Maven 3.9.9 into your user Maven cache on its first invocation, so the first run needs internet access; later runs use that cached copy. No global Maven installation or PATH entry is needed. Open [http://localhost:8080](http://localhost:8080).

The schema and seed scripts are safe to re-run. Tables use `CREATE TABLE IF NOT EXISTS`; views use `CREATE OR REPLACE VIEW`; triggers and stored procedures are dropped and recreated by name; seed rows use duplicate-key updates. Re-running preserves investigation and workflow data while ensuring the demo seed rows and programmable objects exist. It does not delete tables or reset `AUTO_INCREMENT` values. `CREATE TABLE IF NOT EXISTS` is not a schema migration mechanism: if a later code version changes an existing table definition, apply a reviewed migration before starting that version.

The script requires the MySQL client to connect with the application account. It reports a clear error if the server is stopped, the account cannot connect, or the account lacks the required schema privileges. Start/stop the local MySQL Server through Windows Services or MySQL Installer; Threat Trace does not control the server service.

## Database initialization manually

If you prefer to initialize separately, make sure the database and account above exist, then open PowerShell in the project directory. Enter the application password for the current PowerShell process and apply both SQL files:

```powershell
$env:MYSQL_PWD = 'your-local-password'
Get-Content -Raw database/schema.sql | mysql --protocol=TCP --host=localhost --port=3306 --user=threattrace --database=threattrace
Get-Content -Raw database/seed.sql | mysql --protocol=TCP --host=localhost --port=3306 --user=threattrace --database=threattrace
Remove-Item Env:\MYSQL_PWD
```

The startup script performs the same steps using values from `.env`. The SQL files intentionally target the fixed `threattrace` database, so `MYSQL_DATABASE` must remain `threattrace`.

## JDBC configuration

The Spring Boot datasource in `src/main/resources/application.properties` uses:

```text
jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:threattrace}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Those placeholders match the `.env` values loaded by `scripts/start.ps1` before the wrapper starts Java. The script also uses the same host, port, database, and user for `mysql.exe`; it validates the database name against the SQL scripts. The password is passed to the MySQL client through the process environment and to Spring Boot as `MYSQL_PASSWORD`. MySQL Connector/J is declared in `pom.xml`.

| Variable | Default | Purpose |
|---|---|---|
| `MYSQL_HOST` | `localhost` | Local MySQL Server host |
| `MYSQL_PORT` | `3306` | Local MySQL Server TCP port |
| `MYSQL_DATABASE` | `threattrace` | Fixed schema used by SQL and JDBC |
| `MYSQL_USER` | `threattrace` | Application/schema-initialization account |
| `MYSQL_PASSWORD` | `threattrace-local` | Local account password; change it before running |
| `MYSQL_CLIENT` | `mysql` | `mysql.exe` command or full path |
| `APP_PORT` | `8080` | Spring Boot web port |
| `ANALYTICS_URL` | `http://localhost:8100` | Local minimized-feature worker URL |
| `THREATTRACE_PSEUDONYM_KEY` | Demo fallback | HMAC key for stable preview pseudonyms; use a long random local value |

## Manual application run

With the schema already initialized and `.env` configured, use three PowerShell terminals from the project directory. Set the required environment values in the Java terminal; PowerShell does not automatically load `.env` outside `scripts/start.ps1`.

```powershell
python analytics/service.py
```

```powershell
$env:MYSQL_HOST='localhost'; $env:MYSQL_PORT='3306'; $env:MYSQL_DATABASE='threattrace'; $env:MYSQL_USER='threattrace'; $env:MYSQL_PASSWORD='your-local-password'; $env:APP_PORT='8080'; $env:ANALYTICS_URL='http://localhost:8100'; .\mvnw.cmd spring-boot:run
```

The Python worker listens only on `127.0.0.1:8100`.

## Test and verify

```powershell
.\mvnw.cmd test
python -m unittest discover -s analytics -p "test*.py"
node --check src/main/resources/static/app.js
```

The wrapper uses the Maven version in `.mvn/wrapper/maven-wrapper.properties` (currently 3.9.9). Check it with `.\mvnw.cmd --version` from PowerShell.

Manual end-to-end check in the browser:

1. Confirm the case selector shows two records labelled synthetic.
2. In Timeline, change both filters, select an event, and move Burst focus. Marker positions use elapsed time; long quiet periods are visible as long gaps.
3. In Attack DNA, compare the other case, open each “Why similar?” detail, and confirm evidence IDs, differences, and telemetry caveats are visible.
4. In Digital Twin, run each hypothetical action and inspect its assumptions.
5. In Evidence Court, open the evidence list and compare supporting and contradictory links.
6. In Evidence Passport, select evidence, create a draft, inspect the pseudonymized preview, submit it, approve it in the demo reviewer control, then export the JSON and inspect its SHA-256 field.
7. Confirm `vw_audit_history` shows the package workflow and a released package has a manifest hash.

Comparisons and simulations are persisted with method/model versions and relevant inputs. MySQL stores source event references. Python receives minimized categorical features and relative intervals; Java attaches exact event timestamps to the explanation response after analysis. Event descriptions and absolute timestamps are not sent to the Python worker.

## Troubleshooting

- **`mysql.exe` not found:** add its `bin` folder to `PATH` or set `MYSQL_CLIENT` in `.env` to the full executable path.
- **Java not found or too old:** install a JDK 17 or newer and ensure `java.exe` is on `PATH`. Maven is downloaded and run by `mvnw.cmd`; it does not need a global install.
- **First wrapper run cannot download Maven:** confirm internet access to Maven Central, then rerun `./scripts/start.ps1` or `.\mvnw.cmd --version`. The distribution is cached under your user Maven wrapper directory.
- **Connection refused or timeout:** start the local MySQL Server Windows service and check `MYSQL_HOST` and `MYSQL_PORT`.
- **Access denied / insufficient privileges:** verify the username/password and that the account host matches the TCP host. Grant the account privileges on `threattrace.*` so it can create tables, views, triggers, and procedures.
- **Java connects to a different database:** confirm `.env` has `MYSQL_DATABASE=threattrace`; this value matches both the SQL scripts and JDBC URL.
- **Port 8080 or 8100 is busy:** change `APP_PORT`, or change the worker port in `analytics/service.py` and set `ANALYTICS_URL` to match.
- **Analytics unavailable:** ensure the Python service is running and `ANALYTICS_URL` matches it.
- **Schema object creation fails:** check MySQL Server version and the account's `CREATE`, `CREATE VIEW`, `TRIGGER`, and `CREATE ROUTINE` privileges on `threattrace`.
- **`ERROR 1419` while creating a trigger:** use the local MySQL administrator to enable `log_bin_trust_function_creators` as described above, verify it is `1`, then rerun startup. Do not grant `SUPER` to the `threattrace` account.
- **Export unavailable:** the package must contain at least one event, be submitted, and have an approval recorded. Export is only available in `RELEASED` status.

## Scope and safety limits

This is an academic demonstration with synthetic cases. It does not authenticate users, enforce organization roles, transmit to external recipients, encrypt transfers, provide a legal authorization check, or guarantee anonymity. The reviewer and recipient fields are demonstration labels, not verified identities. Do not enter real logs or personal data into the current demo. The digital twin is hypothetical, not a prediction of real containment outcomes. No C++ component is included because there is no measured performance need.
