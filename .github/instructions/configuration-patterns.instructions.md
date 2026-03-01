---
name: 'Configuration Patterns'
description: 'Spring configuration management, profile usage patterns, property binding, and feature flag implementation'
applyTo: '**/config/**/*.java, **/application*.properties, **/application*.yml'
---
# Configuration and Profile Management

> **Based on Actual Implementation**: Configuration patterns from the Card Service. See `src/main/resources/application.properties` for actual configuration and `src/main/java/com/ytl/card/config/` for configuration classes.

## Core Principles

- **Externalize all environment-specific values** — Use `${ENV_VAR:default}` pattern; never hardcode secrets or credentials
- **Gate features with flags defaulting to disabled** — Always set `havingValue` explicitly; cover both true and false branches
- **Scope configuration by domain** — Use short, domain-prefixed property names (e.g., `card.limit.*`, `external.e6.*`)
- **Isolate profiles by concern** — Use `@Profile` annotations to activate beans only in appropriate runtime contexts
- **Remove stale flags** — Delete both conditional bean methods, property definitions, and references when a feature matures

## Spring Profiles

### Active Profiles in Card Service
- `application-self-service`: Public/customer-facing REST APIs (Swagger enabled when flag true)
- `application-internal`: Internal/admin & operational APIs (Swagger enabled when flag true)
- `episode6`: External provider integration specific adjustments
- `hitrust`: Adds Hitrust-specific signing/validation properties
- `debezium`: Activates embedded Debezium outbox health contributors
- `taskrunner`: Activates task execution components / cron overrides
- `test`: Unit/integration test scope (avoid remote calls, use mocks)

### Profile Usage Patterns
- Multi-profile activation: `@Profile({"application-self-service", "application-internal"})` (see `SwaggerOpenApiConfig`)
- Exclude test-only: `@Profile("!test")` for config beans that must not be loaded during tests (e.g. external clients, OSS credentials). Note: cron tasks have different profile requirements — see `@.github/instructions/cron-task-patterns.instructions.md`
- Per-profile property activation: use `spring.config.activate.on-profile` sections in `application.properties` for block overrides
- Use environment variable expansion with defaults: `${VAR_NAME:default}` for resilience

### Example Profile Block (from properties)
```properties
#---
spring.config.activate.on-profile = application-internal
springdoc.swagger-ui.enabled = ${SWAGGER_UI_ENABLED:false}
```

## Configuration Properties Binding

### Binding Strategy
- Use `@ConfigurationProperties(prefix = "kafka")` for grouped external system settings (e.g. Kafka producers/consumers)
- Keep prefix short & domain-scoped (`card.limit.*`, `external.e6.*`, `paynet.*`, `visa.settlement.*`)
- Prefer one config class per logical domain; avoid massive catch-all classes

### Example
```java
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    private Map<String, String> producers;
    private Map<String, String> consumers;
}
```

## Property Naming Conventions
- Lowercase, dot-separated: `card.limit.spending.upper-limit`
- Boolean flags end with `.enabled`: `multi.card.replacement.enabled`
- Use kebab-case for compound segments; avoid camelCase
- Include units or context: `.timeout-ms`, `.period.days`, `.upper-limit`
- For list values use comma-separated (e.g. `card.forex.special.currency = AUD,EUR,GBP,NZD`)
- Problem type or domain constants may use uppercase env variables when directly injected (e.g. `ALIBABA_CLOUD_ROLE_ARN` via `@ConditionalOnProperty`)

## Externalized Configuration Patterns
- Centrally define defaults in root `application.properties`
- Override per environment via deployed profile property file or environment variables
- Secrets: import from secrets manager via `spring.config.import=optional:aliyun-secretsmanager:...`
- Keep secret placeholders empty by default (e.g. `${ALIYUN_OSS_ACCESS_KEY_ID}`) to force explicit provisioning

## Feature Flags

### Purpose
Used to gate non-ready or gradually released functionality without requiring code redeploy when toggling in different environments.

