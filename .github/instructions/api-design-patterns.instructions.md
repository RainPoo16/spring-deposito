---
name: 'Api Design Patterns'
description: 'REST API design patterns, OpenAPI documentation standards, and OpenTelemetry span instrumentation for controllers and services'
applyTo: '**/controller/**/*.java, **/*Controller.java, **/service/**/*.java, **/*Service.java'
---

# API Design and Documentation Patterns

> **Based on Actual Implementation**: This instruction file reflects REST API patterns used in `CardController` and actual service implementations. All examples are taken from existing code in `src/main/java/com/ytl/card/controller/`.

## Core Principles

- **Always return `ResponseEntity<T>`** — Never return plain DTOs from controller methods
- **Set explicit `contentType`** — Always specify `MediaType.APPLICATION_JSON` on success responses
- **Use `@Schema(implementation = ProblemDetail.class)`** — Required for all 4xx/5xx error responses
- **Instrument every service endpoint with OpenTelemetry spans** — Capture non-PII data only
- **Follow RESTful naming conventions** — Use nouns, plural collections, kebab-case URLs, max 2-3 nesting levels

## PII Protection

All PII protection rules from `@.github/instructions/pii-protection.instructions.md` apply. Never log customer names, card numbers, tokens, or credentials. Only log UUIDs, correlation IDs, status codes, and system-generated metadata.

## Observability and Span Instrumentation

### OpenTelemetry Span Events for Service Endpoints

For **every service endpoint** (methods in `*Service.java` classes that handle business logic), implement span instrumentation to capture non-PII data for observability in Honeycomb.

#### Step-by-Step Implementation

**1. Add `@WithSpan` annotation to the method:**
```java
@Transactional(rollbackFor = Exception.class)
@WithSpan(value = "methodName")
public ReturnType methodName(UUID customerId, String param) 
        throws CustomException {
    // method implementation
}
```

**2. Capture current span at method start:**
```java
@WithSpan(value = "methodName")
public ReturnType methodName(UUID customerId, String param) 
        throws CustomException {
    Span span = Span.current();
    setMethodNameBaseSpan(span, customerId, param);
    
    return cardService.processRequest(customerId, param);
}
```

**3. Define span attribute keys in `/observability/key/SpanAttributeKeys.java`:**

Create a nested static class matching your method name (PascalCase):
```java
public class SpanAttributeKeys {

    public static final class MethodName {
        public static final String ACTION_PREFIX = "method_name";
        
        public static final String CUSTOMER_ID =
                TraceUtils.createCommonKey(TraceNamespaceConstant.CUSTOMER_ID_KEY);
        
        public static final String CARD_ACCOUNT_ID =
                TraceUtils.createCardCommonKey(TraceNamespaceConstant.CARD_ACCOUNT_ID_KEY);
        
        public static final String CARD_ID =
                TraceUtils.createCardCommonKey(TraceNamespaceConstant.CARD_ID_KEY);
        
        public static final String TOKEN_ID =
                TraceUtils.createCardKey(ACTION_PREFIX, TraceNamespaceConstant.TOKEN_ID_KEY);
        
        public static final String TOKEN_STATUS =
                TraceUtils.createCardKey(ACTION_PREFIX, TraceNamespaceConstant.TOKEN_STATUS_KEY);
        
        // Add other relevant non-PII attributes
    }
}
```

**Key creation patterns:**
- `TraceUtils.createCommonKey()` - for common fields (customer_id, card_account_id, card_id)
- `TraceUtils.createCardCommonKey()` - for card-specific common fields
- `TraceUtils.createCardKey(ACTION_PREFIX, key)` - for method-specific card attributes
- `TraceUtils.createE6Key()` - for Episode6-specific fields (e6_customer_number, e6_card_number)

**4. Add constants to `/observability/constant/TraceNamespaceConstant.java` (if needed):**

Only add new constants if they don't already exist:
```java
public final class TraceNamespaceConstant {

    // Add new constants only if not already present
    public static final String NEW_FIELD_KEY = "new_field";
    public static final String TOKEN_UNIQUE_REFERENCE_KEY = "token_unique_reference";
}
```

**5. Create helper methods at the end of the service class:**

Create two helper methods: one for base span (initial params) and one for span event (final result):

