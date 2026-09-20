# Arquitectura — Foodario

> Documento de arquitectura técnica basado en [CONCEPT.md](./CONCEPT.md).
> Define la estructura, capas, stack tecnológico y convenciones del proyecto.

---

## 1. Visión general

Foodario es una **"despensa/heladera digital"** que responde una pregunta concreta: *"¿ya tengo esto en casa?"*. La aplicación es **offline-first**: toda la información vive en el dispositivo, sin backend en el alcance actual.

Las tres acciones principales del dominio:

| Acción | Descripción |
|---|---|
| ➕ **Tengo** | Agregar alimentos al inventario |
| ➖ **Consumí** | Reducir/eliminar cantidades del inventario |
| 🛒 **Necesito comprar** | Lista de compras separada del inventario |

Flujo natural: `Comprás → agregás a la heladera → consumís → se actualiza el inventario → cuando se termina, pasa a compras`.

---

## 2. Stack tecnológico

| Concern | Tecnología | Motivo |
|---|---|---|
| Multiplataforma | **Kotlin Multiplatform** | Una sola base de código para Android e iOS |
| UI | **Compose Multiplatform** | UI declarativa compartida en `commonMain` |
| Persistencia | **SQLDelight** | Base de datos local type-safe, multiplataforma, genera código Kotlin a partir de SQL |
| Inyección de dependencias | **Koin** | DI liviana, multiplataforma, sin kapt/codegen pesado |
| Asincronía | **Kotlin Coroutines + Flow** | Streams reactivos desde la DB hasta la UI |
| ViewModel | **androidx.lifecycle ViewModel (KMP)** | ViewModels multiplataforma con `viewmodel-compose` |
| Tests unitarios | **kotlin.test + MockK** | MockK permite mockear en cada capa para tests aislados |
| Tests de Flow | **Turbine** | Aserciones sobre `StateFlow`/`Flow` en ViewModels y repositorios |
| Coroutines en tests | **kotlinx-coroutines-test** | `runTest`, dispatchers de test |

### Versiones de referencia (a agregar en `gradle/libs.versions.toml`)

