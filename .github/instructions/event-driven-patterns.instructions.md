---
name: 'Event Driven Patterns'
description: 'Event publishing via Debezium outbox pattern, Kafka integration, Avro schema evolution, and idempotent event consumption'
applyTo: '**/handler/**/*.java, **/domain/eventoutbox/EventOutbox.java, **/*EventListener.java'
---

# Event-Driven Architecture Patterns

> **Based on Actual Implementation**: These patterns reflect established conventions for Spring Boot microservices. Examples use this repository's structure for illustration.

## PII Protection

All PII protection rules from `@.github/instructions/pii-protection.instructions.md` apply. Never log customer names, account numbers, tokens, or credentials. Only log UUIDs, correlation IDs, status codes, and system-generated metadata.

This instruction file defines how the service publishes and consumes domain events using the Debezium Outbox pattern, Kafka, Avro schemas, and CQRS projections.

## Core Principles

1. Atomic persistence + publish: domain state and outbox entry are committed in the same DB transaction (Outbox pattern).
2. Idempotent consumption: handlers tolerate replay and at-least-once delivery; no duplicate side-effects.
3. Backward-compatible Avro evolution (additive only; never break existing consumers).
4. Deterministic ordering per aggregate using stable partition key (`accountId` or `transactionId`).
5. Observability: structured envelope logging (eventType, aggregateId, eventId, partition, offset) – avoid PII.
6. Feature flags: experimental events emitted only when corresponding flag enabled.
7. Time-ordered IDs: use UUID v7 (`GUID.v7().toUUID()`) for eventId and aggregate IDs to aid ordering & keyset scans.
8. Separation of concerns: business mutation first, event persisted; Kafka publication delegated to Debezium connector.

## Outbox Publishing Pattern

```java
@Service
public class AccountLifecycleService {
  private final AccountRepository accountRepository;
  private final EventOutboxRepository outboxRepository;

  @Transactional(rollbackFor = Exception.class)
  public void activate(Account account) {
    account.activate();
    accountRepository.save(account);

    AccountActivated payload = AccountActivated.from(account);
    EventOutbox outbox = EventOutbox.builder()
        .id(GUID.v7().toUUID())              // outbox row ID
        .aggregateType("Account")
        .aggregateId(account.getId())
        .eventType("AccountActivated")
        .partitionKey(account.getId()) // ordering by account
        .occurredAt(Instant.now())
        .payload(avroSerializer.serialize(payload))
        .traceId(Tracing.currentTraceId())
        .build();
    outboxRepository.save(outbox); // Debezium picks up & publishes asynchronously
  }
}
```

Guidelines:

- No external HTTP/Kafka calls inside the transactional method; keep it lean.
- Never publish directly to Kafka in a domain transaction.
- Use `rollbackFor = Exception.class` to ensure outbox & state atomicity.

## Event Envelope (Recommended Fields)

| Field          | Purpose                                                               |
| -------------- | --------------------------------------------------------------------- |
| eventId        | UUID v7 unique identifier per event occurrence                        |
| aggregateType  | Domain aggregate name (`Account`, `Transaction`)                      |
| aggregateId    | UUID v7 of the aggregate instance                                     |
| partitionKey   | Key used for Kafka partitioning (usually `accountId` or `transactionId`) |
| eventType      | Semantic past-tense name (e.g. `AccountActivated`)                    |
| occurredAt     | UTC timestamp at commit time                                          |
| payload        | Avro serialized domain data                                           |
| traceId/spanId | OpenTelemetry tracing IDs (minimal)                                   |
| metadata       | Optional map for correlating workflows / feature flag context         |

Keep envelope minimal; avoid large metadata blobs and secrets.

## Feature Flags & Event Emission

Wrap emission logic for experimental features:

```java
if (!featureFlags.isAccountReplacementEnabled()) return; // suppress event
publishOutbox(eventOutboxRepository, aggregateId, eventType, payload);
```

- Capture flag state at workflow start; avoid mid-sequence toggling.
- Never emit disabled feature events (noise & unclear contract).

## Avro Schema & Manifest Management

Schemas live under `schemas/<domain>/<version>`; versions enumerated in `schemas/kafka-apis-manifest.yaml` with `since` markers.

Evolution Rules:
| Change | Allowed | Strategy |
|--------|---------|----------|
| Add optional field | Yes | Provide default / union with null |
| Add required field | Discouraged | Introduce optional then enforce logically |
| Remove field | No (until deprecated) | Mark deprecated; remove after ≥1 release cycle |
| Rename field | No | Add new field, deprecate old |

New event workflow:

1. Create Avro schema in next version folder.
2. Update manifest `publishes` with `name` + `since` version.
3. Regenerate Avro classes (Maven Avro plugin runs at `generate-sources`).
4. Add producer unit test (serialization).
5. Add consumer compatibility test (old schema reading new event).

## Partition Key Selection

| Key           | Use Case                                                |
| ------------- | ------------------------------------------------------- |
| accountId     | Ordering of account-specific lifecycle and setting updates |
| transactionId | Transaction-level transitions linked to an account         |
| customerId    | Customer-centric projections (less frequent)            |

Once chosen for an event type, do NOT change partition key without migrating to a new topic.

## Kafka Topic Naming

Topics follow `<Domain>.<EventType>` convention (e.g. `Deposit.Account`, `Deposit.AccountCreated`). Test configuration (`KafkaTestConfig`) compacts topics (cleanup.policy=compact). Avoid renaming existing production topics; add new ones with manifest updates.