```java
// At the end of your *Service.java class

private void setMethodNameBaseSpan(Span span, UUID customerId, String param) {
    TraceUtils.setSpanAttributes(
            span,
            List.of(
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.CUSTOMER_ID,
                            AttributeType.STRING,
                            customerId),
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.OTHER_PARAM,
                            AttributeType.STRING,
                            param)));
}

private void setMethodNameSpanEvent(Span span, CardAccount cardAccount, Card card) {
    TraceUtils.setSpanAttributes(
            span,
            List.of(
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.CUSTOMER_ID,
                            AttributeType.STRING,
                            cardAccount.getCustomerId()),
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.CARD_ACCOUNT_ID,
                            AttributeType.STRING,
                            cardAccount.getId()),
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.CARD_ID,
                            AttributeType.STRING,
                            card.getId()),
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.CARD_TYPE,
                            AttributeType.STRING,
                            card.getType()),
                    new TraceUtils.SpanAttribute(
                            SpanAttributeKeys.MethodName.LAST_FOUR_PAN,
                            AttributeType.STRING,
                            card.getLastFourPan())));
}
```

**6. Call span event method before returning:**

```java
@WithSpan(value = "methodName")
public CardDto methodName(UUID customerId, String param) 
        throws CustomException {
    Span span = Span.current();
    setMethodNameBaseSpan(span, customerId, param);
    
    CardAccount cardAccount = getCardAccount();
    Card card = cardAccount.getActiveCard();
    
    // Set span event with final non-PII data before returning
    setMethodNameSpanEvent(span, cardAccount, card);
    
    return mapToDto(card);
}
```

#### Required Imports

Add these imports to your service class:
```java
import com.ytl.card.observability.key.SpanAttributeKeys;
import com.ytl.card.observability.util.TraceUtils;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
```

#### Complete Example Reference

See `CardAccountService.java` for reference:
- Methods: `createVirtualCard()`, `createPhysicalCard()`
- Helper methods: `setVirtualCardBaseSpan()`, `setVirtualCardCreationSpanEvent()`

See `CardTokenService.java` for reference:
- Methods: `getCardTokenFromBase64()`, `activateCardToken()`
- Helper methods: `setGetCardTokenFromBase64BaseSpan()`, `setActivateCardTokenSpanEvent()`

#### Important Guidelines

- Only capture non-PII data in span attributes. See `@.github/instructions/pii-protection.instructions.md` for the complete list.
- Use `AttributeType.STRING` for all values (UUIDs, enums, strings, numbers).
- Keep attribute keys in snake_case (e.g., `customer_id`, `card_type`).

**Naming Conventions:**
- Nested class name: PascalCase matching method name (e.g., `GetCardTokenFromBase64`, `ActivateCardToken`)
- ACTION_PREFIX: snake_case (e.g., `"get_card_token_from_base64"`, `"activate_card_token"`)
- Helper method names: `set{MethodName}BaseSpan()` and `set{MethodName}SpanEvent()`

**When to Capture Spans:**
- Base span: At method entry with initial parameters
- Span event: Before returning with final result/entity data
- For methods with multiple return paths, call span event before each return

**File Organization:**
- Span attribute keys: `/src/main/java/com/ytl/card/observability/key/SpanAttributeKeys.java`
- Constants: `/src/main/java/com/ytl/card/observability/constant/TraceNamespaceConstant.java`
- Helper methods: At the end of the respective `*Service.java` class (after all public methods)

## REST Controller Design

### REST API Endpoint Naming Conventions

Follow these principles for creating intuitive, RESTful endpoint URLs:

#### 1. Use Nouns, Not Verbs
Endpoints represent resources (nouns), not actions. HTTP methods indicate the action.

#### 2. Use Plural Nouns for Collections
Use plural forms consistently for resource collections.

#### 3. Use Hierarchical Structure for Relationships
Show resource relationships through URL structure (max 2-3 levels). For deeper nesting, use query parameters.

#### 4. Use Lowercase and Hyphens
Use kebab-case (lowercase with hyphens), not camelCase, underscores, or PascalCase.

#### 5. Keep URLs Simple and Intuitive
Keep URLs readable and predictable. Avoid deep nesting beyond 2-3 levels.