```toml
[versions]
sqldelight = "2.1.0"
koin = "4.1.1"
mockk = "1.14.6"
turbine = "1.2.1"
kotlinx-coroutines = "1.10.2"

[libraries]
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

[plugins]
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

---

## 3. Estructura de módulos

El proyecto mantiene la estructura actual de **3 módulos**:

```
foodario/
├── androidApp/     # Shell Android (Activity, Application, entry point)
├── iosApp/         # Shell iOS (Xcode project, entry point)
└── shared/         # Toda la lógica: UI, dominio y datos (KMP)
```

**Regla:** toda la lógica de negocio, presentación y persistencia vive en `shared`. Los módulos de app solo inicializan Koin y montan el Composable raíz.

### Source sets de `shared`

```
shared/src/
├── commonMain/       # UI (Compose), ViewModels, UseCases, Repositorios, modelo de dominio, SQLDelight (.sq)
├── androidMain/      # Driver SQLDelight Android, inicialización Koin Android
├── iosMain/          # Driver SQLDelight iOS, inicialización Koin iOS
├── commonTest/       # Tests unitarios de las 3 capas (con MockK)
├── androidHostTest/  # Tests Android específicos (driver, instrumentación ligera)
└── iosTest/          # Tests iOS específicos
```

---

## 4. Clean Architecture — capas y reglas de dependencia

### 4.1 Diagrama de capas

```
┌─────────────────────────────────────────────────────┐
│                  PRESENTATION                        │
│   Composables (Screens, Components) → ViewModels     │
│   Estado: StateFlow<UiState> — UDF unidireccional    │
├─────────────────────────────────────────────────────┤
│                    DOMAIN                            │
│   UseCases · Modelos de dominio · Repository (ports) │
│   Kotlin puro, sin dependencias de framework         │
├─────────────────────────────────────────────────────┤
│                     DATA                             │
│   Repository impl · SQLDelight DAOs · Mappers        │
│   Única fuente de verdad: base de datos local        │
└─────────────────────────────────────────────────────┘
```

### 4.2 Regla de dependencia

**Las dependencias apuntan siempre hacia adentro:**

- `Presentation → Domain ← Data`
- **Domain no conoce a nadie**: ni Compose, ni SQLDelight, ni Koin. Solo Kotlin puro + coroutines.
- **Data depende de Domain**: implementa las interfaces (ports) que Domain define.
- **Presentation depende de Domain**: los ViewModels consumen UseCases, nunca repositorios directamente.
- La composición (wiring) se resuelve en el **DI layer (Koin)**, que sí conoce todas las capas.

### 4.3 Responsabilidad por capa

| Capa | Responsabilidad | NO debe |
|---|---|---|
| **Composable** | Renderizar `UiState`, emitir eventos de usuario | Lógica de negocio, acceso a datos |
| **ViewModel** | Orquestar UseCases, exponer `StateFlow<UiState>`, manejar eventos | SQL, navegación directa a DB |
| **UseCase** | Una única operación de negocio, combinar repositorios | Conocer la UI, SQLDelight |
| **Repository (interface)** | Contrato de acceso a datos en Domain | — |
| **Repository (impl)** | Implementar el contrato contra SQLDelight, mapear DB ↔ dominio | Lógica de negocio |
| **DAO / Queries** | SQL generado por SQLDelight | Conocer ViewModels |

### 4.4 Contrato de casos de uso (patrón adoptado de SplitIt)

Todos los casos de uso implementan una **interfaz genérica común**, siguiendo el patrón del proyecto hermano [SplitIt](../../SplitIt/composeApp/src/commonMain/kotlin/com/splitit/domain/usecase/UseCase.kt):

```kotlin
// core/domain/usecase/UseCase.kt
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface ObserveUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}
```

- **`P`**: DTO inmutable de entrada. Cada caso de uso define su propio `data class XxxParams`; los que no requieren parámetros usan `object XxxParams` (convención de SplitIt: `object ObserveGroupsParams`).
- **`R`**: tipo de retorno (modelo de dominio o `Unit`).
- **`UseCase<P, R>`**: operaciones de acción — un solo disparo, `suspend`.
- **`ObserveUseCase<P, R>`**: streams reactivos sobre la DB. Es la extensión del patrón necesaria porque Foodario es Flow-based (SQLDelight `asFlow`); SplitIt usa solo `suspend` + refresh manual en el ViewModel.

Ejemplo:

```kotlin
data class AddFoodItemParams(
    val name: String,
    val category: FoodCategory,
    val quantity: Double = 1.0,                 // carga rápida: "Leche → +"
    val unit: QuantityUnit = QuantityUnit.UNIT,
    val isFrozen: Boolean = false,
    val expirationDate: LocalDate? = null,
)

