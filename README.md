WTC-8RMW37GX
# HealthSafe

## Overview

Hospital ward status and emergency staffing schedules.

Domain entities: wards, wings, specialist departments.

Every class in this repo lives in a single flat package, `co.wethinkcode.healthsafe`. HealthSafe is built
as a small set of independent services, following a growth path from simple data
cleanup through synchronous REST calls to asynchronous MQ decoupling and alerting:

1. clean a messy legacy CSV export (`wards-outdated.csv`) — handled by **IngestionServiceApp**
2. serve it up and act on it, via three REST services calling each other directly
   over HTTP
3. decouple the relevant services with an ActiveMQ topic (`staffing-events-topic`) instead of
   direct calls — shared broker setup lives in [`common/`](common)
4. raise the alarm on failure — handled by **EquipmentAlertServiceApp**

| Service | Folder | Port | Role |
|---|---|---|---|
| IngestionServiceApp | [`ingestion-service/`](ingestion-service) | 7030 | Parses and cleans `wards-outdated.csv` |
| WardServiceApp | [`ward-service/`](ward-service) | 7031 | Provides lists of wards and departments. |
| AlertLevelServiceApp | [`alert-level-service/`](alert-level-service) | 7032 | Tracks the hospital Emergency Status (0-8, 8 = full Code Blue). |
| StaffingServiceApp | [`staffing-service/`](staffing-service) | 7033 | Provides on-call schedules for doctors based on ward and status. |
| EquipmentAlertServiceApp | [`equipment-alert-service/`](equipment-alert-service) | 7034 | uses a Queue to guarantee delivery of critical medical equipment failure alerts. |

Plus [`common/`](common) (no port) — the shared ActiveMQ broker and MQ config notes
for `staffing-events-topic`: Staffing updates are broadcast as Events via the broker to decouple the frontend from the Staffing Service.

**Status:** partial. Stage 1 (ingestion) is done and tested. The message queue code exists in ward-service and is tested against a real broker, but it is not wired into the running services yet. The REST services and the equipment-alert consumer are still scaffolds.

| Stage | State |
|---|---|
| 1. Ingestion (clean the CSV, serve it over REST) | Done, 5 unit tests |
| 2. REST services (ward, alert-level, staffing) | Not started, still scaffolds |
| 3. Topic (`staffing-events-topic`) | Consumer written and tested in ward-service; staffing-service does not publish yet |
| 4. Queue (`equipment-failure-queue`) | Producer written and tested in ward-service; equipment-alert-service does not consume yet |

## Your task

Each stage below builds on the last — do them in order. Every service already builds
and runs (`/health` returns `OK`); your job is to fill in the `TODO`s.

1. **Ingestion** (required) — in `IngestionServiceApp`, read and clean
   `wards-outdated.csv` (see [ingestion-service/README.md](ingestion-service/README.md)
   for the known data issues and a worked example) and expose the cleaned records
   over REST for `ward-service` to consume.
2. **REST services** (required) — implement `ward-service`, `alert-level-service`,
   and `staffing-service` per the [Integration contracts](#integration-contracts)
   below: wards/departments lookup, Emergency Status tracking, and on-call
   scheduling that calls the other two services synchronously over HTTP.
3. **MQ decoupling** (stretch) — replace the synchronous call from `ward-service`
   to `staffing-service` with the `staffing-events-topic` broadcast described in
   [common/README.md](common/README.md), so staffing updates reach `ward-service`
   asynchronously instead.
4. **Alerting** (stretch) — have `ward-service` publish to the
   `equipment-failure-queue` when it detects an equipment failure, and implement
   `equipment-alert-service` as the guaranteed-delivery consumer (see
   [equipment-alert-service/README.md](equipment-alert-service/README.md)).

Stage 1-2 are the required core; stages 3-4 are where you can show judgment about
when to reach for a queue/topic instead of a direct call. There's no fixed time
limit, but budget your effort so you have a working stage 1-2 before spending time
on 3-4 — a complete core beats a half-done everything.

Automated tests aren't required, but are a good way to show your work — see each
service's `## Test

Each module has its own tests. Run them from inside the module folder, because there is no parent pom:

    cd ingestion-service && mvn test      # 5 tests, no broker needed
    cd ward-service && mvn test           # 3 tests, need the broker running

The ward-service tests talk to a real ActiveMQ broker, so start it first:

    cd common && docker compose up -d

Each test creates its own uniquely named queue or topic, so runs do not interfere with each other. The destinations show up in the ActiveMQ console at http://localhost:8161 (admin / admin).
