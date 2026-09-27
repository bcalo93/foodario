# Architecture — Foodario

> Defines the project structure, layers, tech stack, and conventions.

---

## 1. Overview

Foodario is a **"digital pantry/fridge"** that answers one concrete question: *"do I already have this at home?"*. The app is **offline-first**: all data lives on the device, with no backend in the current scope.

The three main domain actions:

| Action               | Description                                   |
|----------------------|-----------------------------------------------|
| ➕ **I have**         | Add a food item to the inventory              |
| ➖ **I used**         | Reduce / remove a quantity from the inventory |
| 🛒 **I need to buy** | Shopping list, separate from the inventory    |

Natural flow: `Shop → add to the fridge → consume → inventory updates → when it runs out, move it to the shopping list`.

---

## 2. Tech stack

| Concern | Technology | Reason |
|---|---|---|
| Multiplatform | **Kotlin Multiplatform** (Kotlin 2.x, AGP 9.x) | One codebase for Android and iOS |
| UI | **Compose Multiplatform** 1.x (Material 3 + Navigation Compose type-safe) | Declarative UI shared in `commonMain` |
| Persistence | **SQLDelight** 2.x | Type-safe local DB, multiplatform, generates Kotlin code from SQL |
| Dependency injection | **Koin** 4.x | Lightweight DI, multiplatform, no kapt/heavy codegen |
| Async | **Kotlin Coroutines + Flow** | Reactive streams from the DB to the UI |
| ViewModel | **androidx.lifecycle ViewModel (KMP)** | Multiplatform ViewModels via `viewmodel-compose` |
| Compose resources | `compose.components-resources` (bundled fonts) | Caveat + Nunito offline; no runtime downloads |
| Dates | `kotlinx-datetime` | `LocalDate` / `Instant` in domain; epoch millis in DB |
| Navigation | `androidx.navigation:navigation-compose` (KMP) | Type-safe routes (`@Serializable data object/class`) |
| Unit tests | `kotlin.test` + MockK | MockK lets you mock per layer for isolated tests |
| Flow tests | **Turbine** | Assertions on `StateFlow`/`Flow` in ViewModels and repositories |
| Coroutine tests | `kotlinx-coroutines-test` | `runTest`, test dispatchers |

> Exact versions and library aliases live in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) — that's the single source of truth. Don't duplicate them elsewhere; this document only lists the **major lines** to avoid going stale.

---

## 3. Module structure

The project keeps the current **3-module** structure:

```
foodario/
├── androidApp/     # Android shell (Activity, Application, entry point)
├── iosApp/         # iOS shell (Xcode project, entry point)
└── shared/         # All logic: UI, domain and data (KMP)
```

**Rule:** all business, presentation and persistence logic lives in `shared`. The app modules only initialize Koin and mount the root Composable.

### `shared` source sets

```
shared/src/
├── commonMain/       # UI (Compose), ViewModels, UseCases, Repositories, domain models, SQLDelight (.sq)
├── androidMain/      # Android SQLDelight driver + Android Koin init
├── iosMain/          # iOS SQLDelight driver + iOS Koin init
├── commonTest/       # Pure-JVM tests: mappers, models, theme helpers (no mockk/koin-test)
├── androidHostTest/  # Tests with MockK + manual Koin + JdbcSqliteDriver IN_MEMORY. Lives the ViewModel, UseCase and repository tests.
└── iosTest/          # Native iOS tests (smoke-test placeholder for now)
```

---

## 4. Clean Architecture — layers and dependency rules