class AddFoodItemUseCase(
    private val repository: InventoryRepository,
) : UseCase<AddFoodItemParams, FoodItem> {
    override suspend fun invoke(params: AddFoodItemParams): FoodItem {
        require(params.name.isNotBlank()) { "El nombre no puede estar vacío" }
        require(params.quantity > 0) { "La cantidad debe ser positiva" }
        return repository.add(params.toDomain())
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

**Reglas del patrón:**

1. Los **ViewModels dependen del tipo genérico** (`UseCase<P, R>` / `ObserveUseCase<P, R>`), no de la clase concreta — igual que en SplitIt (`private val observeGroupDetails: UseCase<ObserveGroupDetailsParams, GroupDetails>`). Esto hace los mocks de MockK triviales y desacopla la presentación de la implementación del dominio.
2. Los casos de uso de un feature se **agrupan en un solo archivo** con sus `Params` (`InventoryUseCases.kt`), siguiendo la convención de SplitIt (`GroupUseCases.kt`, `ExpenseUseCases.kt`).
3. La validación de entrada vive dentro del `invoke` con `require`/`requireNotNull`, como en SplitIt.

---

## 5. Estructura de paquetes (`commonMain`)

Organización **por feature**, con las capas dentro de cada feature. Esto escala mejor que paquetes por capa cuando crezcan las funcionalidades.

```
com.foodario/
├── di/
│   ├── SharedModules.kt          # Agregado de módulos Koin
│   ├── DataModule.kt             # DB, DAOs, repositorios
│   ├── DomainModule.kt           # UseCases
│   └── PresentationModule.kt     # ViewModels
│
├── core/
│   ├── domain/
│   │   ├── usecase/              # UseCase.kt: contratos UseCase<P, R> + ObserveUseCase<P, R>
│   │   └── ...                   # Result, errores de dominio, modelos compartidos
│   └── presentation/             # Componentes Compose reutilizables, tema, UiText
│
├── inventory/                    # Feature: inventario (heladera)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── FoodItem.kt
│   │   │   ├── FoodCategory.kt
│   │   │   └── QuantityUnit.kt
│   │   ├── repository/
│   │   │   └── InventoryRepository.kt
│   │   └── usecase/
│   │       ├── InventoryUseCases.kt          # Add, UpdateQuantity, Consume, Delete, ToggleFrozen (+ Params)
│   │       └── ObserveInventoryUseCase.kt    # Stream reactivo con búsqueda/categoría
│   ├── data/
│   │   ├── InventoryRepositoryImpl.kt
│   │   └── FoodItemMapper.kt
│   └── presentation/
│       ├── InventoryScreen.kt
│       ├── InventoryViewModel.kt
│       ├── InventoryUiState.kt
│       ├── AddFoodScreen.kt
│       ├── AddFoodViewModel.kt
│       ├── FoodDetailScreen.kt
│       └── FoodDetailViewModel.kt
│
├── shoppinglist/                 # Feature: lista de compras
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
│       ├── ShoppingListScreen.kt
│       ├── ShoppingListViewModel.kt
│       └── ShoppingListUiState.kt
│
└── navigation/                   # Rutas y grafo de navegación
    └── FoodarioNavHost.kt
```

**Convenciones:**

- Todo UseCase implementa `UseCase<P, R>` (acción, `suspend`) u `ObserveUseCase<P, R>` (stream reactivo) — ver §4.4.
- Cada UseCase define su `Params` DTO en el mismo archivo; los casos sin parámetros usan `object XxxParams`.
- Los UseCases de un feature se agrupan en un archivo (`InventoryUseCases.kt`), como en SplitIt.
- Los mappers son funciones de extensión puras (`FoodItemEntity.toDomain()`, `FoodItem.toEntity()`).
- Los ViewModels extienden `androidx.lifecycle.ViewModel` (multiplataforma) y usan `viewModelScope`.

---

## 6. Modelo de dominio

Derivado directamente de CONCEPT.md (MVP):

```kotlin
// inventory/domain/model/FoodItem.kt
data class FoodItem(
    val id: Long,
    val name: String,
    val category: FoodCategory,
    val quantity: Double,
    val unit: QuantityUnit,
    val isFrozen: Boolean,
    val expirationDate: LocalDate?,   // opcional
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class FoodCategory {
    DAIRY,        // 🥛 Lácteos
    MEAT,         // 🥩 Carnes
    VEGETABLES,   // 🥦 Verduras
    FRUITS,       // 🍎 Frutas
    PANTRY,       // 🥫 Almacén
    FROZEN,       // 🧊 Congelados
    BEVERAGES,    // 🥤 Bebidas
    COOKED,       // 🍲 Comida cocinada (salsas, etc.)
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

**Notas de diseño:**

- `quantity` es `Double` para soportar tanto unidades enteras (8 huevos) como pesos/volúmenes (0.3 kg).
- `expirationDate` es nullable: el MVP lo marca como opcional.
- `isFrozen` es un flag del ítem, independiente de la categoría `FROZEN` (un ítem de categoría `MEAT` puede estar congelado).
- La categoría `COOKED` cubre el caso del concepto: *"agregar comida cocinada como una salsa"*.

---

## 7. Persistencia — SQLDelight

### 7.1 Configuración

En `shared/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("FoodarioDatabase") {
            packageName = "com.foodario.database"
        }
    }
}
```

Los archivos `.sq` viven en `commonMain/sqldelight/com/foodario/database/`.

### 7.2 Esquema

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

selectExpiringBefore:
SELECT * FROM food_item
WHERE expiration_date IS NOT NULL AND expiration_date <= :threshold
ORDER BY expiration_date ASC;

insert:
INSERT INTO food_item(name, category, quantity, unit, is_frozen, expiration_date, created_at, updated_at)
VALUES (:name, :category, :quantity, :unit, :isFrozen, :expirationDate, :createdAt, :updatedAt);

updateQuantity:
UPDATE food_item SET quantity = :quantity, updated_at = :updatedAt WHERE id = :id;

updateFrozen:
UPDATE food_item SET is_frozen = :isFrozen, updated_at = :updatedAt WHERE id = :id;

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

insert:
INSERT INTO shopping_item(name, category, quantity, unit, created_at)
VALUES (:name, :category, :quantity, :unit, :createdAt);

delete:
DELETE FROM shopping_item WHERE id = :id;
```

