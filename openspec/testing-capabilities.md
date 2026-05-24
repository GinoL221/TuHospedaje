# Testing Capabilities — TuHospedaje

**Strict TDD Mode**: enabled
**Detected**: 2026-05-21

## Test Runner

- **Backend**: `mvn test` — JUnit 5 (Jupiter) via Maven Surefire (implicit via spring-boot-starter-test)
- **Frontend**: none (no test framework installed in package.json)

## Test Layers

| Layer | Available | Tool |
|---|---|---|
| Unit | ✅ | JUnit 5 + AssertJ (`@Test`, `assertThat`) |
| Integration | ✅ | `@SpringBootTest` + `@AutoConfigureMockMvc` (MockMvc) |
| E2E | ❌ | — |

### Existing Tests (4 test classes)

| Test | Type | What it covers |
|---|---|---|
| `AuthServiceImplTest` | Integration (SpringBootTest) | Registration, duplicate email, login, invalid credentials |
| `AuthControllerIntegrationTest` | Integration (SpringBootTest + MockMvc) | HTTP endpoints: register, login, validation errors, auth |
| `JwtServiceTest` | Integration (SpringBootTest) | Token generation, username extraction from token |
| `BackendApplicationTests` | Smoke | Context loads |

> **Note**: All backend tests use `@SpringBootTest` (full context), so there are no pure unit tests
> with mocked dependencies yet. They are integration tests in practice.

## Coverage

- Available: ❌ (no jacoco, no `maven-surefire-plugin` coverage config)

## Quality Tools

| Tool | Available | Command |
|---|---|---|
| Linter (backend) | ❌ | — (no checkstyle/pmd/spotless in pom.xml) |
| Linter (frontend) | ✅ | `npm run lint` (ESLint 10.2.1) |
| Type checker | ❌ | — (TypeScript `@types/react` in devDeps but no `tsc` config) |
| Formatter | ❌ | — |
