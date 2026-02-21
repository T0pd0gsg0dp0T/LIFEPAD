# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/kotlin/com/lifepad/app/` is the main source root.
- Feature packages include `dashboard/`, `notepad/`, `journal/`, `finance/`, `search/`, and `navigation/`.
- Shared layers live in `data/` (Room entities/DAOs/repositories), `domain/` (parsers/business logic), `di/` (Hilt modules), `components/`, `notification/`, `security/`, `ui/`, and `util/`.
- Resources are in `app/src/main/res/`; Room schemas are in `app/schemas/`.
- Tests live in `app/src/test/` (unit) and `app/src/androidTest/` (instrumented/Compose UI).

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew assembleRelease` builds a release APK (minified via Proguard rules).
- `./gradlew installDebug` installs the debug APK on a connected device.
- `./gradlew test` runs unit tests.
- `./gradlew testDebugUnitTest --tests "com.lifepad.app.NoteViewModelTest"` runs a single unit test class.
- `./gradlew connectedAndroidTest` runs instrumented tests on a device/emulator.
- `./gradlew lint` runs Android lint checks.

## Coding Style & Naming Conventions
- Kotlin + Jetpack Compose with 4-space indentation (follow existing files in `app/src/main/kotlin/`).
- Package names are lowercase (e.g., `com.lifepad.app.journal`).
- Classes/composables use `PascalCase`; functions/variables use `camelCase`.
- Keep feature UI and ViewModels in their feature package; shared UI goes in `components/`.

## Testing Guidelines
- Unit tests use JUnit4 with Truth and MockK; instrumented tests use AndroidX Test and Espresso.
- Name test classes with the `*Test` suffix and mirror production package paths.
- If you change the Room schema, ensure updated JSON schema files are committed in `app/schemas/`.

## Commit & Pull Request Guidelines
- Git history is not available in this workspace, so no commit convention can be inferred.
- Use concise, scoped messages (example: `feat(journal): add mood filter`).
- PRs should include a summary, test commands with results, and screenshots for UI changes.

## Configuration & Security Notes
- Keep SDK paths and machine-local values in `local.properties` (do not commit).
- Avoid committing secrets or personal data in fixtures, sample content, or screenshots.