### 7.3 Drivers por plataforma (expect/actual)

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

### 7.4 Reactividad

Se usa `app.cash.sqldelight:coroutines-extensions` para convertir queries en `Flow`:

```kotlin
override fun observeInventory(): Flow<List<FoodItem>> =
    queries.selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entities -> entities.map { it.toDomain() } }
```

La base de datos es la **única fuente de verdad**: cualquier `insert`/`update`/`delete` emite automáticamente hacia la UI a través de estos Flows. No hay cachés en memoria ni estados duplicados.

---

## 8. Flujo de datos (UDF)

Flujo unidireccional en toda la app:

```
Usuario → Composable (evento) → ViewModel → UseCase → Repository → SQLDelight
                                                                            │
Composable (re-render) ← UiState ← StateFlow ← Flow ← Query actualizada ←───┘
```

**Ejemplo concreto — "Consumí 1 leche":**

1. `InventoryScreen` emite `InventoryEvent.DecreaseQuantity(itemId)`.
2. `InventoryViewModel` llama a `updateQuantity(UpdateQuantityParams(itemId, newQuantity))`.
3. El UseCase valida (cantidad ≥ 0; si llega a 0, sugiere pasar a compras) y delega en `InventoryRepository`.
4. El repositorio ejecuta `updateQuantity` en SQLDelight.
5. SQLDelight invalida el query `selectAll` → el `Flow` emite la nueva lista.
6. El ViewModel la transforma a `InventoryUiState` y la UI se re-renderiza.

### UiState por pantalla