### 4.1 Layer diagram

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION                        │
│   Composables (Screens, Components) → ViewModels     │
│   State: StateFlow<UiState> — unidirectional UDF     │
├─────────────────────────────────────────────────────┤
│                    DOMAIN                            │
│   UseCases · Domain models · Repository (ports)      │
│   Pure Kotlin, no framework dependencies             │
├─────────────────────────────────────────────────────┤
│                     DATA                             │
│   Repository impl · SQLDelight DAOs · Mappers        │
│   Single source of truth: local DB                   │
└─────────────────────────────────────────────────────┘
```

### 4.2 Dependency rule

**Dependencies always point inward:**

- `Presentation → Domain ← Data`
- **Domain knows nobody:** not Compose, not SQLDelight, not Koin. Only pure Kotlin + coroutines.
- **Data depends on Domain:** implements the interfaces (ports) Domain defines.
- **Presentation depends on Domain:** ViewModels consume UseCases, never repositories directly.
- Composition (wiring) is resolved in the **DI layer (Koin)**, which is allowed to know all layers.

### 4.3 Responsibility per layer

| Layer | Responsibility | Must NOT |
|---|---|---|
| **Composable** | Render `UiState`, emit user events | Business logic, direct data access |
| **ViewModel** | Orchestrate UseCases, expose `StateFlow<UiState>`, handle events | SQL, direct navigation to the DB |
| **UseCase** | A single business operation, combine repositories | Know about UI, SQLDelight |
| **Repository (interface)** | Data-access contract in Domain | — |
| **Repository (impl)** | Implement the contract against SQLDelight, map DB ↔ domain | Business logic |
| **DAO / Queries** | SQL generated by SQLDelight | Know about ViewModels |

### 4.4 UseCase contract (pattern borrowed from SplitIt)

Every UseCase implements a **common generic interface**, following the pattern of the sibling project [SplitIt](../../SplitIt/composeApp/src/commonMain/kotlin/com/splitit/domain/usecase/UseCase.kt):

```kotlin
// core/domain/usecase/UseCase.kt
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface ObserveUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}
```

- **`P`**: immutable input DTO. Each UseCase defines its own `data class XxxParams`; those that don't need params use `object XxxParams` (SplitIt convention: `object ObserveGroupsParams`).
- **`R`**: return type (domain model or `Unit`).
- **`UseCase<P, R>`**: action operations — single-shot, `suspend`.
- **`ObserveUseCase<P, R>`**: reactive streams over the DB. This is the extension of the pattern Foodario needs because it is Flow-based (SQLDelight `asFlow`); SplitIt only uses `suspend` + manual refresh in the ViewModel.

Example:

```kotlin
data class AddFoodItemParams(
    val name: String,
    val category: FoodCategory,
    val quantity: Double = 1.0,                 // quick add: "Milk → +"
    val unit: QuantityUnit = QuantityUnit.UNIT,
    val isFrozen: Boolean = false,
    val expirationDate: LocalDate? = null,
)

class AddFoodItemUseCase(
    private val repository: InventoryRepository,
) : UseCase<AddFoodItemParams, FoodItem> {
    override suspend fun invoke(params: AddFoodItemParams): FoodItem {
        require(params.name.isNotBlank()) { "Name must not be blank" }
        require(params.quantity > 0) { "Quantity must be positive" }
        return repository.add(params.toFoodItem())
    }
}

data class ObserveInventoryParams(
    val query: String = "",
    val category: FoodCategory? = null,
)