#### 6. Use Query Parameters for Filtering, Sorting, and Pagination
Don't create separate endpoints—use query parameters for filtering, sorting, and pagination.

**Naming Conventions Examples:**

| ✅ Good | ❌ Bad | Rule |
|---------|--------|------|
| `GET /cards` | `GET /getCards` | Use nouns, not verbs |
| `GET /card-accounts` | `GET /card-account` | Use plural nouns |
| `GET /customers/{id}/cards` | `GET /card-transactions?cardId={id}` | Use hierarchical structure |
| `/api/card-accounts` | `/api/cardAccounts` | Use lowercase + hyphens |
| `GET /cards?status=active` | `GET /active-cards` | Use query params for filters |

#### 7. Use Standard HTTP Methods Correctly

| Method | Purpose | Idempotent | Request Body | Response Body | Common Status |
|--------|---------|------------|--------------|---------------|---------------|
| **GET** | Retrieve | ✅ Yes | ❌ No | ✅ Yes | 200, 404 |
| **POST** | Create/Action | ❌ No | ✅ Yes | ✅ Yes | 201, 200 |
| **PUT** | Replace all | ✅ Yes | ✅ All fields | Optional | 200, 204 |
| **PATCH** | Update partial | ✅ Yes | ✅ Changed fields | Optional | 200, 204 |
| **DELETE** | Remove | ✅ Yes | ❌ No | ❌ No | 204, 404 |

**Key Characteristics:**
- **GET**: Safe, cacheable, no side effects
- **POST**: Not idempotent, use for creation or actions (e.g., `/cards/{id}/activate`)
- **PUT**: Replace entire resource (all fields required in body)
- **PATCH**: Update specific fields only (partial update)
- **DELETE**: Idempotent (multiple deletes = same result)

**PUT vs PATCH Decision:**
- **PATCH** for updating 1-3 specific fields: `{"nickname": "New Name"}`
- **PUT** for replacing entire resource with all fields

#### 8. Action Endpoints (Non-CRUD Operations)
For operations that don't fit CRUD, use verb as sub-resource with POST method:

```
POST /cards/{cardId}/activate
POST /cards/{cardId}/lock
POST /transactions/{id}/refund
```

**Pattern**: `POST /resources/{id}/action-verb`

#### 9. Domain-Specific Path Prefixes

Based on controller type and audience:

| Controller Type | Path Prefix | Example Endpoints |
|-----------------|-------------|-------------------|
| **Customer-Facing** | `/{domain}` | `/cards`, `/accounts`, `/transactions` |
| **Administrative** | `/internal/{domain}` | `/internal/reconciliation`, `/internal/cards` |
| **External Provider** | `/webhooks/{provider}` or `/callbacks/{provider}` | `/webhooks/visa`, `/callbacks/san` |

### Controller Naming and Tag Conventions

| Controller Type | Class Name | Path Prefix | Tag Description | Profile |
|-----------------|------------|-------------|-----------------|---------|
| **Administrative** | `Internal{Domain}Controller` | `/internal/...` | `API endpoints for administrative {domain} operations.` | `application-internal` |
| **Customer-Facing** | `{Domain}Controller` | `/{domain}/...` | `API endpoints for customer {domain} operations.` | `application-self-service` |
| **External Provider** | `{Provider}{Domain}Controller` | `/webhooks/{provider}/...` | `API endpoints for {Provider} {integration type}.` | `application-webhook` |

### Controller Structure
- Annotate with `@RestController` and `@Tag` for grouping
- Use `@Profile` for environment-specific activation
- Use `@RequestMapping` only for shared base paths (e.g., `/internal`)
- Keep constructor injection (no field injection)

### Request/Response Handling
- Use `@RequestHeader` for contextual IDs (e.g., `x-customer-id`); document with `@Parameter`
- Use `@PathVariable` for entity identifiers in URL path
- Use `@RequestBody @Valid` for complex payloads
- **ALWAYS return `ResponseEntity<T>`** - never plain DTOs
- Set explicit `contentType` (`MediaType.APPLICATION_JSON`)
- For empty success, use `ResponseEntity.noContent().build()`