```kotlin
data class InventoryUiState(
    val items: List<FoodItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

Un solo `StateFlow<InventoryUiState>` por ViewModel. Los eventos son `sealed interface` (`InventoryEvent`).

---

## 9. Inyección de dependencias — Koin

### 9.1 Organización de módulos

Un módulo Koin por capa, agregados en `SharedModules.kt`:

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
    // Inventory — los bindings se declaran con el tipo genérico explícito
    // para que los ViewModels dependan de la interfaz, no de la clase concreta
    factory<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>> { ObserveInventoryUseCase(get()) }
    factory<UseCase<AddFoodItemParams, FoodItem>> { AddFoodItemUseCase(get()) }
    factory<UseCase<UpdateQuantityParams, Unit>> { UpdateQuantityUseCase(get()) }
    factory<UseCase<ConsumeFoodItemParams, Unit>> { ConsumeFoodItemUseCase(get()) }
    factory<UseCase<DeleteFoodItemParams, Unit>> { DeleteFoodItemUseCase(get()) }
    factory<UseCase<ToggleFrozenParams, Unit>> { ToggleFrozenUseCase(get()) }
    // Shopping list
    factory<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>> { ObserveShoppingListUseCase(get()) }
    factory<UseCase<AddToShoppingListParams, Unit>> { AddToShoppingListUseCase(get()) }
    factory<UseCase<RemoveFromShoppingListParams, Unit>> { RemoveFromShoppingListUseCase(get()) }
    factory<UseCase<MoveToInventoryParams, FoodItem>> { MoveToInventoryUseCase(get(), get()) }
}

// di/PresentationModule.kt
val presentationModule = module {
    viewModel { InventoryViewModel(get(), get(), get()) }
    viewModel { AddFoodViewModel(get()) }
    viewModel { FoodDetailViewModel(get(), get(), get(), get()) }
    viewModel { ShoppingListViewModel(get(), get(), get()) }
}
```

### 9.2 Inicialización por plataforma

- **Android**: `FoodarioApplication : Application` en `androidApp` llama a `startKoin` con `androidContext()` + módulo de plataforma (`DatabaseDriverFactory(context)`).
- **iOS**: función `initKoin()` en `iosMain` llamada desde `iosApp` (Swift) al arrancar; provee el `DatabaseDriverFactory` nativo.

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

### 9.3 Resolución en la UI

Los ViewModels se resuelven en Compose con `koin-compose-viewmodel`:

```kotlin
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = koinViewModel()) { ... }
```

---

## 10. Estrategia de testing — MockK por capa

**Principio rector:** cada test prueba **una sola capa**, mockeando sus colaboradores con MockK. Nada de tests que crucen capas en los unitarios.

### 10.1 Matriz de testing

| Capa | Sujeto bajo test | Se mockea | Se verifica |
|---|---|---|---|
| **Data** | `InventoryRepositoryImpl` | `FoodItemQueries` (SQLDelight) | Mapeo correcto, llamadas a queries, propagación de Flows |
| **Domain** | `AddFoodItemUseCase` | `InventoryRepository` | Validaciones, transformaciones, delegación al repo |
| **Presentation** | `InventoryViewModel` | UseCases | `UiState` correcto por evento, manejo de errores |

### 10.2 Dependencias de test (`commonTest`)

```kotlin
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.mockk)
    implementation(libs.turbine)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.koin.test)   // verificación de módulos Koin
}
```

### 10.3 Ejemplos por capa

**Data — repositorio con queries mockeadas:**

```kotlin
class InventoryRepositoryImplTest {
    private val queries = mockk<FoodItemQueries>()
    private val repository = InventoryRepositoryImpl(queries)

    @Test
    fun `insert maps domain item to query params`() = runTest {
        coEvery { queries.insert(any(), any(), any(), any(), any(), any(), any(), any()) } just Runs

        repository.add(item)

        verify { queries.insert("Leche", "DAIRY", 2.0, "UNIT", 0, null, any(), any()) }
    }
}
```

**Domain — use case con repositorio mockeado:**

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

**Presentation — ViewModel con UseCases mockeados + Turbine:**

```kotlin
class InventoryViewModelTest {
    // Se mockea la interfaz genérica, no la clase concreta
    private val observeInventory =
        mockk<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>()
    private lateinit var viewModel: InventoryViewModel

    @Test
    fun `emits items in ui state`() = runTest {
        every { observeInventory(ObserveInventoryParams()) } returns flowOf(listOf(leche, huevos))
        viewModel = InventoryViewModel(observeInventory, ...)

        viewModel.uiState.test {
            assertEquals(listOf(leche, huevos), awaitItem().items)
        }
    }
}
```

### 10.4 Reglas de testing

