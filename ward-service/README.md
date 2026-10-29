# WardServiceApp

## Overview

Provides lists of wards and departments.

Part of the [HealthSafe](../README.md) project. Independent Maven module, no
parent pom.

MQ: this service subscribes to the ActiveMQ topic `staffing-events-topic` — see [`../common/`](../common) — and publishes to the ActiveMQ queue `equipment-failure-queue` when it detects an equipment failure on one of its wards, consumed by [`../equipment-alert-service`](../equipment-alert-service). Broker URL, topic name, and queue name come from the common `co.wethinkcode.healthsafe.mq.MqConfig` class alongside it in this module.

REST: called by `staffing-service` (`../staffing-service`) and `alert-level-service` (`../alert-level-service`) — see [Integration contracts](../README.md#integration-contracts) in the root README for the endpoint shapes.

## What it does now

REST: only `GET /health` so far. The ward and department endpoints are not built yet.

Messaging: two classes in the `mq` package, tested but not yet wired into `WardServiceApp`:

- `EquipmentFailureProducer` sends a JSON alert (ward, equipment type, reason, timestamp) to the queue `equipment-failure-queue`. Messages are sent as persistent.
- `StaffingEventConsumer` listens to the topic `staffing-events-topic` and passes each text message to a callback.

## Decisions and trade-offs

- A queue for equipment failures and a topic for staffing events: a failure must reach one handler and survive until it is handled, while staffing updates are broadcast to anyone listening.
- The tests run against a real broker, and each uses a uniquely named queue or topic, so runs do not interfere with each other.
- The alert JSON is built with string formatting, so a quote in the reason text would break it. A JSON library would fix that.
- The topic consumer is not durable, so it misses messages sent while the service is down.
- Not done: the REST endpoints, calling the producer when a failure is detected, and starting the consumer with the service.

## Project structure

    ward-service/
    ├── pom.xml
    └── src
        ├── main/java/co/wethinkcode/healthsafe/
        │   ├── WardServiceApp.java
        │   └── mq/
        │       ├── MqConfig.java                  broker URL, topic and queue names
        │       ├── EquipmentFailureProducer.java  sends to the queue
        │       └── StaffingEventConsumer.java     listens to the topic
        └── test/java/co/wethinkcode/healthsafe/mq/
            ├── EquipmentFailureProducerTest.java
            ├── EquipmentFailureQueueIntegrationTest.java
            └── StaffingEventConsumerTest.java

## Build

```
mvn package
```

## Run

```
java -jar target/ward-service.jar
```

Listens on port `7031`.

## Test

The tests talk to a real ActiveMQ broker, so start it first:

    cd ../common && docker compose up -d
    mvn test

3 tests: a failure alert reaches the queue, the queue drains after the message is consumed, and a topic message reaches the listener.
