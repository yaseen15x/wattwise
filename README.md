# WattWise

[![WattWise CI](https://github.com/yaseen15x/wattwise/actions/workflows/ci.yml/badge.svg)](https://github.com/yaseen15x/wattwise/actions/workflows/ci.yml)

A home energy simulator that calculates appliance running costs and compares when to run them under different electricity tariffs.

Built with Java, Spring Boot, React, TypeScript and PostgreSQL.

## What it does

- Save, edit and delete appliances with their power ratings.
- Calculate energy consumption in kWh and estimated cost in GBP.
- Calculate household totals with a per-appliance breakdown through the API.
- Calculate costs for runs that span different tariff periods.
- Recommend a cheaper start time within an allowed scheduling window.
- Visualise tariff periods and compare original and recommended schedules.
- Store appliances in PostgreSQL.

Electricity rates are entered manually. WattWise does not fetch live prices or control appliances.

## Example

A 2,000 W heater running for one hour uses 2 kWh.

With a tariff of £0.30/kWh before 07:00 and £0.10/kWh afterwards:

| Schedule | Running time | Estimated cost |
|---|---|---|
| Original | 06:00–07:00 | £0.60 |
| Recommended | 07:00–08:00 | £0.20 |

For an allowed window of 06:00–10:00, the recommended run saves £0.40 in this example while using the same amount of energy.

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring Web MVC |
| Validation | Jakarta Bean Validation |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL 17 |
| Database migrations | Flyway |
| Frontend | React, TypeScript, Vite |
| Testing | JUnit, MockMvc, Testcontainers |
| Containers | Docker Compose, Nginx |
| Continuous integration | GitHub Actions |

## Architecture

The browser loads the React application from Nginx. Requests to `/api/` are forwarded to the Spring Boot backend, which reads and writes appliance records in PostgreSQL.

The backend separates HTTP controllers, request and response DTOs, calculation services and persistence. Flyway manages the database schema, and Hibernate validates the entity mappings against it.

## Run with Docker

### Requirements

- Git
- Docker with Docker Compose

Java and Node.js do not need to be installed locally for this option.

### 1. Clone the repository

```bash
git clone https://github.com/yaseen15x/wattwise.git
cd wattwise
```

### 2. Configure the database password

Create a file named `.env` in the project root:

```dotenv
POSTGRES_PASSWORD=replace_with_your_own_password
```

The `.env` file is ignored by Git. Do not commit real credentials.

### 3. Build and start the application

```bash
docker compose up --build -d
```

The first build downloads dependencies and container images, so it may take a few minutes.

Open:

http://localhost:3000

### 4. Check the services

```bash
docker compose ps
```

View backend logs:

```bash
docker compose logs -f backend
```

### Stop the application

```bash
docker compose down
```

Appliance data is retained in the PostgreSQL named volume. Running `docker compose down -v` deletes that volume and its data.

The database password is set when the volume is first initialised. Changing `.env` afterwards does not automatically change the password stored in PostgreSQL.

## Local development

### Requirements

- Java 21 or later
- Node.js 22.14 or later within the Node 22 release line
- Docker with Docker Compose

Create the root `.env` file as described above.

If the full Docker application is already running, stop it before starting the development services:

```bash
docker compose down
```

### Start PostgreSQL

From the project root:

```bash
docker compose up -d db
```

PostgreSQL is available locally on port `5433`.

### Start the backend

In a separate terminal:

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

The backend runs at http://localhost:8080.

When launched from the backend directory, it reads the root `.env` file.

### Start the frontend

In another terminal, starting from the project root:

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173.

The Vite development server proxies `/api/` requests to the backend.

## Tests and checks

### Backend

With Docker running, execute from the backend directory:

```bash
./mvnw verify
```

The test suite covers:

- Energy and cost calculations.
- Household totals and rounding.
- Time-based tariff calculations.
- Scheduling window constraints.
- Schedule optimisation.
- Application startup.
- Saved appliance API behaviour.

Integration tests use Testcontainers to start a temporary PostgreSQL database. They do not use the application's normal database.

### Frontend

From the frontend directory:

```bash
npm ci
npm run lint
npm run build
```

These commands check lint rules, TypeScript compilation and the production build. They are not browser interaction tests.

### Continuous integration

GitHub Actions runs backend tests and packaging, plus frontend linting and the production build, on pushes and pull requests to `main`.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/hello` | Basic application response |
| POST | `/api/energy/calculate` | Calculate energy and cost |
| POST | `/api/household/calculate` | Calculate a household breakdown |
| POST | `/api/energy/calculate-scheduled` | Calculate a run across tariff periods |
| POST | `/api/energy/optimise` | Recommend a start time |
| GET | `/api/appliances` | List saved appliances |
| POST | `/api/appliances` | Create an appliance |
| GET | `/api/appliances/{id}` | Retrieve an appliance |
| PUT | `/api/appliances/{id}` | Update an appliance |
| DELETE | `/api/appliances/{id}` | Delete an appliance |
| POST | `/api/appliances/{id}/calculate` | Calculate a saved appliance's running cost |

### Calculate energy and cost

With the Docker application running:

```bash
curl -X POST http://localhost:3000/api/energy/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "powerWatts": 2000,
    "durationHours": 3,
    "rateGbpPerKwh": 0.25
  }'
```

Example response:

```json
{
  "energyKwh": 6,
  "costGbp": 1.50
}
```

### Optimise a schedule

```bash
curl -X POST http://localhost:3000/api/energy/optimise \
  -H "Content-Type: application/json" \
  -d '{
    "schedule": {
      "name": "Heater",
      "powerWatts": 2000,
      "startMinute": 360,
      "durationMinutes": 60,
      "tariffs": [
        {
          "startMinute": 0,
          "endMinute": 420,
          "rateGbpPerKwh": 0.30
        },
        {
          "startMinute": 420,
          "endMinute": 1440,
          "rateGbpPerKwh": 0.10
        }
      ]
    },
    "earliestStartMinute": 360,
    "latestFinishMinute": 600
  }'