1. **Un test, una capa.** Si el test del ViewModel necesita SQLDelight, está mal planteado.
2. **MockK para colaboradores, fakes solo cuando el mock sea más complejo que el fake** (p. ej. un `FakeRepository` en tests de UseCases con lógica de combinación compleja).
3. **Turbine para todo Flow** expuesto a la UI.
4. **`runTest` + `StandardTestDispatcher`** en ViewModels; inyectar dispatchers si hiciera falta (ver §11).
5. **Tests de DB reales (opcional, no unitarios):** con `JdbcSqliteDriver` en `androidHostTest` para validar el esquema `.sq`. No usan MockK.
6. **Verificación del grafo Koin:** `koin-test` con `verify()` sobre los módulos, para detectar bindings faltantes en CI.

---

## 11. Convenciones transversales

- **Dispatchers:** los repositorios usan `Dispatchers.IO` (vía `mapToList` / `withContext`). Si se necesita testabilidad fina, se inyecta un `CoroutineDispatcher` por constructor con default `Dispatchers.IO`.
- **Errores:** los UseCases validan con `require`/`requireNotNull` (convención de SplitIt); el ViewModel captura la excepción y la traduce a `UiState.error`. Los Flows de observación propagan excepciones que el ViewModel captura con `catch { }`.
- **Fechas:** `kotlinx-datetime` (`LocalDate`, `Instant`) en dominio; epoch millis en DB.
- **Navegación:** Compose Navigation multiplataforma (`androidx.navigation:navigation-compose` KMP) con rutas tipadas (type-safe navigation).
- **Nomenclatura de UseCases:** verbo + sustantivo + `UseCase` (`AddFoodItemUseCase`), implementando `UseCase<P, R>` u `ObserveUseCase<P, R>`. Los que observan streams usan prefijo `Observe` (`ObserveInventoryUseCase`).

---

## 12. Alineación con el roadmap del concepto

| Fase (CONCEPT.md) | Impacto en arquitectura |
|---|---|
| **MVP**: inventario, buscador, categorías, cantidad, vencimiento opcional, lista de compras | Cubierto por los features `inventory` + `shoppinglist` descriptos en §5 |
| **Carga rápida** ("Leche → +") | `AddFoodItemUseCase` acepta solo `name` con defaults (cantidad 1, unidad UNIT, sin vencimiento); el detalle se completa después |
| **Fase 2**: alertas de vencimiento / stock bajo | Nuevo feature `alerts` con su propio módulo: `ObserveExpiringItemsUseCase` (query `selectExpiringBefore` ya prevista) + notificaciones locales por plataforma (expect/actual) |
| **Fase 3**: escaneo de facturas con cámara | Nuevo feature `scanning`: expect/actual para cámara/ML Kit por plataforma, reutilizando `AddFoodItemUseCase` para precargar |

La separación por features y la regla de dependencias garantizan que las fases 2 y 3 se agreguen **sin modificar** las capas existentes, solo extendiéndolas.

---

## 13. Resumen de decisiones (ADR-lite)

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| 1 | SQLDelight | Room (solo Android), Realm | Type-safe SQL, KMP real, Flows nativos |
| 2 | Koin | Dagger/Hilt, Kodein | KMP nativo, sin codegen, curva baja |
| 3 | MockK | Mocks manuales/fakes | Aislamiento real por capa con poco boilerplate |
| 4 | Un módulo `shared` | Módulos por capa (`:domain`, `:data`) | Escala actual del MVP; los paquetes por feature ya dan la separación |
| 5 | DB como única fuente de verdad | Caché en memoria | Offline-first real, menos estados que sincronizar |
| 6 | Paquetes por feature | Paquetes por capa | Localiza cambios, escala mejor con las fases 2 y 3 |
| 7 | Interfaz genérica `UseCase<P, R>` + `ObserveUseCase<P, R>` (patrón de SplitIt) | `invoke` ad-hoc por clase | Contrato uniforme, ViewModels desacoplados de clases concretas, mocks triviales con MockK |