## Consumer Pattern (Idempotent)

### File Location

- **All Kafka event listeners must be placed in the `/handler/` package** (e.g., `src/main/java/com/examples/deposit/handler/`)
- File naming convention: `*EventListener.java` (e.g., `CreditLineAccountClosedEventListener.java`, `AccountTransactionEventListener.java`)
- Use `@Component` and `@Profile("eventhandler")` annotations
- This ensures consistent organization and makes event handlers easy to locate

```java
@Component
@Profile("eventhandler")
public class AccountEventListener {
  private final AccountProjectionService projectionService;
  private final ProcessedEventStore processedStore; // track processed eventIds

  @KafkaListener(topics = "Deposit.Account", groupId = "account-projection")
  public void onMessage(AccountEventEnvelope envelope) {
    if (processedStore.isProcessed(envelope.getEventId())) return; // idempotency guard
    try {
      projectionService.apply(envelope);
      processedStore.markProcessed(envelope.getEventId());
    } catch (TransientDependencyException e) {
      throw e; // trigger retry
    } catch (Exception e) {
      throw new FatalConsumerException(e); // send to DLQ via error handler
    }
  }
}
```

Guidelines:

- Idempotency must not rely solely on Kafka offset.
- Keep handler fast; delegate heavy logic.
- Use Micrometer counters: success, retry, dlq.

## Error Handling & Dead Letter Queue (DLQ)

| Scenario                          | Handling                           |
| --------------------------------- | ---------------------------------- |
| Deserialization failure           | Direct DLQ (cannot parse)          |
| Transient external fail           | Exponential backoff retry then DLQ |
| Permanent business rule violation | DLQ immediately with cause         |

DLQ record content: `eventId`, `eventType`, `aggregateId`, partition, offset, errorClass, message.

## Projection / Read Model Updates

- Only upsert changed fields; avoid full rewrites.
- Use separate projection tables for frequent read endpoints (e.g. active accounts per customer).
- Denormalize responsibly; ensure replay builds the same state.

### Replay / Backfill Procedure

1. Create new projection table or truncate existing.
2. Consume from earliest offset of relevant topics.
3. Apply events idempotently; ignore duplicates.
4. Validate final row counts vs aggregate sources.

(DB keyset pagination for large maintenance tasks is covered in `database-jpa-patterns.instructions.md` and is distinct from event replay.)

## Testing Strategy

| Layer                | Example                                              |
| -------------------- | ---------------------------------------------------- |
| Unit (producer)      | Mapping aggregate → event payload correctness        |
| Integration (outbox) | Insert + Debezium publication (Testcontainers Kafka) |
| Consumer             | Idempotent projection update & DLQ path              |
| Schema compatibility | Old consumer reads new event (Avro)                  |
| Feature flag         | Event not emitted when flag disabled                 |

Spock snippet:

```groovy
when: 'account activated'
service.activate(account)
then: 'AccountActivated outbox row exists'
outboxRepository.findLatestByAggregateId(account.id).eventType == 'AccountActivated'
```

## Observability

- Log INFO: `eventType`, `aggregateId`, `eventId`, partition, offset.
- Structured logging (logstash encoder); payload only at DEBUG.
- Metrics: `domain_events_published_total`, `domain_events_consumed_total`, `domain_events_dlq_total`, consumer lag dashboard.
- Tracing: propagate minimal context (traceId/spanId) – avoid large baggage.

## Security & PII

- No full names, account numbers, or sensitive personal data in events.
- Allowed: surrogate IDs and minimal non-PII references if strictly required & approved.
- Payload encryption not used; rely on topic ACLs and network security.

## Feature Flag Checklist for New Event

- [ ] Flag key added to configuration (`deposit.feature.<slug>.enabled=false`).
- [ ] Emission conditional on flag.
- [ ] QA environments enable flag progressively.
- [ ] Monitoring alerts configured before prod enable.

## Anti-Patterns & Corrections

| Anti-Pattern                          | Correction                                     |
| ------------------------------------- | ---------------------------------------------- |
| Direct Kafka publish in transaction   | Use outbox row; Debezium connector publishes   |
| Non-idempotent consumer side-effects  | Track processed IDs / design pure upsert logic |
| Abrupt Avro field removal             | Deprecate & wait one release cycle             |
| Partition key inconsistency           | Standardize per event type & document          |
| Emitting events for failed operations | Emit only after successful commit              |
| Large metadata maps bloating envelope | Keep only essential correlation/tracing fields |

## Evolution Checklist

- [ ] Meaningful past-tense event name
- [ ] Schema added + manifest updated (`kafka-apis-manifest.yaml`)
- [ ] Backward compatibility validated
- [ ] Partition key selected & documented
- [ ] Feature flag (if experimental) integrated
- [ ] Producer & consumer tests added
- [ ] Metrics & logging updated

## References

- `schemas/kafka-apis-manifest.yaml` (version & publishes list)
- Outbox starter: `spring-boot-starter-embedded-debezium-kafka-outbox`
- UUID v7 generation: `com.github.f4b6a3.uuid.alt.GUID.v7()` — see `@.github/instructions/database-jpa-patterns.instructions.md` for detailed UUID v7 patterns and keyset pagination
- Test topics & compaction: `KafkaTestConfig` & `KafkaTestService`
- Database & JPA patterns (keyset pagination vs replay)
- Swagger domain language (keep event types consistent with API resource names)
