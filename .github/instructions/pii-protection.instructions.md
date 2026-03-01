---
name: 'Pii Protection'
description: 'CRITICAL: Mandatory guidelines for protecting PII and sensitive data in code, logs, events, and documentation'
applyTo: '**/*.java, **/*.groovy'
---

# PII and Sensitive Data Protection Guidelines

## Overview

This document establishes mandatory guidelines for handling Personally Identifiable Information (PII) and sensitive data in code, logs, and documentation. These rules apply to ALL code: production, testing, backfilling, debugging, and development.

## Core Principles

- **Never log PII or sensitive data** — violation is a security incident and regulatory compliance breach
- **Default to exclusion** — when uncertain whether data qualifies as PII, do not log it
- **Use identifiers, not identity** — always log UUIDs, correlation IDs, and status codes instead of personal or financial data
- **Protect all environments** — PII rules apply equally to production, testing, backfilling, and debugging code
- **Mask when unavoidable** — if sensitive data must appear in logs, use dedicated masking functions and restricted audit logs

> **Precedence**: PII protection rules override all other instruction files. When any instruction conflicts with PII protection, PII protection wins.

## Critical Rule: Never Log PII or Sensitive Data

**Violation of this rule is a security incident and regulatory compliance breach.**

## What is Considered PII/Sensitive Data?

### Personal Information
- ❌ Full names (first name, last name, middle name)
- ❌ Email addresses
- ❌ Phone numbers (mobile, landline)
- ❌ Physical addresses (street, city, postal codes)
- ❌ Date of birth
- ❌ Age (if identifiable)
- ❌ Gender (in combination with other data)
- ❌ Nationality
- ❌ Biometric data (fingerprints, facial recognition data)

### Financial Information
- ❌ Card numbers (PAN - Primary Account Number)
- ❌ CVV/CVC codes
- ❌ Card expiry dates (in combination with other card data)
- ❌ Card tokens (unless explicitly designed for logging)
- ❌ Bank account numbers
- ❌ IBAN numbers
- ❌ Routing numbers
- ❌ Transaction amounts (when combined with identifying info)

### Government/Legal Identifiers
- ❌ National ID numbers (IC, NRIC, SSN, etc.)
- ❌ Passport numbers
- ❌ Driver's license numbers
- ❌ Tax identification numbers
- ❌ Business registration numbers (when linked to individuals)

### Authentication & Security
- ❌ Passwords (plain text or hashed)
- ❌ PINs
- ❌ Security questions/answers
- ❌ Authentication tokens (JWT, OAuth tokens)
- ❌ API keys
- ❌ Encryption keys
- ❌ OTP codes

### Technical Identifiers
- ❌ IP addresses (can be PII under GDPR)
- ❌ MAC addresses
- ❌ Device identifiers (IMEI, serial numbers)
- ❌ Session IDs (in some contexts)
- ❌ Browser fingerprints

## What CAN Be Logged

### Safe Identifiers
- ✅ UUIDs (customer IDs, card IDs, transaction IDs)
- ✅ Correlation IDs
- ✅ Request IDs
- ✅ Trace IDs
- ✅ Internal reference numbers (non-PII)

### Technical Metadata
- ✅ Timestamps
- ✅ Status codes (HTTP, business logic)
- ✅ Error codes and types
- ✅ Method names and class names
- ✅ Processing durations
- ✅ Thread IDs
- ✅ Service names and versions

### Aggregate Data
- ✅ Counts (records processed, failed, succeeded)
- ✅ Statistics (averages, percentiles - without individual data)
- ✅ Batch numbers
- ✅ Pagination cursors (UUIDs)

### Non-Sensitive Business Data
- ✅ Product types (card type: VIRTUAL, PHYSICAL)
- ✅ Status values (ACTIVE, INACTIVE, PENDING)
- ✅ Category names
- ✅ Public configuration values

## Logging Guidelines by Context

### 1. Production Application Logs

**DO:**
```java
// Log IDs and technical context
logger.info("Processing card activation for cardId: {}", cardId);
logger.info("Customer {} updated successfully", customerId);
logger.error("Failed to process transaction {}: {}", txnId, e.getMessage());
```

**DON'T:**
```java
// NEVER log sensitive data
❌ logger.info("Activating card {} for {}", cardNumber, customerName);
❌ logger.info("Processing payment from {} to {}", email, phoneNumber);
❌ logger.error("Invalid card details: PAN={}, CVV={}", pan, cvv);
❌ logger.debug("Request body: {}", requestBody); // May contain PII
❌ logger.info("Customer: {}", customer); // toString() may expose PII
```

### 2. Backfilling and Migration Tasks

**DO:**
```java
// Log batch progress with IDs only
logger.warn("Processing batch {} for CardAccount (size: {}, lastId: {})", 
    batchNumber, records.size(), lastId);
logger.warn("Batch {}: Updated {} / Processed {} / Skipped {}", 
    batchNumber, updated, processed, skipped);
logger.warn("Skipped record {}: validation failed", recordId);
```

**DON'T:**
```java
// NEVER log field values being updated
❌ logger.warn("Updating card {} with new status {}", cardNumber, newStatus);
❌ logger.warn("Processing record: {}", Arrays.toString(record)); // May contain PII
❌ logger.warn("Old value: {}, new value: {}", oldEmail, newEmail);
❌ logger.error("Failed to update: {}", entity); // Entity may contain PII
```

### 3. Error Handling and Debugging