class ObserveInventoryUseCase(
    private val repository: InventoryRepository,
) : ObserveUseCase<ObserveInventoryParams, List<FoodItem>> {
    override fun invoke(params: ObserveInventoryParams): Flow<List<FoodItem>> =
        repository.observeInventory(params.query, params.category)
}
```

**Pattern rules:**

1. **ViewModels depend on the generic type** (`UseCase<P, R>` / `ObserveUseCase<P, R>`), not on the concrete class — same as in SplitIt (`private val observeGroupDetails: UseCase<ObserveGroupDetailsParams, GroupDetails>`). This makes MockK mocks trivial and decouples presentation from the domain implementation.
2. The UseCases of a feature are **grouped in a single file** with their `Params` (`InventoryUseCases.kt`, `ShoppingListUseCases.kt`), following the SplitIt convention (`GroupUseCases.kt`, `ExpenseUseCases.kt`). `Params` that don't need fields live as `object` (`object ObserveShoppingListParams`).
3. Input validation lives inside `invoke` with `require` / `requireNotNull`, like in SplitIt.
4. Every UseCase registered in `DomainModule.kt` uses `named("…")` as a qualifier and the ViewModel resolves it with `get(named("…"))` (see §9).

---

## 5. Package structure (`commonMain`)

Organized **by feature**, with the layers inside each feature. This scales better than layer-based packages as features grow.

```
com.foodario/
├── di/
│   ├── SharedModules.kt          # Aggregated Koin modules
│   ├── DataModule.kt             # DB, DAOs, repositories
│   ├── DomainModule.kt           # UseCases (named("…"))
│   └── PresentationModule.kt     # ViewModels (DSL with params.get() for VMs with arguments)
│
├── core/
│   ├── domain/
│   │   └── usecase/              # UseCase.kt: contracts UseCase<P, R> + ObserveUseCase<P, R>
│   └── presentation/
│       ├── theme/                # FoodarioTheme, Color, Type, CategoryColors, Dimensions, ThemePreview
│       ├── components/           # Reusable Composables + their @Previews
│       └── preview/              # PreviewUseCases.kt: fake data for shared previews
│
├── inventory/                    # Feature: inventory (fridge)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FoodItem.kt
│   │   │   ├── FoodCategory.kt  # enum with displayName
│   │   │   └── QuantityUnit.kt  # enum with displayName + step; helpers snapToStep / formatQuantityNumber
│   │   ├── repository/
│   │   │   └── InventoryRepository.kt
│   │   └── usecase/
│   │       ├── InventoryUseCases.kt         # Add, UpdateQuantity/Category/Unit/Expiration, Consume, Delete, ToggleFrozen (+ Params)
│   │       ├── ObserveInventoryUseCase.kt   # Stream with search / category
│   │       └── ObserveFoodItemUseCase.kt    # Stream by id → FoodDetailViewModel
│   ├── data/
│   │   ├── InventoryRepositoryImpl.kt
│   │   └── FoodItemMapper.kt
│   └── presentation/
│       ├── InventoryScreen.kt + InventoryScreenPreview.kt
│       ├── InventoryViewModel.kt
│       ├── InventoryUiState.kt
│       ├── FoodDetailScreen.kt + FoodDetailScreenPreview.kt
│       ├── FoodDetailViewModel.kt + FoodDetailDateFormatting.kt
│       ├── FoodDetailUiState.kt
│       └── uicomponents/
│           ├── InventoryContent.kt           # screen orchestrator: list + QuickAddBar + AddItemFlightOverlay
│           ├── InventoryItemRow.kt           # row with inline quantityStepper
│           ├── InventorySearchField.kt
│           ├── AddItemFlightOverlay.kt       # animation on add (CONCEPT §3.5)
│           ├── FoodDetailContent.kt
│           ├── FoodDetailEditDialogs.kt      # inline editing of quantity / unit / category
│           ├── FoodDetailConfirmationDialogs.kt  # delete confirmation
│           └── FoodDetailExpirationDialog.kt
│
├── shoppinglist/                 # Feature: shopping list
│   ├── domain/
│   │   ├── model/
│   │   │   └── ShoppingItem.kt
│   │   ├── repository/
│   │   │   └── ShoppingListRepository.kt
│   │   └── usecase/
│   │       ├── ShoppingListUseCases.kt       # Add, Remove, MoveToInventory (+ Params)
│   │       └── ObserveShoppingListUseCase.kt
│   ├── data/
│   │   ├── ShoppingListRepositoryImpl.kt
│   │   └── ShoppingItemMapper.kt
│   └── presentation/
│       ├── ShoppingListScreen.kt + ShoppingListScreenPreview.kt
│       ├── ShoppingListViewModel.kt          # holds local checkedIds + QuickAdd + addCategory
│       ├── ShoppingListUiState.kt
│       └── uicomponents/
│           ├── ShoppingListContent.kt
│           └── ShoppingListItemRow.kt        # hand-drawn check (HandDrawnCheckbox) + "Move to fridge"
│
└── navigation/                   # Routes and nav graph
    └── FoodarioNavHost.kt        # InventoryRoute, ShoppingListRoute, FoodDetailRoute(@Serializable)
