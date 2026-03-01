## Phase 6 Complete: Database Schema/Data and App Config Migration

Migrated to PostgreSQL-only banking schema, removed H2/MySQL SQL artifacts and seed data, cleaned application properties to REST-service oriented config. Default profile uses H2 with JPA auto-DDL for development/testing; Postgres profile uses explicit SQL schema init.

**Files created/changed:**
- src/main/resources/db/postgres/schema.sql (replaced with banking tables)
- src/main/resources/application.properties (REST-service config)
- src/main/resources/application-postgres.properties (PostgreSQL profile)

**Files deleted:**
- src/main/resources/db/h2/ (entire directory)
- src/main/resources/db/mysql/ (entire directory)
- src/main/resources/db/postgres/data.sql
- src/main/resources/db/postgres/petclinic_db_setup_postgres.txt
- src/main/resources/application-mysql.properties
- src/main/resources/messages/ (all i18n property files)

**Tests created/changed:**
- None new (existing tests verified: 36 pass, 0 failures)

**Review Status:** APPROVED (full test suite passes)

**Git Commit Message:**
```
chore: migrate database schema and config to banking service

- Replace Petclinic PostgreSQL schema with banking tables
- Remove H2 and MySQL database scripts and config
- Remove i18n message property files
- Update application name to bank-deposit-service
- Configure JPA and actuator for REST-only service
```