**DO:**
```java
// Log error types and context without sensitive data
logger.error("Validation failed for customer {}: {}", 
    customerId, e.getClass().getSimpleName());
logger.error("Database error processing record {}", recordId, e);
logger.warn("Business rule violation for transaction {}: error_code={}", 
    txnId, errorCode);
```

**DON'T:**
```java
// NEVER log sensitive data in errors
❌ logger.error("Invalid email format: {}", email);
❌ logger.error("Card verification failed: {}", cardDetails);
❌ logger.error("Request failed: {}", request); // May contain PII
❌ logger.error("Exception: {}", e); // If exception message contains PII
```

### 4. API Request/Response Logging

**DO:**
```java
// Log request metadata only
logger.info("API request received: method={}, path={}, customerId={}", 
    method, path, customerId);
logger.info("API response: status={}, duration={}ms", status, duration);
```

**DON'T:**
```java
// NEVER log request/response bodies
❌ logger.debug("Request: {}", requestBody);
❌ logger.debug("Response: {}", responseBody);
❌ logger.info("Headers: {}", headers); // May contain auth tokens
❌ logger.debug("Parameters: {}", params); // May contain PII
```

### 5. Testing and Development

**DO:**
```java
// Use clearly fake data in tests
def testEmail = "test-user@example.com"
def testCard = "1234-5678-9012-3456"  // Clearly not real
logger.info("Test created entity with id: {}", entity.id)
```

**DON'T:**
```java
// AVOID realistic test data in logs
❌ def testEmail = "john.smith@gmail.com" // Looks real
❌ logger.info("Created test card: {}", card.getPan())
❌ println(customer) // May print PII via toString()
```

## Data Masking When Logging is Required

If you absolutely must log sensitive data for debugging (with proper justification and security controls):

### 1. Use Masking Functions
```java
// Show only last 4 digits of card
public static String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) return "****";
    return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
}

logger.debug("Processing card ending in {}", maskCardNumber(pan));
```

### 2. Use Dedicated Audit Logs
```java
// Separate secure audit log with restricted access
auditLogger.info("Card {} accessed by user {} at {}", 
    cardId, userId, timestamp); // No PII, just event
```

### 3. Add Security Warnings
```java
// Document why sensitive data is logged
// SECURITY: Logging masked PAN for fraud investigation support
// Access restricted to security team only
secureLogger.warn("Fraud check: cardId={}, lastFour={}", 
    cardId, lastFour);
```

## Code Review Checklist

Before submitting code, verify:

- [ ] No `logger.info/debug/warn/error()` statements contain PII
- [ ] No `System.out.println()` or `println()` with sensitive data
- [ ] No `.toString()` calls on entities that may contain PII
- [ ] No logging of request/response bodies without sanitization
- [ ] No logging of exception messages that may contain PII
- [ ] No logging of Array/Collection contents that may contain PII
- [ ] Test data is clearly fake and non-realistic
- [ ] Comments don't include example PII

## Enforcement and Compliance

### Why This Matters

1. **Legal Compliance**: GDPR, PDPA, CCPA, and other regulations prohibit logging PII
2. **Security**: Logs are often stored long-term and accessible to many people
3. **Data Breaches**: Logs are common targets in security incidents
4. **Audit Requirements**: Regulatory audits flag PII in logs as critical violations
5. **Third-Party Access**: Logs may be sent to external monitoring/alerting systems

### Consequences of Violations

- Security incident reporting required
- Regulatory fines (up to 4% of annual revenue under GDPR)
- Customer trust damage
- Mandatory breach notifications
- Code must be patched and redeployed immediately
- Historical logs may need to be purged

### What to Do If You Find PII in Logs

1. **Immediate**: Remove the logging statement in code
2. **Report**: Notify security team about potential exposure
3. **Patch**: Deploy fix as high priority
4. **Clean**: Work with ops team to purge affected logs if possible
5. **Review**: Audit related code for similar issues

## Tools and Utilities

### Static Analysis
```bash
# Search for potential PII logging (run before committing)
grep -r "logger.*email" src/
grep -r "logger.*phone" src/
grep -r "logger.*card" src/
grep -r "logger.*pan" src/
grep -r "logger.*password" src/
```

### Secure Logging Utility (Example)
```java
public class SecureLogger {
    private static final Logger logger = LoggerFactory.getLogger(SecureLogger.class);
    
    public static void logWithId(String message, UUID id) {
        logger.info("{} [id={}]", message, id);
    }
    
    public static void logError(String message, UUID id, Exception e) {
        logger.error("{} [id={}]: {}", message, id, e.getClass().getSimpleName());
    }
}
```

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Logging entity objects via `.toString()` that expose PII fields | Always log specific safe fields (IDs, statuses) instead of entire objects |
| Logging request/response bodies that contain customer data | Log only request metadata (method, path, status, duration) — never bodies |
| Using realistic-looking PII in test data (e.g., real name formats) | Use obviously fake data (`test-user@example.com`, `1234-5678-9012-3456`) |

## References

- **GDPR**: Article 5 (data minimization), Article 32 (security)
- **PDPA** (Singapore): Section 24 (protection of personal data)
- **PCI DSS**: Requirement 3 (protect stored cardholder data)
- **ISO 27001**: A.18.1.3 (protection of records)
- `@.github/instructions/testing-patterns.instructions.md` — PII rules for test data
- `@.github/instructions/java-spring-coding-standards.instructions.md` — logging best practices