```

**Conventions:**

- Every UseCase implements `UseCase<P, R>` (action, `suspend`) or `ObserveUseCase<P, R>` (reactive stream) — see §4.4.
- Each UseCase defines its `Params` DTO in the same file; parameter-less cases use `object XxxParams` (`ObserveShoppingListParams`).
- A feature's UseCases are grouped in a single file (`InventoryUseCases.kt`, `ShoppingListUseCases.kt`), like in SplitIt.
- Mappers are pure extension functions (`FoodItemEntity.toDomain()`, `FoodItem.toEntity()`).
- ViewModels extend `androidx.lifecycle.ViewModel` (multiplatform) and use `viewModelScope`. They expose `uiState: StateFlow<XxxUiState>` and, when there are one-shot side effects, also a `Channel<XxxEffect>` consumed via `effects: Flow<XxxEffect>` (e.g. `InventoryEffect.ItemAdded`, `FoodDetailEffect.Deleted`).
- Events that the UI emits to the ViewModel are `sealed interface XxxEvent`, grouped in the same file as the ViewModel.
- **There is no** separate `AddFoodScreen.kt` / `AddFoodViewModel.kt`: quick add happens **inside** `InventoryScreen` (and `ShoppingListScreen` for the shopping list) using the shared `QuickAddBar` component. The category for the next "quick" add lives in the ViewModel itself (`quickAddCategory: StateFlow<FoodCategory>`), not in the UI.
- Each detail/list sub-Composable moves to its own file under `presentation/uicomponents/` once it stops being trivial.

---

## 6. Domain model

Derived directly from CONCEPT.md (MVP):

```kotlin
// inventory/domain/model/FoodItem.kt
data class FoodItem(
    val id: Long,
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val isFrozen: Boolean,
    val expirationDate: LocalDate?,   // optional
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class FoodCategory {
    DAIRY,        // 🥛 Dairy
    MEAT,         // 🥩 Meat
    VEGETABLES,   // 🥦 Vegetables
    FRUITS,       // 🍎 Fruits
    PANTRY,       // 🥫 Pantry
    FROZEN,       // 🧊 Frozen
    BEVERAGES,    // 🥤 Beverages
    COOKED,       // 🍲 Cooked food (sauces, etc.)
    OTHER,
}

enum class QuantityUnit {
    UNIT, GRAMS, KILOGRAMS, MILLILITERS, LITERS,
}

// shoppinglist/domain/model/ShoppingItem.kt
data class ShoppingItem(
    val id: Long,
    val name: String,
    val category: FoodCategory?,
    val quantity: Double?,
    val unit: QuantityUnit?,
    val createdAt: Instant,
)
```

**Design notes:**

- `quantity` is `Double` to support both integer units (8 eggs) and weights/volumes (0.3 kg).
- `expirationDate` is nullable: the MVP marks it optional.
- `isFrozen` is a flag on the item, independent of the `FROZEN` category (a `MEAT` item can be frozen).
- The `COOKED` category covers the concept use case: *"add cooked food like a sauce"*.

---

## 7. Persistence — SQLDelight

### 7.1 Configuration

In `shared/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("FoodarioDatabase") {
            packageName.set("com.foodario.database")
        }
    }
}
```

`.sq` files live in `commonMain/sqldelight/com/foodario/database/`.

### 7.2 Schema

```sql
-- FoodItem.sq
CREATE TABLE food_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    quantity REAL NOT NULL,
    unit TEXT NOT NULL,
    is_frozen INTEGER NOT NULL DEFAULT 0,
    expiration_date INTEGER,              -- epoch millis, nullable
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX idx_food_item_name ON food_item(name);
CREATE INDEX idx_food_item_expiration ON food_item(expiration_date);

selectAll:
SELECT * FROM food_item ORDER BY name ASC;

search:
SELECT * FROM food_item WHERE name LIKE '%' || :query || '%' ORDER BY name ASC;

selectById:
SELECT * FROM food_item WHERE id = :id;

selectByName:
SELECT * FROM food_item WHERE name = :name ORDER BY id ASC LIMIT 1;

selectExpiringBefore:
SELECT * FROM food_item
WHERE expiration_date IS NOT NULL AND expiration_date <= :threshold
ORDER BY expiration_date ASC;

insertFoodItem {
INSERT INTO food_item(name, category, quantity, unit, is_frozen, expiration_date, created_at, updated_at)
VALUES (:name, :category, :quantity, :unit, :isFrozen, :expirationDate, :createdAt, :updatedAt);
SELECT last_insert_rowid() AS id;     -- block: returns the inserted id to the repository
}

updateQuantity:
UPDATE food_item SET quantity = :quantity, updated_at = :updatedAt WHERE id = :id;

updateCategory:
UPDATE food_item SET category = :category, updated_at = :updatedAt WHERE id = :id;

updateUnit:
UPDATE food_item SET unit = :unit, updated_at = :updatedAt WHERE id = :id;

updateFrozen:
UPDATE food_item SET is_frozen = :isFrozen, updated_at = :updatedAt WHERE id = :id;

updateExpiration:
UPDATE food_item SET expiration_date = :expirationDate, updated_at = :updatedAt WHERE id = :id;

