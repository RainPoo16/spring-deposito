---
name: 'Java Spring Coding Standards'
description: 'Java 21 coding standards, Spring Boot patterns, dependency injection, error handling, and logging best practices'
applyTo: '**/*.java'
---
# Java and Spring Boot Coding Standards

> **Based on Actual Implementation**: These coding standards reflect patterns used throughout the Card Service codebase in `src/main/java/com/ytl/card/`. All patterns are verified against actual implementation.

## Core Principles

1. **Constructor injection everywhere**: Use `@RequiredArgsConstructor` with `private final` fields — never use `@Autowired`.
2. **Domain logic in entities, orchestration in services**: Keep business rules in domain objects; services coordinate cross-entity operations and infrastructure concerns.
3. **UUID v7 for all entity IDs**: Use `GUID.v7().toUUID()` — never `UUID.randomUUID()`.
4. **Never log PII**: Log only record IDs (UUIDs), error codes, counts, and timestamps. See `pii-protection.instructions.md`.
5. **Explicit transaction boundaries**: Always use `@Transactional(rollbackFor = Exception.class)` on mutating service methods.

## General Java Standards

### Code Organization
- Use Java 21 features where appropriate (pattern matching, records, etc.)
- Follow standard Java package naming: `com.ytl.card.<layer>`
- Organize code in layers: `controller`, `service`, `repository`, `domain`, `config`, `mapper`, `exception`, `handler`, `external`
- Use `domain` package for JPA entities (not `model`)
- Place DTOs in `controller.<feature>.dto` packages
- Use `projection` package for read-only query projections
- Use `handler` package for event listeners and domain event handlers
- Place external service clients in `external` package with sub-packages per service
- Use proper access modifiers - use `private` for internal members
- Use `final` for fields that won't be reassigned

### Naming Conventions
- Use PascalCase for classes and interfaces
- Use camelCase for methods and variables
- Use UPPER_SNAKE_CASE for constants
- Prefix private fields with underscore only if using MapStruct conflicts
- Use descriptive names that reflect business domain

### Error Handling
- Use custom exception classes extending appropriate base exceptions (e.g., `Exception` for checked exceptions)
- Place custom exceptions in the `exception` package
- Use simple constructors accepting message: `public CustomException(String message)`
- Implement global exception handling using `ExceptionHandlingAdvice` as base class for controller exception handling
- Define response DTOs in `controller.exception` package (e.g., `InternalServerExceptionResp`)
- Use `PROBLEM_DETAILS_HEADER` with `MediaType.APPLICATION_PROBLEM_JSON_VALUE`
- Always include meaningful error messages and context
- Return appropriate HTTP status codes with `ResponseEntity`
- Log errors at appropriate level using SLF4J (use `logger.atError()` for structured logging)
- Throw checked exceptions for business-level errors (e.g., `CardIsLockedException`)
- Throw `IllegalStateException` for programming errors or unexpected states
- **NEVER log PII** — see `@.github/instructions/pii-protection.instructions.md` for comprehensive guidelines. Log only record IDs (UUIDs), error codes, counts, and timestamps.

## Spring Boot Patterns

### Dependency Injection
- Use constructor injection (no `@Autowired` on constructors)
- Implement constructor injection with `@RequiredArgsConstructor` from Lombok
- Use immutable service classes with final fields (`private final`)
- Use `@Qualifier` when multiple beans of the same type exist

```java
// ✅ Correct: constructor injection via @RequiredArgsConstructor + private final fields
@Service
@RequiredArgsConstructor
public class CardDesignService {
    private final CardDesignRepository cardDesignRepository;
    private final CardProfileRepository cardProfileRepository;
    private final CardOrderRepository cardOrderRepository;
}
// Reference: src/main/java/com/ytl/card/service/CardDesignService.java
```

### Configuration
- Use `@Configuration` classes in `config` package
- Enable features declaratively: `@EnableScheduling`, `@EnableCaching`
- Use `@ConfigurationProperties` for external configuration
- Use `@Profile` annotations for environment-specific configurations
- Use `@ConditionalOnProperty` for feature toggles
- Use constructor injection in configuration classes
- Implement `@EventListener(ApplicationReadyEvent.class)` for initialization logic

### Service Layer
- Mark service classes with `@Service`
- Use `@Transactional(rollbackFor = Exception.class)` on methods that modify data
- Use `@Transactional(propagation = Propagation.REQUIRES_NEW)` when a new transaction is explicitly needed
- Use `EntityManager.refresh()` after `saveAndFlush()` when you need database-generated values immediately
- Keep business logic in the service layer, not controllers
- Keep domain logic in domain entities; use domain services for cross-entity operations
- **Logging in Services**: Never log PII or sensitive data (see Error Handling section for details)