### Lifecycle
1. Introduce flag with default disabled (false) in `application.properties`
2. Enable in lower environments (dev/staging) by setting environment variable or profile-specific property block
3. Monitor metrics & logs; if stable, enable in production
4. Remove flag & conditional code after feature matures (avoid permanent flag proliferation)

### Naming
- Prefix by domain: `card.limit.20k.enabled`, `card.visa.clearing-refund.e6.retry.enabled`
- Use concise descriptive nouns/verbs; avoid ambiguous names (e.g. prefer `card.rpl.fund.option.enabled` over `fund.option`)
- Related flags share prefix for grouping in monitoring dashboards

### Implementation Patterns

#### Dual Bean Strategy
Used when feature introduces alternative implementation (e.g. physical card replacement workflow):
```java
@Bean
@ConditionalOnProperty(value = "multi.card.replacement.enabled", havingValue = "true")
PhysicalCardReplacementWorkflow newPhysicalCardReplacementWorkflow(
        CardService cardService, NotificationService notificationService) {
    return new MultiCardReplacementWorkflow(cardService, notificationService);
}

@Bean
@ConditionalOnProperty(value = "multi.card.replacement.enabled", havingValue = "false")
PhysicalCardReplacementWorkflow oldPhysicalCardReplacementWorkflow(
        CardService cardService, NotificationService notificationService) {
    return new SingleCardReplacementWorkflow(cardService, notificationService);
}
```
Guidelines:
- Always cover both true & false branches to avoid missing bean
- Keep method names distinct & reflective
- Inject dependencies identically for parity

#### Configuration Variant
Switching configuration values only:
```java
@Bean
@ConditionalOnProperty(value = "card.limit.20k.enabled", havingValue = "true")
DefaultConfigurableSpendingLimit highLimit(@Value("${card.limit.20k.upper-limit}") BigDecimal up,
                                           @Value("${card.limit.spending.lower-limit}") BigDecimal low) {
    return new DefaultConfigurableSpendingLimit(up, low);
}

@Bean
@ConditionalOnProperty(value = "card.limit.20k.enabled", havingValue = "false")
DefaultConfigurableSpendingLimit normalLimit(@Value("${card.limit.spending.upper-limit}") BigDecimal up,
                                             @Value("${card.limit.spending.lower-limit}") BigDecimal low) {
    return new DefaultConfigurableSpendingLimit(up, low);
}
```

#### Mock vs Real External Clients
```java
@Bean
@ConditionalOnProperty(value = "mock.payment.service.enabled", havingValue = "false")
PaymentClient paymentClient(RestClient restClient, @Value("${payment.api.url}") String url, ObjectMapper om) {
    return new PaymentClientImpl(restClient, url, om);
}

@Bean
@ConditionalOnProperty(value = "mock.payment.service.enabled", havingValue = "true")
PaymentClient mockPaymentClient(TransferProjectionRepository repo) { return new MockPaymentClient(repo); }
```
Guidelines:
- Flag defaults to false (use real client)
- Mock path minimal: no network calls
- Avoid mixing `mock.*` flags with unrelated domain prefixes

### Conditional Annotation Tips
- Use `havingValue = "true"` / `"false"` explicitly (do not rely on `matchIfMissing` for clarity)
- For existence-based conditions (secrets/credentials), use `@ConditionalOnProperty("ALIBABA_CLOUD_ROLE_ARN")`
- For fallback bean creation use `@ConditionalOnMissingBean`

### Validation
- Document each flag (description, impact, owner) in README or central feature flag registry (future improvement)
- Add metrics & logs keyed by flag state to confirm behavior

## REST Client & Service Discovery

### Discovery Toggling
```java
@Bean
@LoadBalanced
@ConditionalOnProperty(value = "spring.cloud.discovery.enabled", havingValue = "true")
RestClient.Builder restClientBuilder() { return RestClient.builder(); }
```
- Provide non-load balanced variant without condition
- Preserve uniform encoding strategy (`DefaultUriBuilderFactory` with `TEMPLATE_AND_VALUES`)
- Use connection pooling with tuned timeouts (see `RestClientConfig`)