delete:
DELETE FROM food_item WHERE id = :id;
```

```sql
-- ShoppingItem.sq
CREATE TABLE shopping_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT,
    quantity REAL,
    unit TEXT,
    created_at INTEGER NOT NULL
);

selectAll:
SELECT * FROM shopping_item ORDER BY created_at DESC;

selectById:
SELECT * FROM shopping_item WHERE id = :id;

insert:
INSERT INTO shopping_item(name, category, quantity, unit, created_at)
VALUES (:name, :category, :quantity, :unit, :createdAt);

delete:
DELETE FROM shopping_item WHERE id = :id;
```

> Naming: SQL queries use *update* verbs (`updateCategory`, `updateFrozen`, etc.), but **repository** methods are `setCategory`, `setFrozen`, `setUnit`, `setExpirationDate` to reflect "field change" semantics in the domain. The mapping is 1-to-1 between `setX` (repo) and `updateX` (SQL).

### 7.3 Platform drivers (expect/actual)

```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(FoodarioDatabase.Schema, context, "foodario.db")
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(FoodarioDatabase.Schema, "foodario.db")
}
```

### 7.4 Reactivity

`app.cash.sqldelight:coroutines-extensions` is used to turn queries into `Flow`:

```kotlin
// InventoryRepository — observe the whole inventory (filtered by name and category)
override fun observeInventory(query: String, category: FoodCategory?): Flow<List<FoodItem>> {
    val flow = if (query.isBlank()) queries.selectAll().asFlow()
               else queries.search(query = query).asFlow()
    return flow.mapToList(ioDispatcher)
        .map { entities -> entities.map { it.toDomain() }.filterByCategory(category) }
}

// InventoryRepository — observe a single item by id (FoodDetailViewModel)
override fun observeById(id: Long): Flow<FoodItem?> =
    queries.selectById(id = id).asFlow().mapToOneOrNull(ioDispatcher).map { it?.toDomain() }
```

`ioDispatcher` is injected into the repository (`CoroutineDispatcher = Dispatchers.IO` by default) so it can be substituted in tests. The category filter happens in memory (`filterByCategory`); the `search` query does it in SQL and already comes back ordered by `name ASC`.

The database is the **single source of truth**: any `insert`/`update`/`delete` automatically emits toward the UI through these Flows. There are no in-memory caches or duplicated states.

---

## 8. Data flow (UDF)

Unidirectional flow throughout the app:

```
User → Composable (event) → ViewModel → UseCase → Repository → SQLDelight
                                                                            │
