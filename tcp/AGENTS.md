# Repository Guidelines

## Project Structure & Module Organization
- Application code lives under `src/main/java/teamnova/omok`, grouped into `core` (NIO reactor), `glue` (managers, handlers, protocol), and `modules` (domain logic such as board, matching, formulas).
- Runtime resources such as logging config are in `src/main/resources` (notably `logback.xml`).
- Unit and integration tests belong in `src/test/java`; mirror the main package path when adding new suites.
- Build artifacts, coverage reports, and generated jars land under `build/`; keep this directory out of version control.

## Build, Test, and Development Commands
- `./gradlew clean build` recompiles everything and runs the full verification pipeline; rely on it before pushing.
- `./gradlew test` executes JUnit 5 suites only—use when iterating on new cases.
- `./gradlew run --args="15015"` starts the TCP server locally; override the default port when needed.
- `./gradlew shadowJar` assembles the all-in-one deployment jar at `build/libs/java-tcp-server-1.0.0-all.jar`.

## Coding Style & Naming Conventions
- Target Java 17; use spaces with a four-space indent and keep imports ordered alphabetically.
- Classes and enums use PascalCase (`MatchingManager`), methods and variables use camelCase, and constants stay in SHOUT_CASE.
- Stick with the existing package topology (`teamnova.omok.*`) and group new code by responsibility (e.g., handlers into the `glue.handler` subtree).
- Prefer SLF4J-style logging (`LoggerFactory`) so logback routing remains consistent.

## Testing Guidelines
- Write JUnit 5 tests beside related code under `src/test/java/teamnova/omok/...` with filenames ending in `Test`.
- Cover custom matching logic and state transitions with deterministic unit tests; mock network edges instead of binding real sockets.
- Run `./gradlew test` locally before every PR.

## Commit & Pull Request Guidelines
- Follow the existing convention `[scope] concise message`, e.g., `[java] Adjust matching timeout`. Use scope keywords like `java`, `infra`, or `docs`.
- Keep commits focused; squash fixups before review unless history is intentionally split.
- PRs should summarize behaviour changes, list manual or automated test evidence (`./gradlew clean build`), and link to related issues or tickets.
- Include screenshots or log excerpts when changes affect operator workflows.

## Runtime & Configuration Tips
- Default boot port is 15015 (see `Main.parsePort`); pass a CLI argument to adjust it for local clashes.
- Sensitive config (DB URLs, secrets) should flow through environment variables or externalized config files—never hard-code credentials in `src/main/java`.
- Review and update `src/main/resources/logback.xml` when altering logging categories to ensure consistent ingestion formatting.

## 프로그래밍 원칙

1. 높은 응집도
- 모듈이 수행하는 기능이 얼마나 밀접하게 관련이 있는가

2. 낮은 결합도
- 한 모듈이 다른 모듈에 얼마나 의존하는가

3. 단일 책임 원칙
- 하 모듈은 단 하나의 책임(변경 이유)만 가져야 한다.

4. 개방-폐쇄 원칙
- 확장에는 열려있고 수정에는 닫혀있어야한다(기존코드 수정없이 기능 추가).

5. 리스코프 치환 원칙
- 자식은 부모를 완전히 대체할 수 있어야한다.

6. 인터페이스 분리 원칙
- 하나의 큰 인터페이스 보다는 여러개의 작은 인터페이스가 낫다.

7. 의존성 역전 원칙
- 고수준 모듈이 저수준 모듈에 직접 의존하지 말고 추상화에 의존해야한다.

8. DRY원칙
- 같은 코드를 반복하지 말라

9. KISS원칙
- 가능한 단순하게 설계하라

10. YAGNI 원칙
- 필요하지 않은 기능을 미리 만들지 마라

11. Low of Demeter
- 친구에 친구에게 말하지 말라, 자신이 직접 아는 객체만 사용하라

12. SOLID 원칙
- S: 단일 책임 (SRP)
- O: 개방-폐쇄 (OCP)
- L: 리스코프 치환 (LSP)
- I: 인터페이스 분리 (ISP)
- D: 의존성 역전 (DIP)

## 주석 원칙

1. 왜 를 설명하고 무엇은 코드가 말하게 하라

2. 주석은 코드와 함꼐 유지 보수 되어야 한다.

3. 모듈 수준의 주석은 "요약 + 목적" 중심으로 작성하라

4. 구현 주석은 "의도" 나 "주의점"을 강조하라.

5. 불필요한 주석은 피하라

6. 일관된 형식과 태그를 사용하라

7. 문서 주석은 툴 친화적으로 작성해라