**Why `ResponseEntity<>` is Required:**
- Explicit control over HTTP status codes and headers
- Consistent error handling across endpoints
- Makes HTTP semantics testable

**Example Pattern:**
```java
// ❌ Bad - Direct DTO return
public CardAccountDto getAccount(@PathVariable UUID id) {
    return cardService.getAccount(id); 
}

// ✅ Good - Explicit ResponseEntity
public ResponseEntity<CardAccountDto> getAccount(@PathVariable UUID id) {
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(cardService.getAccount(id));
}
```

### Example Controller Pattern

```java
// Customer-Facing Controller
@RestController
@Tag(name = "Card", description = "API endpoints for customer card operations.")
@Profile("application-self-service")
public class CardController {
    private final CardAccountService cardAccountService;

    public CardController(CardAccountService cardAccountService) {
        this.cardAccountService = cardAccountService;
    }

    @Operation(summary = "Get customer active card accounts")
    @GetMapping("/accounts")
    public ResponseEntity<GetCardsResp> getCustomerActiveCardAccounts(
            @RequestHeader("x-customer-id") UUID customerId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GetCardsResp(cardAccountService.getActiveCardsByCustomerId(customerId)));
    }
}

// Administrative Controller
@RestController
@RequestMapping("/internal")
@Tag(name = "Admin", description = "API endpoints for administrative operations.")
@Profile("application-internal")
public class InternalReconciliationController {
    // Similar structure with /internal paths
}

// External Provider Controller
@RestController
@RequestMapping("/webhooks/visa")
@Tag(name = "Visa Webhook", description = "API endpoints for Visa webhook integrations.")
@Profile("application-webhook")
public class VisaWebhookController {
    // Similar structure with /webhooks paths
}
```

## OpenAPI Documentation

### Annotation Guidelines
- Always use `@Operation` with: summary, parameters, responses
- Use `@Tag` on controller classes for grouping
- **ALWAYS use `@Schema(implementation = ProblemDetail.class)` for all 4xx and 5xx error responses**
- Include at least one success example and one error example using `@ExampleObject`
- Keep example JSON minimal yet representative

### Reusable Meta-Annotations
- Prefer meta-annotations for frequently repeated documentation blocks
- Store under `controller.swagger` or `controller.doc` package
- Keep names imperative and specific: `LockCardDoc`, `UnlockCardDoc`

**Meta-Annotation Checklist:**
1. Clear `summary` (< 80 chars)
2. All headers & path params in `parameters`
3. Success response with schema + example
4. Each error code with `@Schema(implementation = ProblemDetail.class)` + example

## Problem Details and Error Handling

### Problem Details Conventions
- Error responses use RFC7807 style (problem+json)
- **ALWAYS use `@Schema(implementation = ProblemDetail.class)` for all 4xx/5xx error responses**
- `type` format: `card/<kebab-case-problem>` (e.g., `card/card-not-found`)
- Response classes end with `Resp` (e.g., `CardNotFoundResp`)
- HTTP code mappings:
  - 404: Resource not found
  - 422: Business rule violation
  - 424: External provider dependency failure
  - 409: Workflow/execution conflicts
  - 400: Malformed request

### Global Exception Handler
- One central `@RestControllerAdvice` mapping exceptions → problem response DTOs
- Always return `ResponseEntity` with explicit status + `MediaType.APPLICATION_PROBLEM_JSON`
- Log exception with correlation/trace ID before returning response

## Request/Response DTOs

### DTO Design
- Prefer Java `record` for pure data carriers
- Separate request and response: suffix `Req` and `Resp`
- Avoid exposing unnecessary internal IDs
- Flatten nested structures where it improves readability