Composable (re-render) ← UiState ← StateFlow ← Flow ← Updated query ←────────┘
```

**Concrete example — "I used 1 milk":**

1. `InventoryScreen` emits `InventoryEvent.DecreaseQuantity(itemId)`.
2. `InventoryViewModel` calls `updateQuantity(UpdateQuantityParams(itemId, newQuantity))`.
3. The UseCase validates (quantity ≥ 0; if it reaches 0, suggest moving to shopping) and delegates to `InventoryRepository`.
4. The repository executes `updateQuantity` on SQLDelight.
5. SQLDelight invalidates the `selectAll` query → the `Flow` emits the new list.
6. The ViewModel transforms it into `InventoryUiState` and the UI re-renders.

### UiState per screen

```kotlin
data class InventoryUiState(
    val items: List<FoodItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

A single `StateFlow<InventoryUiState>` per ViewModel. Events are `sealed interface`s (`InventoryEvent`).

---

## 9. Dependency injection — Koin

### 9.1 Module organization

One Koin module per layer, aggregated in `SharedModules.kt`:

```kotlin
// di/DataModule.kt
val dataModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { FoodarioDatabase(get()) }
    single { get<FoodarioDatabase>().foodItemQueries }
    single { get<FoodarioDatabase>().shoppingItemQueries }
    single<InventoryRepository> { InventoryRepositoryImpl(get()) }
    single<ShoppingListRepository> { ShoppingListRepositoryImpl(get()) }
}

// di/DomainModule.kt
val domainModule = module {
    // Bindings are declared with named("…") + the explicit generic type
    // so that ViewModels depend on the interface, not the concrete class.
    // Inventory
    factory<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>(named("observeInventory")) {
        ObserveInventoryUseCase(get())
    }
    factory<ObserveUseCase<ObserveFoodItemParams, FoodItem?>>(named("observeFoodItem")) {
        ObserveFoodItemUseCase(get())
    }
    factory<UseCase<AddFoodItemParams, FoodItem>>(named("addFoodItem"))              { AddFoodItemUseCase(get()) }
    factory<UseCase<UpdateQuantityParams, Unit>>(named("updateQuantity"))           { UpdateQuantityUseCase(get()) }
    factory<UseCase<UpdateCategoryParams, Unit>>(named("updateCategory"))            { UpdateCategoryUseCase(get()) }
    factory<UseCase<UpdateUnitParams, Unit>>(named("updateUnit"))                    { UpdateUnitUseCase(get()) }
    factory<UseCase<UpdateExpirationParams, Unit>>(named("updateExpiration"))       { UpdateExpirationUseCase(get()) }
    factory<UseCase<ConsumeFoodItemParams, Unit>>(named("consumeFoodItem"))          { ConsumeFoodItemUseCase(get()) }
    factory<UseCase<DeleteFoodItemParams, Unit>>(named("deleteFoodItem"))            { DeleteFoodItemUseCase(get()) }
    factory<UseCase<ToggleFrozenParams, Unit>>(named("toggleFrozen"))                { ToggleFrozenUseCase(get()) }
    // Shopping list
    factory<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>>(named("observeShoppingList")) {
        ObserveShoppingListUseCase(get())
    }
    factory<UseCase<AddToShoppingListParams, Unit>>(named("addToShoppingList"))       { AddToShoppingListUseCase(get()) }
    factory<UseCase<RemoveFromShoppingListParams, Unit>>(named("removeFromShoppingList")) {
        RemoveFromShoppingListUseCase(get())
    }
    factory<UseCase<MoveToInventoryParams, FoodItem>>(named("moveToInventory"))      { MoveToInventoryUseCase(get(), get()) }
}

// di/PresentationModule.kt
val presentationModule = module {
    viewModel {
        InventoryViewModel(
            observeInventory = get(named("observeInventory")),
            addFoodItem      = get(named("addFoodItem")),
            updateQuantity   = get(named("updateQuantity")),
            deleteFoodItem   = get(named("deleteFoodItem")),
        )
    }
    viewModel { params ->                                 // params.get() consumes the itemId
        FoodDetailViewModel(
            itemId            = params.get(),
            observeFoodItem   = get(named("observeFoodItem")),
            updateQuantity    = get(named("updateQuantity")),
            updateCategory    = get(named("updateCategory")),
            updateUnit        = get(named("updateUnit")),
            toggleFrozen      = get(named("toggleFrozen")),
            updateExpiration  = get(named("updateExpiration")),
            deleteFoodItem    = get(named("deleteFoodItem")),
            addToShoppingList = get(named("addToShoppingList")),
        )
    }
    viewModel {
        ShoppingListViewModel(
            observeShoppingList    = get(named("observeShoppingList")),
            moveToInventory        = get(named("moveToInventory")),
            addToShoppingList      = get(named("addToShoppingList")),
            removeFromShoppingList = get(named("removeFromShoppingList")),
        )
    }
}
```

### 9.2 Platform initialization

- **Android:** `FoodarioApplication : Application` in `androidApp` calls `startKoin` with `androidContext()` + the platform module (`DatabaseDriverFactory(context)`).
- **iOS:** the `initKoin()` function in `iosMain` is called from `iosApp` (Swift) on startup; it provides the native `DatabaseDriverFactory`.

```kotlin
// commonMain — di/SharedModules.kt
fun sharedModules() = listOf(dataModule, domainModule, presentationModule)

// androidMain
fun initKoin(context: Context) = startKoin {
    androidContext(context)
    modules(platformModule() + sharedModules())
}

// iosMain
fun initKoin() = startKoin {
    modules(platformModule() + sharedModules())
}
```

### 9.3 Resolution in the UI

ViewModels are resolved in Compose with `koin-compose-viewmodel`:

```kotlin
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = koinViewModel()) { ... }
```

---

## 10. Testing strategy — MockK per layer

**Guiding principle:** each test exercises **a single layer**, mocking its collaborators with MockK. No tests cross layers in the unit suite.

### 10.1 Testing matrix

| Layer | Subject under test | Mocked | Verified |
|---|---|---|---|
| **Data** | `InventoryRepositoryImpl` | `FoodItemQueries` (SQLDelight) — actually: real in-memory sqlite | Correct mapping, query calls, flow propagation |
| **Domain** | `AddFoodItemUseCase` | `InventoryRepository` | Validations, transformations, delegation to the repo |
| **Presentation** | `InventoryViewModel` | UseCases | Correct `UiState` per event, error handling |

### 10.2 Test dependencies (per source set)

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.turbine)
    implementation(libs.kotlinx.coroutines.test)
}