### Repository Layer
- Extend `CrudRepository` or `JpaRepository` based on needs
- Use `JpaSpecificationExecutor` for dynamic queries
- Implement custom specifications in separate classes
- Use `@Query` for complex queries when specifications aren't sufficient
- Use method name conventions for query derivation (e.g., `findByCardId`, `findByCardTransactionIdAndEndToEndId`)
- Return `Optional<T>` for single result queries that may not exist
- Use `List<T>` for multi-result queries
- Keep repositories as interfaces without implementation classes

### Domain Entities
- Place all JPA entities in the `domain` package
- Use `@Entity` for JPA entities with proper table mapping
- Extend `AbstractAggregateRoot<T>` for aggregate roots that publish domain events
- Use `@EntityListeners` for entity lifecycle events (e.g., `FactEventEntityListener.class`)
- Always include `@Version` field for optimistic locking
- Use UUID as primary key type (`@Id private UUID id`)
- Use `GUID.v7()` from `com.github.f4b6a3.uuid.alt.GUID` for generating time-ordered UUIDs (better for database indexing)
- Never use `UUID.randomUUID()` for entity IDs
- Include `createdAt` and `updatedAt` timestamp fields
- Use `@Enumerated(EnumType.STRING)` for enum fields
- Implement proper `equals()` and `hashCode()` for entities
- Implement static factory methods (e.g., `create()`) instead of public constructors with parameters
- Keep constructors package-private or protected
- Use `@Getter` sparingly on entities; use explicit getter methods for better encapsulation

### DTOs and Value Objects
- Use record classes for DTOs and value objects when possible
- Separate domain models from DTOs
- Place DTOs in `controller.<feature>.dto` packages
- Define DTOs specific to external services in service sub-packages (e.g., `external.dto`)
- Keep external service contracts separate from internal domain models

### Mappers
- Use MapStruct for DTO-to-domain and domain-to-DTO conversions
- Place mappers in the `mapper` package
- Use `@Mapper` annotation with `uses` parameter for shared mapping utilities
- Define mapper as interface with `INSTANCE` constant: `Mappers.getMapper(MapperClass.class)`
- Use `@Mapping` annotations for non-standard field mappings
- Create a `CommonMapper` utility for shared conversion logic
- Place domain-specific mappers in `domain.mapper` sub-package

## Validation
- Use Bean Validation annotations (`@Valid`, `@NotNull`, `@NotBlank`, etc.)
- Create custom validators for complex business rules
- Validate at the controller boundary with `@Valid`
- Use meaningful validation messages

## Documentation
- Document public APIs with proper JavaDoc
- Include parameter descriptions and return value explanations
- Document any complex business logic or algorithms
- Use clear and concise comments for non-obvious code

## Event Handling
- Implement entity listeners in `handler` package
- Use `@EntityListeners` annotation on entities
- Place event-related domain logic in handlers (e.g., `FactEventEntityListener`)
- Implement workflow handlers for complex business processes

## External Service Integration
- Create service interfaces in `external` package
- Organize by service name (e.g., `external.martech`, `external.authentication`)
- Define DTOs specific to external services in service sub-packages
- Keep external service contracts separate from internal domain models

## Async Processing
- Use Quartz for scheduled jobs
- Implement proper job data serialization
- Use `@Profile` to enable schedulers in specific environments
- Handle job failures gracefully with retry mechanisms

## Constants and Enums
- Define business enums in `domain.constant` package
- Use meaningful enum names reflecting business concepts
- Store enum values as strings in database using `@Enumerated(EnumType.STRING)`

## Testing
- Use Spock framework (Groovy) for test specifications
- Organize test classes mirroring source structure
- Use testcontainers for integration tests requiring infrastructure

## Common Pitfalls

| Pitfall | Correction |
|---------|------------|
| Using `UUID.randomUUID()` for entity IDs | Always use `GUID.v7().toUUID()` for time-ordered UUIDs |
| Missing `@Transactional(rollbackFor = Exception.class)` on mutating methods | Always specify `rollbackFor` — default only rolls back unchecked exceptions |
| Logging entity objects directly (PII exposure via `toString()`) | Log only UUIDs, error codes, and counts — never entity or DTO objects |

## Code Style
- Use Spotless for code formatting (configured in pom.xml)
- Run `mvn spotless:apply` before committing
- Follow Lombok best practices: use `@RequiredArgsConstructor` over `@AllArgsConstructor`
- Use UUID type consistently across entities, not strings

## References

- `@ARCHITECTURE.md` — System architecture and domain model
- `@.github/instructions/database-jpa-patterns.instructions.md` — JPA entity design and repository patterns
- `@.github/instructions/testing-patterns.instructions.md` — Spock Framework testing patterns
- `@.github/instructions/pii-protection.instructions.md` — PII protection guidelines
- `@.github/instructions/api-design-patterns.instructions.md` — REST API and OpenTelemetry patterns