### Validation Patterns
- Place Bean Validation annotations in DTOs (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`)
- Use custom validators for domain-specific constraints
- Don't duplicate validation in controller if enforced by DTO + service layer

## Pagination and Query Parameters

- Use Spring `Pageable` for large lists; simple collections for bounded lists
- Response wrapper naming: `GetCardsResp` containing `List<CardAccountSummary>`
- Document optional parameters with `@Parameter(required = false)`
- Document enum allowed values in description/examples

## Security & Headers

- Header naming: use `x-` prefix for custom headers (`x-customer-id`)
- Document headers via `@Parameter` (no implicit/undocumented headers)

## Content Type and Serialization

- Success: `MediaType.APPLICATION_JSON_VALUE`
- Error: `MediaType.APPLICATION_PROBLEM_JSON_VALUE`
- Always set `contentType` explicitly in `ResponseEntity`
- Configure Jackson to serialize enums consistently
- Omit null fields for cleaner payloads

## Examples Best Practices

- Use Java text blocks (`""" ... """`) for multi-line JSON in `@ExampleObject`
- Keep examples stable and deterministic (no dynamic timestamps)
- Show only fields relevant to structure; omit or use ellipsis for the rest


## Service Client Registration

After creating endpoints, register them in the appropriate test service client for integration testing:

**Client Locations:**
- Administrative (`/internal/**`): `InternalCardServiceClient` in `src/test/groovy/com/ytl/card/client/`
- Customer-facing: `CardServiceClient` in `src/test/groovy/com/ytl/card/client/`
- External provider: Provider-specific client (e.g., `VisaWebhookServiceClient`)

**Registration Checklist:**
- [ ] Test method added to appropriate service client
- [ ] Method name reflects endpoint operation clearly
- [ ] Request/response types match controller DTOs
- [ ] Headers properly set (e.g., `x-customer-id`)
- [ ] HTTP method matches controller endpoint
- [ ] Path matches controller `@RequestMapping` + method mapping

## Review Gate Before Merging

### Documentation and Schema Compliance
- Each new endpoint: at least one success and one error example
- No leftover generic terms (e.g. ensure no `Lead` references in card domain code/docs)
- Status codes match mapped exception handler
- Problem `type` strings follow `card/<kebab-case>` pattern
- **All controller methods return `ResponseEntity<T>`, never plain DTOs**
- **All 4xx and 5xx error responses use `@Schema(implementation = ProblemDetail.class)`**
- Explicit `contentType` set for all success responses (use `MediaType.APPLICATION_JSON`)

### Controller Naming Conventions
- **Controller naming follows conventions**:
  - Administrative: `Internal{Domain}Controller` with `/internal/...` paths
  - Customer-facing: `{Domain}Controller` with domain-based paths
  - External provider: `{Provider}{Domain}Controller` with provider-specific paths
- **Tag descriptions follow format**: `API endpoints for [administrative|customer|{provider}] {domain} operations.`

### REST API Endpoint Naming Validation
- **Endpoints use nouns, not verbs** (e.g., `/cards` not `/getCards`)
- **Collections use plural nouns** (e.g., `/products` not `/product`)
- **Hierarchical relationships properly structured** (e.g., `/customers/{id}/cards`)
- **URLs use lowercase with hyphens** (e.g., `/card-accounts` not `/cardAccounts` or `/card_accounts`)
- **Nesting limited to 2-3 levels** (avoid deep hierarchies)
- **Filtering/sorting/pagination uses query parameters** (not separate endpoints)
- **Action endpoints use verb as sub-resource** (e.g., `/cards/{id}/activate` for non-CRUD operations)

### HTTP Method Usage Validation
- **GET** - Retrieve only (safe, idempotent, no request body)
- **POST** - Create resources or trigger actions (returns 201/200, has request body)
- **PUT** - Replace entire resource (idempotent, all fields in body)
- **PATCH** - Partial update (idempotent, only changed fields in body)
- **DELETE** - Remove resource (idempotent, no body, returns 204)
- **POST for actions** - Use `/resources/{id}/action-verb` pattern for non-CRUD operations

### Test Client Registration
- **Test client registration complete**:
  - `/internal/**` endpoints registered in `InternalCardServiceClient`
  - Customer-facing endpoints registered in `CardServiceClient`
  - External provider endpoints registered in provider-specific client

## References

- `@ARCHITECTURE.md` - System architecture and domain model
- `@.github/instructions/pii-protection.instructions.md` - PII protection guidelines for logging and data handling
- `@.github/instructions/testing-patterns.instructions.md` - Spock Framework testing patterns and TestContainers setup
- `@.github/instructions/java-spring-coding-standards.instructions.md` - Java 21 coding standards and Spring Boot patterns
- `@.github/instructions/pagination-endpoint-design-patterns.instructions.md` - Paginated list endpoint patterns