getByName("androidHostTest").dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.mockk)                 // per-layer mocks (unit)
    implementation(libs.koin.test)             // Koin graph verification
    implementation(libs.sqldelight.sqlite.driver)  // JdbcSqliteDriver IN_MEMORY
}
```

`mockk` and `koin-test` live in `androidHostTest`, **not** in `commonTest`: the goal is to keep `commonTest` pure-JVM with no Android dependencies or heavy mocks. ViewModel tests that need `MockK` live in `androidHostTest` even when they only touch `ViewModel` + `UseCases`.

### 10.3 Examples per layer

**Data — repository with a real in-memory SQLite driver:**

The data-layer tests **do not mock** the queries: they spin up a `JdbcSqliteDriver(IN_MEMORY)` with the real schema and verify mapping + flow + flows. Repositories are only mocked when testing a higher layer.

```kotlin
class InventoryRepositoryImplTest {
    private fun inMemoryRepository(): InventoryRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FoodarioDatabase.Schema.create(driver)
        val database = FoodarioDatabase(driver)
        return InventoryRepositoryImpl(database.foodItemQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `insert returns id and preserves domain`() = runTest {
        val repository = inMemoryRepository()
        val created = repository.add(foodItem(name = "Milk"))
        assertTrue(created.id > 0L)
        val fetched = repository.getById(created.id)
        assertEquals("Milk", fetched?.name)
    }
}
```

**Domain — UseCase with a mocked repository:**

```kotlin
class UpdateQuantityUseCaseTest {
    private val repository = mockk<InventoryRepository>()
    private val useCase = UpdateQuantityUseCase(repository)

    @Test
    fun `rejects negative quantity`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase(UpdateQuantityParams(itemId = 1, newQuantity = -1.0))
        }
        coVerify(exactly = 0) { repository.updateQuantity(any(), any()) }
    }
}
```

**Presentation — ViewModel with mocked UseCases + Turbine:**

```kotlin
class InventoryViewModelTest {
    // The generic interface is mocked, not the concrete class
    private val observeInventory =
        mockk<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>()
    private lateinit var viewModel: InventoryViewModel

