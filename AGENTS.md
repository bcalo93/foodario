# AGENTS.md

> Foodario is a Kotlin Multiplatform "fridge/pantry" app: shared UI + domain + SQLDelight persistence for Android and iOS, with Koin DI and Compose Multiplatform.

## Module layout

- `androidApp/` — Android shell only: `FoodarioApplication` (initializes Koin), `MainActivity` (mounts `App()`).
- `iosApp/` — Xcode project (`iosApp.xcodeproj`). `iosApp/ContentView.swift` calls `MainViewControllerKt.MainViewController()` from the shared framework.
- `shared/` — All business logic, UI, ViewModels, use cases, repositories, and SQLDelight schema. Sub-sources: `commonMain`, `androidMain`, `iosMain`, `commonTest`, `androidHostTest`, `iosTest`.
- `docs/` — `ARCHITECTURE.md` (layer rules, current stack) and `DESIGN.md` are the source of truth for conventions. Read them before adding a new feature.

Rule of thumb: new code goes in `shared/`. `androidApp/` and `iosApp/` only host the entry point.

## Toolchain & Gradle

- Toolchain pins live in `gradle/libs.versions.toml` — consult it for the current major lines (Kotlin 2.x, AGP 9.x, Compose Multiplatform 1.x, Koin 4.x, SQLDelight 2.x, etc.) rather than trusting any prose version you see around.
- JVM target `JVM_11` in both modules. CI builds with JDK 17; the daemon toolchain uses Azul JDK 21.
- `gradle.properties` enables configuration cache and build cache. Avoid non-cache-safe Gradle code (no `Project` reads in tasks, etc.).
- `appVersionName` and `appVersionCode` are required Gradle properties — defaults live in `gradle.properties`. The release workflow overrides them with `-PappVersionName=<v> -PappVersionCode=<code>`.
- `local.properties` (with `sdk.dir=...`) is gitignored and must exist locally for Android builds.

## Build & run

- Android debug: `./gradlew :androidApp:assembleDebug`
- Android release APK (requires `keystore.properties` at repo root): `./gradlew :androidApp:assembleRelease` — output at `androidApp/build/outputs/apk/release/androidApp-release.apk`.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run there. Bundle id template `com.foodario.Foodario$(TEAM_ID)` — set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` for real builds.
- No `lint`, `detekt`, `ktlint`, or `spotless` is configured. Verification is just `./gradlew :check`.

## Tests

Run a focused test, not the whole suite:

- Android host tests (most unit tests live here, with MockK + an in-memory SQLDelight driver):
  `./gradlew :shared:testAndroidHostTest`
- iOS tests:
  `./gradlew :shared:iosSimulatorArm64Test`
- Pure-JVM `commonTest`:
  `./gradlew :shared:test`
- CI runs `./gradlew :androidApp:check :shared:check` before building the release APK.

Tests assemble a Koin graph manually (`shared/src/androidHostTest/.../KoinGraphTest.kt` is the template) and use the `sqlite` driver plus the `TestHelpers.kt` factories — copy that pattern for new use cases/repos.

## Where new code lives

Inside `shared/src/commonMain/kotlin/com/foodario/`:

- `<feature>/data/` — repositories + SQLDelight mappers.
- `<feature>/domain/model/` — plain Kotlin data classes / enums.
- `<feature>/domain/repository/` — repository interfaces.
- `<feature>/domain/usecase/` — `UseCase<P, R>` / `ObserveUseCase<P, R>` impls (see `core/domain/usecase/UseCase.kt`).
- `<feature>/presentation/` — `*ViewModel`, `*UiState`, `*Screen` Composable.
- `<feature>/presentation/uicomponents/` — sub-composables (e.g. dialogs).
- `core/presentation/components/`, `core/presentation/theme/` — shared design system (notebook/skeuomorphic theme, fonts in `shared/src/commonMain/composeResources/font/`).
- `navigation/` — `FoodarioNavHost` with type-safe routes (`@Serializable` `InventoryRoute`, `ShoppingListRoute`, `FoodDetailRoute`).
- `di/` — `DataModule`, `DomainModule`, `PresentationModule`, `SharedModules`. Platform Koin init lives in `androidMain/.../di/Koin.android.kt` and `iosMain/.../di/Koin.ios.kt`; both call `sharedModules()`.

Cross-cutting wiring notes:

- Use cases are registered in `DomainModule.kt` with `named("...")` qualifiers; ViewModels in `PresentationModule.kt` resolve them via `get(named("..."))`. Don't introduce a different DI convention.
- `DatabaseDriverFactory` is `expect` in `commonMain` with `actual` in `androidMain` (AndroidSqliteDriver, db name `foodario.db`) and `iosMain` (native driver). Add new platform bits the same way.

## SQLDelight

- Schema files: `shared/src/commonMain/sqldelight/com/foodario/database/*.sq` (`FoodItem.sq`, `ShoppingItem.sq`).
- Generated package: `com.foodario.database` (see `sqldelight { databases { create("FoodarioDatabase") { packageName.set("com.foodario.database") } } }`).
- After editing a `.sq`, the Gradle plugin regenerates Kotlin on next build. If IntelliJ flags missing generated types, run `./gradlew :shared:generateDebugFoodarioDatabaseInterface` (or any `:shared` task that triggers codegen).
- Timestamps are stored as `Long` epoch millis; map to/from `kotlinx.datetime.LocalDate` / `kotlin.time.Instant` in the `*Mapper.kt` of each feature.

## Release / signing

See `docs/RELEASE.md` for the full guide. Highlights agents get wrong:

- The release workflow (`.github/workflows/release.yml`) triggers on a tag `vX.Y.Z` (annotated) or a published GitHub release. It derives `appVersionCode = MAJOR*10000 + MINOR*100 + PATCH`, so versions must stay SemVer and below 100 for each component.
- CI secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Local builds use `keystore.properties` at the repo root or env vars `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- `foodario-release.jks`, `*.jks.b64`, and `keystore.properties` are gitignored. If `git status` shows them as untracked, leave them alone — never `git add` them.
- If a release APK was previously installed with the debug key, you must `adb uninstall com.foodario` before installing the release build on the same emulator/device (signature mismatch).
- Verify signatures with `apksigner verify --verbose <apk>` (APK Signature Scheme v2), not `jarsigner`.

## Things that are easy to miss

- iOS targets are only `iosArm64` + `iosSimulatorArm64`. There is no `macosX*`, `linuxX*`, or JS target — don't try to add one without updating `settings.gradle.kts` and providing new source sets.
- The `iosApp` xcodeproj references the `Shared` framework produced by `shared/build.gradle.kts`. After `:shared` changes that touch the public API, Xcode must relink — re-run the Xcode scheme.
- `App()` in `shared/.../com/foodario/App.kt` is the only Composable mounted by both platforms; routing lives in `FoodarioNavHost`.
- Strings shown in the UI (e.g. `"Heladera"`, `"Compras"`) are hardcoded in `FoodarioNavHost.kt`, not in resources. If you add another top-level tab, update both the bottom bar and the route list.
