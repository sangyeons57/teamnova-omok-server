# Repository Guidelines

## Project Structure & Module Organization
- Runtime code lives in `src/main/java/teamnova/omok`. `core` owns networking (`NioReactorServer`, codecs), while `glue` orchestrates gameplay, handlers, and integration layers. Reusable engines (matching, rule, state machine) stay under `modules`.
- The refactored client-session stack sits in `glue/client/session`: `model` contains the pure `ClientSession` data object, `interfaces` exposes `ClientSessionHandle`/`LifecycleListener`, `services` provides `ManagedClientSession`, directory, and lifecycle logic, and `states` wraps the state machine.
- Game-session logic now lives in `glue/game/session`: `model` holds `GameSession`, board/turn stores, `services` groups orchestration (`InGameSessionService`, `SessionEventService`, coordinators), `states` contains the in-game state machine, and `interfaces` is reserved for future public contracts. External callers go through `GameSessionManager` in this package to interact with sessions.
- Tests mirror production packages under `src/test/java`; add new suites beside the code they cover. Shared fixtures belong in `src/test/java/teamnova/omok/support` (create if missing).
- Assets such as `logback.xml` are in `src/main/resources`; build outputs collect in `build/` and should be ignored by Git.

## Build, Test, and Development Commands
- `./gradlew build` compiles sources, runs JUnit tests, and packages distribution zips; run before every push.
- `./gradlew test` is faster when iterating on unit tests.
- `./gradlew run --args="15015"` launches the TCP server on a custom port; omit the argument to use the default.
- `./gradlew shadowJar` produces the fat jar at `build/libs/java-tcp-server-1.0.0-all.jar` for deployments.

## Coding Style & Naming Conventions
- Target Java 17, four-space indentation, and alphabetical imports with no wildcards. Keep one top-level type per file.
- Classes/enums use PascalCase, methods and fields use camelCase, constants stay in UPPER_SNAKE_CASE.
- Group packages by responsibility (e.g., new match handlers under `glue/handler`) and respect the client-session layering: models stay mutation-free, services perform side effects, managers delegate.
- Prefer SLF4J (`LoggerFactory`) and avoid `System.out` in production paths; wire logs through logback configs.

## Testing Guidelines
- Use JUnit 5; name test classes `*Test` and mirror the package of the code under test.
- Mock network boundaries (`NioClientConnection`) when validating session flows; focus on state transitions via `ClientSessionHandle` contracts.
- Add regression tests for lifecycle changes (auth eviction, broadcast delivery, timeout cleanup) whenever altering session services.
- Run `./gradlew test` locally and capture noteworthy logs for reviewers when behavior changes.

## Commit & Pull Request Guidelines
- Follow `[scope] concise summary`, e.g., `[session] Enforce single login`. Keep scopes consistent with existing history (`session`, `match`, `docs`).
- Bundle related changes together and squash fixups; keep refactors and feature work in separate commits when practical.
- PRs should describe behavior changes, list automated/manual validation (`./gradlew build`), and reference Jira/issues. Attach screenshots or log excerpts for gameplay-facing updates.

## Client Session Architecture Tips
- Treat `ClientSessionManager` as the public entry point. Use `ClientSessionHandle` in handlers and services; only the manager/service layer should touch `ManagedClientSession` implementations.
- When adding features, keep stateful logic in `glue/client/session/services` and evolve the `ClientSession` model with minimal behavior—let the lifecycle service and state hub drive transitions.