    @Test
    fun `emits items in ui state`() = runTest {
        every { observeInventory(ObserveInventoryParams()) } returns flowOf(listOf(milk, eggs))
        viewModel = InventoryViewModel(observeInventory, ...)

        viewModel.uiState.test {
            assertEquals(listOf(milk, eggs), awaitItem().items)
        }
    }
}
```

### 10.4 Testing rules

1. **One test, one layer.** If a ViewModel test needs SQLDelight, it's mis-designed.
2. **MockK for collaborators, fakes only when the mock would be more complex than the fake** (e.g. a `FakeRepository` in UseCase tests with complex combination logic).
3. **Turbine for every Flow** exposed to the UI.
4. **`runTest` + `StandardTestDispatcher`** in ViewModels; inject dispatchers if needed (see §11).
5. **Real-DB tests:** repository tests use `JdbcSqliteDriver(IN_MEMORY)` with `FoodarioDatabase.Schema.create(driver)` and verify mapping, mutations and flows against real SQLite (no mocks). See `androidHostTest/.../InventoryRepositoryImplTest.kt` as the template.
6. **Koin graph verification:** `koin.test` (`androidHostTest/.../KoinGraphTest.kt`) assembles the graph by hand and uses `verify()` to catch missing bindings / Qualifier Mismatch before CI.
7. **`runTest` + `Dispatchers.Unconfined`:** repository tests inject `Dispatchers.Unconfined` instead of `Dispatchers.IO` so they don't depend on real dispatchers.

---

## 11. Cross-cutting conventions

- **Dispatchers:** repositories take `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` by constructor (default in production, `Unconfined` in tests). SQLDelight queries are wrapped with `mapToList(ioDispatcher)` or `withContext(ioDispatcher)`.
- **Errors:** UseCases validate with `require` / `requireNotNull` (SplitIt convention); the ViewModel wraps each call in `runCatching { … }` so exceptions don't propagate into `viewModelScope`. Observation Flows use `catch { emit(UiState(error = e.message)) }`.
- **One-shot effects:** ViewModels expose `effects: Flow<XxxEffect>` via `Channel<XxxEffect>(Channel.BUFFERED)` + `receiveAsFlow()`. They carry side effects that must not survive reconfiguration (`FoodDetailEffect.Deleted`, `FoodDetailEffect.AddedToShoppingList`, `InventoryEffect.ItemAdded`). The UI consumes them with `LaunchedEffect(viewModel) { viewModel.effects.collect { … } }`.
- **Dates:** `kotlinx.datetime` (`LocalDate`, `Instant`) in domain; epoch millis (`Long`) in DB. Conversion lives in `*Mapper.kt` of each feature.
- **Navigation:** multiplatform Compose Navigation (`androidx.navigation:navigation-compose` KMP) with type-safe routes (`@Serializable data object` / `data class`). Routes live in `navigation/FoodarioNavHost.kt`. To navigate to a destination with an argument, instantiate the `data class` (`FoodDetailRoute(itemId)`).
- **UseCase naming:** verb + noun + `UseCase` (`AddFoodItemUseCase`), implementing `UseCase<P, R>` or `ObserveUseCase<P, R>`. Stream observers use the `Observe` prefix (`ObserveInventoryUseCase`, `ObserveShoppingListUseCase`). UseCases that mutate a field on an existing item use `set*` on the repo (`setCategory`, `setFrozen`, `setUnit`, `setExpirationDate`) but `update*` in the SQL and in the UseCase name (`UpdateCategoryUseCase`).
- **Touch targets and dimensions:** `FoodarioDimensions` (`xxs/xs/sm/md/lg/xl/xxl/touchTarget/rowHeight/bottomBar` in `core/presentation/theme/Dimensions.kt`) is the only reference for spacing and touch-area sizes. The "≥ 48dp" rule is enforced via `dimensions.touchTarget`.

---

## 12. Alignment with the concept roadmap

| Phase (CONCEPT.md) | Architectural impact |
|---|---|
| **MVP**: inventory, search, categories, quantity, optional expiration, shopping list | Covered by the `inventory` + `shoppinglist` features described in §5 |
| **Quick add** ("Milk → +") | `AddFoodItemUseCase` accepts only `name` with defaults (quantity 1, unit UNIT, no expiration); details are completed later. The category for the next "quick" add lives in the ViewModel (`quickAddCategory: StateFlow<FoodCategory>`) — shared by `InventoryScreen` and `ShoppingListScreen`. |
| **Phase 2**: expiration / low-stock alerts | New `alerts` feature with its own module: the `selectExpiringBefore(threshold)` query already exists in `FoodItem.sq`; only the matching `ObserveExpiringItemsUseCase` is missing. Local notifications are modeled as `expect/actual` per platform (no new Android module required). |
| **Phase 3**: receipt scanning via camera | New `scanning` feature: expect/actual for camera / ML Kit per platform, reusing `AddFoodItemUseCase` (just added) to pre-fill the parsed items. |

The per-feature separation and the dependency rule guarantee that phases 2 and 3 are added **without modifying** the existing layers — only extending them.

---

## 13. Decision summary (ADR-lite)

| # | Decision | Discarded alternative | Reason |
|---|---|---|---|
| 1 | SQLDelight | Room (Android-only), Realm | Type-safe SQL, real KMP, native Flows |
| 2 | Koin | Dagger/Hilt, Kodein | KMP-native, no codegen, low learning curve |
| 3 | MockK | Manual mocks / fakes | Real per-layer isolation with little boilerplate |
| 4 | A single `shared` module | Layer-based modules (`:domain`, `:data`) | Current MVP scale; feature-based packages already provide separation |
| 5 | DB as the single source of truth | In-memory cache | Real offline-first, fewer states to sync |
| 6 | Feature-based packages | Layer-based packages | Localizes changes, scales better with phases 2 and 3 |
| 7 | Generic `UseCase<P, R>` + `ObserveUseCase<P, R>` interface (SplitIt pattern) | Ad-hoc `invoke` per class | Uniform contract, ViewModels decoupled from concrete classes, trivial MockK mocks |
