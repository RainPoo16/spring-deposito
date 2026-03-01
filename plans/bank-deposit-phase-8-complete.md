## Phase 8 Complete: Deployment, CI, and Runtime Packaging Alignment

Updated all deployment configs, CI workflow, and build dependencies to align with the banking microservice identity. Removed MySQL, Thymeleaf, and WebJars dependencies. H2 scoped to test-only. PostgreSQL is the only production database. No petclinic references remain.

**Files created/changed:**
- settings.gradle (project name → bank-deposit-service)
- docker-compose.yml (PostgreSQL-only, credentials aligned)
- k8s/deposit-service.yml (new app manifest)
- k8s/db.yml (updated credentials/labels)
- .github/workflows/deploy-and-test-cluster.yml (updated readiness labels)
- pom.xml (removed MySQL/Thymeleaf/WebJars/CSS deps, H2 scoped to test)
- build.gradle (removed MySQL/Thymeleaf/WebJars, H2 scoped to test)
- README.md (banking service documentation)

**Files deleted:**
- k8s/petclinic.yml

**Functions created/changed:**
- N/A (config/deployment changes only)

**Tests created/changed:**
- None new (38 existing tests all passing)

**Review Status:** APPROVED (after revision to clean WebJars/CSS and scope H2)

**Git Commit Message:**
```
chore: align deployment, CI, and dependencies to banking service

- Update k8s manifests with deposit-service labels and image
- Update docker-compose to PostgreSQL-only with deposito credentials
- Update CI workflow readiness labels
- Remove MySQL, Thymeleaf, WebJars, CSS dependencies from Maven/Gradle
- Scope H2 to test-only
- Replace README with banking service documentation
- Rename project to bank-deposit-service
```