```

Example response:

```json
{
  "name": "Heater",
  "originalStartMinute": 360,
  "recommendedStartMinute": 420,
  "durationMinutes": 60,
  "originalMeetsWindow": true,
  "originalCostGbp": 0.60,
  "recommendedCostGbp": 0.20,
  "estimatedSavingsGbp": 0.40
}
```

Times in the API are minutes after midnight: `360` means 06:00, `420` means 07:00 and `1440` means midnight at the end of the day.

For local development, use port `8080` instead of `3000` to call the backend directly.

## Calculation and optimisation approach

Energy consumption is calculated as:

```text
energyKwh = powerWatts × durationHours ÷ 1000
```

For scheduled runs, the calculator applies the relevant tariff to each minute of usage. Tariff periods must cover the full day without gaps or overlaps.

The optimiser compares candidate starts every 15 minutes from the earliest allowed start. It also considers the original start if it fits within the window.

Candidate costs are compared before rounding to avoid choosing between schedules using rounded penny values. Equal-cost candidates favour the earlier start.

Money calculations use Java's `BigDecimal`, with displayed costs rounded to two decimal places using `HALF_UP`.

## Assumptions and limitations

- Power consumption is assumed to remain constant throughout a run. Real appliances may cycle or vary their power draw.
- Estimates exclude standing charges.
- Rates are supplied by the user and are not live electricity prices.
- Optimisation searches the supported candidate starts, not every possible minute.
- Optimisation windows must fall within one day.
- The optimiser handles one appliance at a time.
- The frontend supports two tariff periods; the scheduled calculation API accepts a list of periods.
- Appliance records are saved, but calculation results and tariff scenarios are not persisted.
- Authentication and per-user data isolation are not implemented. The current application is intended for local use and demonstration.

## Project structure

```text
wattwise/
├── .github/workflows/     # Continuous integration
├── backend/
│   ├── src/main/java/    # Controllers, DTOs, models and services
│   ├── src/main/resources/
│   │   └── db/migration/ # Flyway migrations
│   ├── src/test/java/    # Unit and integration tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/             # React components and styles
│   ├── public/          # Static assets
│   ├── Dockerfile
│   └── nginx.conf
└── compose.yaml
```

## Author

[Yaseen Khan](https://github.com/yaseen15x)