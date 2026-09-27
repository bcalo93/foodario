# Foodario

> Offline-first "fridge/pantry" app — answers the one question that matters before you shop: **"Do I already have this at home?"**

Foodario is a Kotlin Multiplatform app that keeps a small notebook of what you have at home and what you still need to buy. Everything lives on the device — no backend, no accounts.

The whole product is shaped around three actions:

| Action                                    | What it does                                         |
|-------------------------------------------|------------------------------------------------------|
| **➕ Tengo** ("I have")                    | Add an item to the inventory                         |
| **➖ Consumí** ("I used")                  | Reduce or remove a quantity from the inventory       |
| **🛒 Necesito comprar** ("I need to buy") | Track the shopping list, separate from the inventory |

The natural flow: `Shop → add to fridge → consume → inventory updates → when it runs out, move to the shopping list`.

The visual metaphor is a hand-written notebook — see [`docs/DESIGN.md`](./docs/DESIGN.md).

---

## Features

- **Quick add** — type a name, pick a category chip, hit `+`. Done. Defaults fill in the rest.
- **Inventory list** — search, filter by category, sort alphabetically, with quantity steppers and freeze / expiration toggles.
- **Item detail** — adjust quantity (long-press `+`/`−` to step continuously), switch category or unit, toggle frozen, set expiration.
- **Shopping list** — hand-drawn checkboxes, "move to fridge" shortcut when you buy something.
- **Offline-first** — everything is stored locally with SQLDelight. No network calls.
- **Light & dark mode** — "classic notebook" / "night notebook" palettes (WCAG AA verified).

---

## Tech stack

Versions are pinned in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml).

| Concern       | Tech                                                                             |
|---------------|----------------------------------------------------------------------------------|
| Multiplatform | Kotlin Multiplatform (Kotlin `2.4.10`, AGP `9.0.1`)                              |
| UI            | Compose Multiplatform `1.11.1` (Material 3, Navigation Compose type-safe routes) |
| Persistence   | SQLDelight `2.3.2` (Android + iOS native drivers)                                |
| DI            | Koin `4.1.1` (named use cases, ViewModel DSL)                                    |
| Async         | Kotlin Coroutines + Flow + `kotlinx-datetime`                                    |
| Tests         | `kotlin.test` + MockK + Turbine + `kotlinx-coroutines-test`                      |

Targets:

- `commonMain` — shared UI, ViewModels, use cases, repositories, SQLDelight schema.
- `androidMain` — Android `SqlDriver`, Android Koin bootstrap.
- `iosMain` — native `SqlDriver`, iOS Koin bootstrap, `MainViewController()` Swift entry point.

---

## Project layout

```
foodario/
├── androidApp/      Android shell — Application + Activity mount App()
├── iosApp/          iOS shell — Xcode project + SwiftUI host
├── shared/          All business logic, UI, persistence (Kotlin Multiplatform)
│   └── src/
│       ├── commonMain/       UI, ViewModels, use cases, repositories, .sq files
│       ├── androidMain/      Android SqlDriver + Koin init
│       ├── iosMain/          iOS SqlDriver + Koin init + MainViewController()
│       ├── commonTest/       Pure-JVM tests
│       ├── androidHostTest/  Android tests with sqlite in-memory driver
│       └── iosTest/          iOS simulator tests
├── docs/            ARCHITECTURE.md, DESIGN.md, RELEASE.md
├── gradle/          Version catalog (libs.versions.toml) + Gradle wrapper
└── AGENTS.md        Instructions for AI agents working in this repo
```

New code belongs in `shared/`. The `androidApp/` and `iosApp/` shells should stay thin.

Each feature in `shared/src/commonMain/kotlin/com/foodario/` follows the same layout:

```
<feature>/
├── data/                       repositories + SQLDelight mappers
├── domain/
│   ├── model/                 plain data classes / enums
│   ├── repository/            repository interfaces
│   └── usecase/               UseCase<P,R> / ObserveUseCase<P,R> implementations
└── presentation/
    ├── uicomponents/          sub-composables (dialogs, rows, overlays)
    ├── *ViewModel.kt
    ├── *UiState.kt
    └── *Screen.kt
```

See [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) for the full layer rules.

---

## Build & run

### Android

```bash
./gradlew :androidApp:assembleDebug
```

The debug APK is produced at `androidApp/build/outputs/apk/debug/`. Install it with `adb install -r <apk>` or run the `androidApp` configuration from Android Studio.

### iOS

```bash
open iosApp/iosApp.xcodeproj
```

Xcode builds and runs the app on the simulator or device. Before the first iOS launch, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` so the bundle id resolves correctly (`com.foodario.Foodario<TEAM_ID>`).

### Required before building Android

- `local.properties` at the repo root with `sdk.dir=<ANDROID_SDK_PATH>` (gitignored).
- JDK 17 (CI uses Temurin). The local Gradle daemon uses Azul JDK 21 via the toolchain.

---

## Tests

Run a focused test, not the whole suite:

```bash
./gradlew :shared:test                # pure-JVM commonTest
./gradlew :shared:testAndroidHostTest # Android tests (MockK + in-memory sqlite driver)
./gradlew :shared:iosSimulatorArm64Test # iOS simulator tests
```

Android tests assemble a Koin graph manually — use `shared/src/androidHostTest/.../KoinGraphTest.kt` as the template.

CI runs `./gradlew :androidApp:check :shared:check` before assembling the release APK.

There is **no** `lint`, `detekt`, `ktlint`, or `spotless` configuration. Verification is the default Gradle `check` task.

---

## Release

The release pipeline is a GitHub Actions workflow triggered on a `vX.Y.Z` tag or a published GitHub Release. It runs the full check, builds a signed APK, and attaches it to the release.

See [`docs/RELEASE.md`](./docs/RELEASE.md) for:

- How to generate the release keystore (`foodario-release.jks`).
- How to set the CI secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- How to cut a release (push a `v*` tag or publish a GitHub Release).
- How to install on an emulator and handle debug↔release signature mismatches.

The version code is derived from the SemVer tag as `MAJOR * 10000 + MINOR * 100 + PATCH`, so each component must stay below 100.

---

## Documentation

| File                                             | What's in it                                                                                    |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------|
| [`AGENTS.md`](./AGENTS.md)                       | Guide for AI coding agents working in this repo (build, test, DI, SQLDelight, release gotchas). |
| [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) | Architecture rules, layers, current stack, source-set layout.                                   |
| [`docs/DESIGN.md`](./docs/DESIGN.md)             | Design system — typography, colors, components, layout.                                         |
| [`docs/RELEASE.md`](./docs/RELEASE.md)           | Full release / signing / install guide.                                                         |

---

## License

Personal project — no license declared. Add one if you intend to share the source.