### Timeouts
- Property naming: `spring.cloud.discovery.rest-template.connect-timeout-ms`, `read-timeout-ms`
- Keep values small for internal services (e.g. 2500ms) & tune based on latency metrics

## ObjectMapper Configuration
- Central `ObjectMapper` bean with JSR-310 module and disabled timestamp serialization
- Set `FAIL_ON_UNKNOWN_PROPERTIES = false` for forward compatibility of external payloads
- Register modules (JDK8 types, Kotlin) only if needed

## Swagger / OpenAPI Activation
- Enabled only in API-serving profiles: `application-self-service`, `application-internal`
- Controlled by `springdoc.swagger-ui.enabled` flag inside each profile block
- Add global header parameters via `OperationCustomizer` (e.g. `x-source`, `x-scopes`)

## OSS / Credentials Strategy
- Conditional OIDC credentials provider when `ALIBABA_CLOUD_ROLE_ARN` present
- Fallback static credentials with `@ConditionalOnMissingBean`
- Avoid loading secrets for test profile (`@Profile("!test")`)

## Observability & Metrics
- Use consistent metric tags: `application`, `ryt.common.metric.source`
- Percentile histograms enabled globally (`management.metrics.distribution.percentiles-histogram.all = true`)
- Set SLO buckets via `management.metrics.distribution.slo.all`

## Database & Pooling
- Hikari properties: set explicit connection, idle, max pool size via env defaults
- Disable Open Session in View: `spring.jpa.open-in-view=false`
- Slow query logging threshold: `hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS = 100`
- Keep batch properties commented until required to avoid premature optimization

## Kafka & Debezium
- Separate consumer & schema registry property groups under `kafka.*`
- Debezium outbox properties use `embedded-debezium-kafka-outbox.*` prefix for clarity
- Enable outbox only with flag: `embedded-debezium-kafka-outbox.enabled=false` by default

## Fraud & Settlement Configuration
- Group MyDebit/SAN settlement & fraud properties with prefixes (`my.debit.*`, `san.*`, `visa.settlement.*`)
- Threshold values include units context (e.g. `two.calendar.days.transaction.volume.threshold`)

## Testing & Mocks
- Use `@Profile("!test")` to exclude real external clients
- Provide mock implementations behind `mock.*.enabled` flags when test profile not active but environment demands simulation
- For integration tests prefer testcontainers over embedded DB with consistent env variable reuse

## Review Checklist (Before Merge)
1. New feature flags follow naming & default disabled pattern
2. All `@ConditionalOnProperty` beans have both branches or acceptable fallback
3. No stray obsolete flags (plan removal stories for stale flags)
4. Profile blocks in properties have clear `#---` separators
5. Secrets referenced only via environment variable placeholders (no hard-coded secrets)
6. ObjectMapper not re-defined in multiple config classes
7. Discovery conditional aligns with `spring.cloud.discovery.enabled` usages

## Migration Guidance
- When removing a flag: delete both conditional bean methods, property definition, and references in README/registry
- For renaming a flag: deprecate old name (keep for one release cycle) with log warning on startup

## Example Feature Flag Definition in application.properties
```properties
# Card Feature Flag
card.visa.clearing-refund.e6.retry.enabled = ${CARD_VISA_CLEARING_REFUND_E6_RETRY_ENABLED:false}
```
Pattern: `<domain>.<capability>[.<sub-capability>].enabled = ${ENV_VAR:false}`

## Anti-Patterns to Avoid
- Using `matchIfMissing=true` to implicitly enable features
- Mixing unrelated domain flags under one prefix
- Hardcoding credentials in properties
- Large monolithic configuration classes with unrelated properties
- Unused commented flags lingering indefinitely

## References

- `@ARCHITECTURE.md` - System architecture and domain model
- `@.github/instructions/java-spring-coding-standards.instructions.md` - Java 21 coding standards and Spring Boot patterns
- `@.github/instructions/pii-protection.instructions.md` - PII protection guidelines
- `@.github/instructions/cron-task-patterns.instructions.md` - Scheduled task patterns and profile requirements
