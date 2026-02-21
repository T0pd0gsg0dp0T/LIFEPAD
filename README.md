# LIFEPAD

[![CI](https://github.com/T0pd0gsg0dp0T/LIFEPAD/actions/workflows/ci.yml/badge.svg)](https://github.com/T0pd0gsg0dp0T/LIFEPAD/actions/workflows/ci.yml)

LIFEPAD is an Android app built with Kotlin and Jetpack Compose. It includes modules for journaling, notepad, finance tracking, dashboards, and search.

The purpose of this project is to provide a spyware-free version of some of my favorite apps.

## Getting Started

### Build

```bash
./gradlew assembleDebug
```

### Test

```bash
./gradlew test
```

### Lint

```bash
./gradlew lint
```

## Project Structure

- `app/src/main/kotlin/com/lifepad/app/` main source root
- `app/src/main/res/` resources
- `app/schemas/` Room schemas
- `app/src/test/` unit tests
- `app/src/androidTest/` instrumented tests
