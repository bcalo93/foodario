# Plan de implementación — Notificaciones de productos próximos a vencer

> **Para el ejecutor:** este plan es auto-contenido y está pensado para ejecutarse fase por fase, en orden.
> No te saltees fases ni cambies el orden: cada fase deja el proyecto compilando.
> Antes de empezar leé `AGENTS.md` (raíz) y las secciones §4.4, §5, §7, §9 y §10 de `docs/ARCHITECTURE.md`.
> **Reglas duras:** strings de UI en español; UseCases con `named("…")` en Koin; la DB es la única fuente de verdad;
> no modifiques lógica existente fuera de los puntos indicados; no agregues targets KMP nuevos.

---

## 0. Objetivo y decisiones ya tomadas

**Objetivo:** los productos con `expirationDate` cargada generan una notificación local **N días antes** del
vencimiento, a una **hora del día** determinada (ambas configurables por el usuario), para que el usuario use
ese item en una comida.

Decisiones de diseño (no las re-discutas, implementalas así):

| # | Decisión | Motivo |
|---|---|---|
| 1 | Feature nuevo `alerts/` en `shared/src/commonMain/kotlin/com/foodario/alerts/` | ARCHITECTURE §12 (Phase 2) lo define así |
| 2 | Ajustes persistidos en SQLDelight (tabla clave-valor `app_setting`) | La DB es la única fuente de verdad; da `Flow` reactivo gratis. Defaults: `enabled=true`, `leadDays=2`, `notifyHour=9`. Rangos válidos: `leadDays 1..14`, `notifyHour 0..23` |
| 3 | Las notificaciones suenan a las **`notifyHour`:00 h local** (configurable, default 9:00, minuto fijo en `:00`) del día `(vencimiento − N días)`. Si ese momento ya pasó y el item aún no venció → disparar ~1 minuto después de sincronizar. Items ya vencidos (`expiration < hoy`) no notifican | Hora configurable a nivel hora (sin minutos) para mantener la UI en un stepper simple |
| 4 | Estrategia **reconcile**: ante cada cambio (items o ajustes), cancelar todas las notificaciones de vencimiento pendientes y reprogramar | Correcta por construcción; evita estados duplicados |
| 5 | Android: **WorkManager** `OneTimeWorkRequest` por item (tag común + unique work por id). iOS: `UNTimeIntervalNotificationTrigger` por item | WorkManager sobrevive reboots y no requiere permiso de alarma exacta; en iOS el trigger por intervalo evita manipular `NSDateComponents` |
| 6 | El cálculo de fechas/hora de disparo vive en **commonMain** (el orquestador); los schedulers de plataforma son "tontos" (solo muestran) | Máxima lógica compartida y testeable |
| 7 | Permiso Android 13+ (`POST_NOTIFICATIONS`) se pide en `MainActivity.onCreate`. Tap en la notificación → abre la app (launch intent). Deep-link al detalle: **fuera de scope v1** | Simplicidad; pedir permiso desde Compose común requeriría plumbing extra |
| 8 | Configuración de N días y hora de aviso: diálogo `AlertSettingsDialog` que se abre desde un ícono 🔔 en el header de `InventoryScreen`. **No** se agrega un tab nuevo | La bottom bar tiene 2 destinos y AGENTS.md pide actualizarla si se agrega un tab; el diálogo es mínimo y acorde al diseño |
| 9 | Items congelados (`isFrozen`) igual notifican en V1 | El dominio no modela "congelar extiende la vida"; decisión explícita |
| 10 | Textos de notificación: título `¡<nombre> está por vencer!`, cuerpo según días restantes (ver §Fase 4) | Consistente con tono de la app |

---

## Fase 1 — Persistencia de ajustes (SQLDelight)

### 1.1 Crear `shared/src/commonMain/sqldelight/com/foodario/database/AppSettings.sq`

```sql
CREATE TABLE app_setting (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

selectAll:
SELECT * FROM app_setting;

upsert:
INSERT OR REPLACE INTO app_setting(key, value) VALUES (:key, :value);
```

Notas:
- El plugin genera `AppSettingsQueries` (nombre del archivo en camelCase) y el tipo de fila `App_setting`.
- **No** tocar `FoodItem.sq` en esta fase.

### 1.2 Regenerar código SQLDelight

```bash
./gradlew :shared:generateDebugFoodarioDatabaseInterface
```

### 1.3 Checkpoint Fase 1

```bash
./gradlew :androidApp:assembleDebug
```

Debe compilar. Todavía no hay código Kotlin nuevo que use las queries.

---

## Fase 2 — Dominio y data de `alerts` + extensión de inventory

### 2.1 `alerts/domain/model/AlertSettings.kt`

```kotlin
package com.foodario.alerts.domain.model

data class AlertSettings(
    val enabled: Boolean = true,
    val leadDays: Int = DEFAULT_LEAD_DAYS,
    val notifyHour: Int = DEFAULT_NOTIFY_HOUR,
) {
    companion object {
        const val DEFAULT_LEAD_DAYS = 2
        val LEAD_DAYS_RANGE = 1..14
        const val DEFAULT_NOTIFY_HOUR = 9
        val NOTIFY_HOUR_RANGE = 0..23
    }
}
```

### 2.2 `alerts/domain/repository/AlertSettingsRepository.kt`

```kotlin
package com.foodario.alerts.domain.repository

import com.foodario.alerts.domain.model.AlertSettings
import kotlinx.coroutines.flow.Flow

interface AlertSettingsRepository {
    fun observeSettings(): Flow<AlertSettings>
    suspend fun getSettings(): AlertSettings
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setLeadDays(leadDays: Int)
    suspend fun setNotifyHour(hour: Int)
}
```

### 2.3 `alerts/data/AlertSettingsRepositoryImpl.kt`

Seguir el patrón de `inventory/data/InventoryRepositoryImpl.kt` (dispatcher inyectado, `withContext`):

```kotlin
package com.foodario.alerts.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.database.AppSettingsQueries
import com.foodario.database.App_setting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlertSettingsRepositoryImpl(
    private val queries: AppSettingsQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AlertSettingsRepository {

    override fun observeSettings(): Flow<AlertSettings> =
        queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.toSettings() }

    override suspend fun getSettings(): AlertSettings = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toSettings()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        withContext(ioDispatcher) { queries.upsert(KEY_ENABLED, enabled.toString()) }
    }

    override suspend fun setLeadDays(leadDays: Int) {
        withContext(ioDispatcher) { queries.upsert(KEY_LEAD_DAYS, leadDays.toString()) }
    }

    override suspend fun setNotifyHour(hour: Int) {
        withContext(ioDispatcher) { queries.upsert(KEY_NOTIFY_HOUR, hour.toString()) }
    }

    private fun List<App_setting>.toSettings(): AlertSettings {
        val map = associate { it.key to it.value }
        return AlertSettings(
            enabled = map[KEY_ENABLED]?.toBooleanStrictOrNull() ?: true,
            leadDays = map[KEY_LEAD_DAYS]?.toIntOrNull() ?: AlertSettings.DEFAULT_LEAD_DAYS,
            notifyHour = map[KEY_NOTIFY_HOUR]?.toIntOrNull() ?: AlertSettings.DEFAULT_NOTIFY_HOUR,
        )
    }

    private companion object {
        const val KEY_ENABLED = "alerts.enabled"
        const val KEY_LEAD_DAYS = "alerts.leadDays"
        const val KEY_NOTIFY_HOUR = "alerts.notifyHour"
    }
}
```

### 2.4 `alerts/domain/usecase/AlertSettingsUseCases.kt`

Mismo patrón que `inventory/domain/usecase/InventoryUseCases.kt` (params + clases en un solo archivo):

```kotlin
package com.foodario.alerts.domain.usecase

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

object ObserveAlertSettingsParams

class ObserveAlertSettingsUseCase(
    private val repository: AlertSettingsRepository,
) : ObserveUseCase<ObserveAlertSettingsParams, AlertSettings> {
    override fun invoke(params: ObserveAlertSettingsParams): Flow<AlertSettings> =
        repository.observeSettings()
}

data class SetAlertNotificationsEnabledParams(val enabled: Boolean)

class SetAlertNotificationsEnabledUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertNotificationsEnabledParams, Unit> {
    override suspend fun invoke(params: SetAlertNotificationsEnabledParams) {
        repository.setEnabled(params.enabled)
    }
}

data class SetAlertLeadDaysParams(val leadDays: Int)

class SetAlertLeadDaysUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertLeadDaysParams, Unit> {
    override suspend fun invoke(params: SetAlertLeadDaysParams) {
        require(params.leadDays in AlertSettings.LEAD_DAYS_RANGE) {
            "Los días de anticipación deben estar entre ${AlertSettings.LEAD_DAYS_RANGE.first} y ${AlertSettings.LEAD_DAYS_RANGE.last}"
        }
        repository.setLeadDays(params.leadDays)
    }
}

data class SetAlertNotifyHourParams(val notifyHour: Int)

class SetAlertNotifyHourUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertNotifyHourParams, Unit> {
    override suspend fun invoke(params: SetAlertNotifyHourParams) {
        require(params.notifyHour in AlertSettings.NOTIFY_HOUR_RANGE) {
            "La hora de aviso debe estar entre 0 y 23"
        }
        repository.setNotifyHour(params.notifyHour)
    }
}
```

### 2.5 Extender `FoodItem.sq` con la query para el scheduler

Agregar al final de `shared/src/commonMain/sqldelight/com/foodario/database/FoodItem.sq` (sin tocar lo existente):

```sql
selectExpiringFrom:
SELECT * FROM food_item
WHERE expiration_date IS NOT NULL AND expiration_date >= :fromDate
ORDER BY expiration_date ASC;
```

> ¿Por qué `>=` y no la ya existente `selectExpiringBefore`? El scheduler programa notificaciones a futuro
> (un item que vence en 30 días con leadDays=2 debe quedar programado hoy para dentro de 28 días).
> `selectExpiringBefore` queda intacta para un futuro banner in-app.

Regenerar: `./gradlew :shared:generateDebugFoodarioDatabaseInterface`

### 2.6 Extender `InventoryRepository` y `InventoryRepositoryImpl`

En `inventory/domain/repository/InventoryRepository.kt` agregar:

```kotlin
fun observeExpiringFrom(fromDate: LocalDate): Flow<List<FoodItem>>
```

En `inventory/data/InventoryRepositoryImpl.kt` agregar (reutiliza la constante `MILLIS_PER_DAY` existente):

```kotlin
override fun observeExpiringFrom(fromDate: LocalDate): Flow<List<FoodItem>> =
    queries.selectExpiringFrom(fromDate = fromDate.toEpochDays() * MILLIS_PER_DAY)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { entities -> entities.map { it.toDomain() } }
```

### 2.7 `inventory/domain/usecase/ObserveExpiringItemsUseCase.kt`

```kotlin
package com.foodario.inventory.domain.usecase

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

object ObserveExpiringItemsParams

class ObserveExpiringItemsUseCase(
    private val repository: InventoryRepository,
    private val todayProvider: () -> LocalDate = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    },
) : ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>> {
    override fun invoke(params: ObserveExpiringItemsParams): Flow<List<FoodItem>> =
        repository.observeExpiringFrom(fromDate = todayProvider())
}
```

> `todayProvider` inyectable = tests deterministas (ver Fase 8).

### 2.8 Checkpoint Fase 2

```bash
./gradlew :androidApp:assembleDebug
```

---

## Fase 3 — Puerto de notificaciones + orquestador (commonMain)

### 3.1 `alerts/domain/model/ExpirationNotification.kt`

```kotlin
package com.foodario.alerts.domain.model

import kotlin.time.Instant

data class ExpirationNotification(
    val itemId: Long,
    val itemName: String,
    val daysLeft: Int,
    val triggerAt: Instant,
)
```

### 3.2 `alerts/domain/service/NotificationScheduler.kt`

```kotlin
package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.ExpirationNotification

interface NotificationScheduler {
    /** Cancela lo pendiente y programa la lista recibida (lista vacía = cancelar todo). */
    fun reconcileExpirations(notifications: List<ExpirationNotification>)
    fun cancelAll()
}
```

Los `actual` de plataforma se implementan en Fases 4 y 5 como clases que implementan esta interfaz
(se usa interfaz + DI, no `expect/actual`, porque se inyecta por Koin — mismo criterio que `DatabaseDriverFactory`
pero sin necesidad de constructor por plataforma en common).

### 3.3 `alerts/domain/service/ExpirationNotificationSync.kt`

```kotlin
package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.usecase.ObserveExpiringItemsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExpirationNotificationSync(
    private val observeExpiringItems: ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>>,
    private val observeAlertSettings: ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>,
    private val scheduler: NotificationScheduler,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowProvider: () -> Instant = { Clock.System.now() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    fun start() {
        scope.launch {
            observeAlertSettings(ObserveAlertSettingsParams)
                .flatMapLatest { settings ->
                    if (!settings.enabled) {
                        flowOf(settings to emptyList())
                    } else {
                        observeExpiringItems(ObserveExpiringItemsParams)
                            .map { items -> settings to items }
                    }
                }
                .collectLatest { (settings, items) ->
                    if (!settings.enabled || items.isEmpty()) {
                        scheduler.cancelAll()
                    } else {
                        scheduler.reconcileExpirations(buildNotifications(items, settings))
                    }
                }
        }
    }

    internal fun buildNotifications(
        items: List<FoodItem>,
        settings: AlertSettings,
    ): List<ExpirationNotification> {
        val now = nowProvider()
        val today = now.toLocalDateTime(timeZone).date
        return items.mapNotNull { item ->
            val expiration = item.expirationDate ?: return@mapNotNull null
            val notifyDate = expiration.minus(settings.leadDays, DateTimeUnit.DAY)
            var trigger = LocalDateTime(notifyDate, LocalTime(settings.notifyHour, 0)).toInstant(timeZone)
            if (trigger <= now) trigger = now + 1.minutes
            ExpirationNotification(
                itemId = item.id,
                itemName = item.name,
                daysLeft = today.daysUntil(expiration),
                triggerAt = trigger,
            )
        }
    }
}
```

### 3.4 Checkpoint Fase 3

```bash
./gradlew :androidApp:assembleDebug
```

Compila; todavía nada llama al sync.

---

## Fase 4 — Implementación Android (WorkManager)

### 4.1 Dependencias

En `gradle/libs.versions.toml`:

```toml
# en [versions]
androidx-work = "2.11.2"

# en [libraries]
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "androidx-work" }
```

En `shared/build.gradle.kts`, bloque `androidMain.dependencies`, agregar:

```kotlin
implementation(libs.androidx.work.runtime.ktx)
implementation(libs.androidx.core.ktx)
```

### 4.2 Permiso en el manifest

En `androidApp/src/main/AndroidManifest.xml`, dentro de `<manifest>` (antes de `<application>`):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 4.3 Pedir el permiso en runtime

En `androidApp/src/main/kotlin/com/foodario/MainActivity.kt`, al final de `onCreate`:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATIONS)
}
```

con `private companion object { const val REQUEST_CODE_NOTIFICATIONS = 1001 }` e imports
`android.Manifest`, `android.os.Build`. (V1: se pide en cada cold start si no está concedido; aceptable.)

### 4.4 `shared/src/androidMain/kotlin/com/foodario/alerts/ExpirationReminderWorker.kt`

```kotlin
package com.foodario.alerts

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpirationReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        val itemName = inputData.getString(KEY_ITEM_NAME) ?: return Result.success()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        if (itemId < 0L) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            itemId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = when (daysLeft) {
            0 -> "Vence hoy. ¡Usalo en una comida!"
            1 -> "Vence mañana. ¡Usalo en una comida!"
            else -> "Vence en $daysLeft días. ¡Usalo en una comida!"
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setContentTitle("¡$itemName está por vencer!")
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(itemId.toInt(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_ITEM_NAME = "itemName"
        const val KEY_DAYS_LEFT = "daysLeft"
        const val CHANNEL_ID = "expirations"
        const val WORK_TAG = "expiration_alert"
        fun workName(itemId: Long) = "exp_alert_$itemId"
    }
}
```

> `applicationInfo.icon` reutiliza el ícono de la app como `smallIcon` → no hace falta agregar recursos al módulo shared.

### 4.5 `shared/src/androidMain/kotlin/com/foodario/alerts/AndroidNotificationScheduler.kt`

```kotlin
package com.foodario.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.service.NotificationScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class AndroidNotificationScheduler(
    private val context: Context,
) : NotificationScheduler {

    init {
        createChannel()
    }

    override fun reconcileExpirations(notifications: List<ExpirationNotification>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(ExpirationReminderWorker.WORK_TAG)
        val now = Clock.System.now()
        notifications.forEach { notification ->
            val delayMillis = (notification.triggerAt - now).inWholeMilliseconds.coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ExpirationReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(ExpirationReminderWorker.WORK_TAG)
                .setInputData(
                    workDataOf(
                        ExpirationReminderWorker.KEY_ITEM_ID to notification.itemId,
                        ExpirationReminderWorker.KEY_ITEM_NAME to notification.itemName,
                        ExpirationReminderWorker.KEY_DAYS_LEFT to notification.daysLeft,
                    )
                )
                .build()
            workManager.enqueueUniqueWork(
                ExpirationReminderWorker.workName(notification.itemId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(ExpirationReminderWorker.WORK_TAG)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ExpirationReminderWorker.CHANNEL_ID,
                "Vencimientos",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avisos de productos próximos a vencer"
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
```

### 4.6 Binding en la plataforma

En `shared/src/androidMain/kotlin/com/foodario/di/Koin.android.kt`, dentro de `platformModule()`:

```kotlin
single<com.foodario.alerts.domain.service.NotificationScheduler> {
    com.foodario.alerts.AndroidNotificationScheduler(androidContext())
}
```

### 4.7 Checkpoint Fase 4

```bash
./gradlew :androidApp:assembleDebug
```

---

## Fase 5 — Implementación iOS (UNUserNotificationCenter)

### 5.1 `shared/src/iosMain/kotlin/com/foodario/alerts/IosNotificationScheduler.kt`

```kotlin
package com.foodario.alerts

import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.service.NotificationScheduler
import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun reconcileExpirations(notifications: List<ExpirationNotification>) {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ours = requests.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(IDENTIFIER_PREFIX) }
            center.removePendingNotificationRequestsWithIdentifiers(ours)
            if (notifications.isEmpty()) return@getPendingNotificationRequestsWithCompletionHandler

            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { granted, _ ->
                if (!granted) return@requestAuthorizationWithOptions
                val now = NSDate.date().timeIntervalSince1970
                notifications.forEach { notification ->
                    val interval = (notification.triggerAt.epochSeconds - now)
                        .coerceAtLeast(MINIMUM_INTERVAL_SECONDS)
                    val content = UNMutableNotificationContent().apply {
                        setTitle("¡${notification.itemName} está por vencer!")
                        setBody(bodyFor(notification.daysLeft))
                        setSound(UNNotificationSound.defaultSound)
                    }
                    val trigger = UNTimeIntervalNotificationTrigger
                        .triggerWithTimeInterval(interval, repeats = false)
                    val request = UNNotificationRequest.requestWithIdentifier(
                        identifier = "$IDENTIFIER_PREFIX${notification.itemId}",
                        content = content,
                        trigger = trigger,
                    )
                    center.addNotificationRequest(request, null)
                }
            }
        }
    }

    override fun cancelAll() {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ours = requests.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(IDENTIFIER_PREFIX) }
            center.removePendingNotificationRequestsWithIdentifiers(ours)
        }
    }

    private fun bodyFor(daysLeft: Int): String = when (daysLeft) {
        0 -> "Vence hoy. ¡Usalo en una comida!"
        1 -> "Vence mañana. ¡Usalo en una comida!"
        else -> "Vence en $daysLeft días. ¡Usalo en una comida!"
    }

    private companion object {
        const val IDENTIFIER_PREFIX = "exp_alert_"
        const val MINIMUM_INTERVAL_SECONDS = 60.0
    }
}
```

### 5.2 Binding en la plataforma

En `shared/src/iosMain/kotlin/com/foodario/di/Koin.ios.kt`, dentro de `platformModule()`:

```kotlin
single<com.foodario.alerts.domain.service.NotificationScheduler> {
    com.foodario.alerts.IosNotificationScheduler()
}
```

### 5.3 Checkpoint Fase 5

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```

(Solo compila en macOS. Si la tarea no existe con ese nombre, listar con `./gradlew :shared:tasks --all | grep -i ios` y usar la de compilación del target simulator.)

---

## Fase 6 — Cableado DI común + arranque del sync

### 6.1 `di/DataModule.kt`

Agregar:

```kotlin
single { get<FoodarioDatabase>().appSettingsQueries }
single<AlertSettingsRepository> { AlertSettingsRepositoryImpl(get()) }
```

(con sus imports de `com.foodario.alerts.data.AlertSettingsRepositoryImpl` y
`com.foodario.alerts.domain.repository.AlertSettingsRepository`)

### 6.2 `di/DomainModule.kt`

Agregar al final del `module { ... }`:

```kotlin
// Alerts (expiration notifications)
factory<ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>>>(named("observeExpiringItems")) {
    ObserveExpiringItemsUseCase(get())
}
factory<ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>>(named("observeAlertSettings")) {
    ObserveAlertSettingsUseCase(get())
}
factory<UseCase<SetAlertNotificationsEnabledParams, Unit>>(named("setAlertNotificationsEnabled")) {
    SetAlertNotificationsEnabledUseCase(get())
}
factory<UseCase<SetAlertLeadDaysParams, Unit>>(named("setAlertLeadDays")) {
    SetAlertLeadDaysUseCase(get())
}
factory<UseCase<SetAlertNotifyHourParams, Unit>>(named("setAlertNotifyHour")) {
    SetAlertNotifyHourUseCase(get())
}
single {
    ExpirationNotificationSync(
        observeExpiringItems = get(named("observeExpiringItems")),
        observeAlertSettings = get(named("observeAlertSettings")),
        scheduler = get(),
    )
}
```

> `ExpirationNotificationSync` es `single` (servicio de aplicación), no `factory`. El `scheduler = get()`
> resuelve contra el binding de plataforma (Fases 4.6 / 5.2).

### 6.3 Arranque en Android

En `androidApp/src/main/kotlin/com/foodario/FoodarioApplication.kt`:

```kotlin
class FoodarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApp = initKoin(this)
        koinApp.koin.get<ExpirationNotificationSync>().start()
    }
}
```

(`initKoin` ya devuelve el `KoinApplication` de `startKoin { ... }`.)

### 6.4 Arranque en iOS

En `shared/src/iosMain/kotlin/com/foodario/MainViewController.kt`:

```kotlin
fun MainViewController(): UIViewController {
    initKoin()
    GlobalContext.get().get<ExpirationNotificationSync>().start()
    return ComposeUIViewController { App() }
}
```

(import `org.koin.core.context.GlobalContext`.)

### 6.5 Actualizar `KoinGraphTest`

En `shared/src/androidHostTest/kotlin/com/foodario/di/KoinGraphTest.kt`:

1. En el test `verify koin graph resolves all definitions`, agregar el tipo de plataforma a `extraTypes`:

```kotlin
module.verify(
    extraTypes = listOf(
        DatabaseDriverFactory::class,
        Long::class,
        NotificationScheduler::class,
    ),
).verify()
```

2. En el test `resolve view models with the use cases for their feature`, agregar al módulo inline los mocks:

```kotlin
single<AlertSettingsRepository> {
    mockk {
        every { observeSettings() } returns flowOf(AlertSettings())
    }
}
single<NotificationScheduler> { mockk(relaxed = true) }
```

y una aserción de resolución:

```kotlin
assertIs<ExpirationNotificationSync>(koin.get<ExpirationNotificationSync>())
```

### 6.6 Checkpoint Fase 6

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

---

## Fase 7 — UI de configuración (diálogo en Inventory)

### 7.1 `alerts/presentation/AlertSettingsUiState.kt`

```kotlin
package com.foodario.alerts.presentation

data class AlertSettingsUiState(
    val enabled: Boolean = true,
    val leadDays: Int = 2,
    val notifyHour: Int = 9,
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

### 7.2 `alerts/presentation/AlertSettingsViewModel.kt`

Seguir el patrón de `InventoryViewModel` (evento sellado + `runCatching` + `stateIn`):

```kotlin
package com.foodario.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.alerts.domain.usecase.SetAlertLeadDaysParams
import com.foodario.alerts.domain.usecase.SetAlertNotificationsEnabledParams
import com.foodario.alerts.domain.usecase.SetAlertNotifyHourParams
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AlertSettingsEvent {
    data class SetEnabled(val enabled: Boolean) : AlertSettingsEvent
    data class SetLeadDays(val leadDays: Int) : AlertSettingsEvent
    data class SetNotifyHour(val notifyHour: Int) : AlertSettingsEvent
}

class AlertSettingsViewModel(
    private val observeAlertSettings: ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>,
    private val setAlertNotificationsEnabled: UseCase<SetAlertNotificationsEnabledParams, Unit>,
    private val setAlertLeadDays: UseCase<SetAlertLeadDaysParams, Unit>,
    private val setAlertNotifyHour: UseCase<SetAlertNotifyHourParams, Unit>,
) : ViewModel() {

    val uiState: StateFlow<AlertSettingsUiState> =
        observeAlertSettings(ObserveAlertSettingsParams)
            .map {
                AlertSettingsUiState(
                    enabled = it.enabled,
                    leadDays = it.leadDays,
                    notifyHour = it.notifyHour,
                    isLoading = false,
                )
            }
            .catch { e -> emit(AlertSettingsUiState(isLoading = false, error = e.message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AlertSettingsUiState(),
            )

    fun onEvent(event: AlertSettingsEvent) {
        when (event) {
            is AlertSettingsEvent.SetEnabled -> viewModelScope.launch {
                runCatching {
                    setAlertNotificationsEnabled(SetAlertNotificationsEnabledParams(event.enabled))
                }
            }
            is AlertSettingsEvent.SetLeadDays -> viewModelScope.launch {
                runCatching { setAlertLeadDays(SetAlertLeadDaysParams(event.leadDays)) }
            }
            is AlertSettingsEvent.SetNotifyHour -> viewModelScope.launch {
                runCatching { setAlertNotifyHour(SetAlertNotifyHourParams(event.notifyHour)) }
            }
        }
    }
}
```

### 7.3 Registrar el ViewModel en `di/PresentationModule.kt`

```kotlin
viewModel {
    AlertSettingsViewModel(
        observeAlertSettings = get(named("observeAlertSettings")),
        setAlertNotificationsEnabled = get(named("setAlertNotificationsEnabled")),
        setAlertLeadDays = get(named("setAlertLeadDays")),
        setAlertNotifyHour = get(named("setAlertNotifyHour")),
    )
}
```

### 7.4 `alerts/presentation/uicomponents/AlertSettingsDialog.kt`

**Antes de escribirlo, leer** `inventory/presentation/uicomponents/FoodDetailExpirationDialog.kt` y copiar su
estructura/estilo (AlertDialog con colores `paperElevated`/`ink`, tipografías del tema). Contenido requerido:

- Título: `"Avisos de vencimiento"` (style `titleHand`, color `ink`).
- Fila 1: `Text("Activar avisos", style = typography.body)` + `Spacer(Modifier.weight(1f))` + `Switch`
  con `checked = uiState.enabled`, `onCheckedChange = { viewModel.onEvent(AlertSettingsEvent.SetEnabled(it)) }`.
- Fila 2 (habilitada solo si `uiState.enabled`, si no, con alpha 0.5): stepper
  `[ − ]  Avisar N días antes  [ + ]`:
  - Botones `−` / `+` con `Modifier.size(FoodarioTheme.dimensions.touchTarget)` (mínimo 48dp, regla de diseño).
  - `−` deshabilitado en `leadDays <= AlertSettings.LEAD_DAYS_RANGE.first`; `+` en `>= last`.
  - El número `N` en `typography.quantityHand`.
- Fila 3 (mismas reglas de habilitación que la fila 2): stepper `[ − ]  Avisar a las HH:00 h  [ + ]`:
  - Muestra la hora formateada con `uiState.notifyHour.toString().padStart(2, '0')` + `":00"`.
  - `−` deshabilitado en `notifyHour <= AlertSettings.NOTIFY_HOUR_RANGE.first`; `+` en `>= last`.
  - Eventos `AlertSettingsEvent.SetNotifyHour(uiState.notifyHour - 1)` / `(+ 1)`.
- Caption: `"Podés usar ese producto en una comida antes de que venza"` (`typography.caption`, color `inkSoft`).
- `confirmButton`: `TextButton(onClick = onDismiss) { Text("Listo") }`.

Firma:

```kotlin
@Composable
fun AlertSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: AlertSettingsViewModel = koinViewModel(),
)
```

usando `androidx.lifecycle.compose.collectAsStateWithLifecycle` y `org.koin.compose.viewmodel.koinViewModel`.

### 7.5 Punto de entrada en `InventoryScreen`

En `inventory/presentation/InventoryScreen.kt`:

```kotlin
var showAlertSettings by remember { mutableStateOf(false) }
// ...
InventoryContent(
    // ...params existentes...,
    onAlertSettingsClick = { showAlertSettings = true },
)
if (showAlertSettings) {
    AlertSettingsDialog(onDismiss = { showAlertSettings = false })
}
```

En `inventory/presentation/uicomponents/InventoryContent.kt`:

1. Agregar parámetro `onAlertSettingsClick: () -> Unit`.
2. Envolver el `Text("Mi heladera", ...)` del header en un `Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically)` con el título + `Spacer(Modifier.weight(1f))` + botón campana:

```kotlin
Box(
    modifier = Modifier
        .size(FoodarioTheme.dimensions.touchTarget)
        .clickable(onClick = onAlertSettingsClick),
    contentAlignment = Alignment.Center,
) {
    Text(text = "🔔", fontSize = 22.sp)
}
```

3. Actualizar `InventoryScreenPreview.kt` si la preview llama a `InventoryContent` (pasar `onAlertSettingsClick = {}`).

### 7.6 Checkpoint Fase 7

```bash
./gradlew :androidApp:assembleDebug
```

Verificación manual (emulador): abrir la app → 🔔 en "Mi heladera" → cambiar días → cerrar y reabrir el diálogo:
el valor persiste.

---

## Fase 8 — Tests (androidHostTest, salvo indicación)

Convenciones: `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` + `FoodarioDatabase.Schema.create(driver)` para
repositorios (template: `inventory/data/InventoryRepositoryImplTest.kt`); MockK para collaborators; Turbine para
Flows; `Dispatchers.Unconfined` como dispatcher inyectado. Usar las factories de `TestHelpers.kt` si existen para
`FoodItem`.

### 8.1 `alerts/data/AlertSettingsRepositoryImplTest.kt`

- `returns defaults when table is empty` → `getSettings()` == `AlertSettings()`.
- `persists enabled, lead days and notify hour` → set + get round-trip de los tres campos.
- `observeSettings emits on change` (Turbine): emite defaults, luego el valor actualizado tras `setLeadDays(5)`.

### 8.2 `alerts/domain/usecase/AlertSettingsUseCasesTest.kt`

- `SetAlertLeadDaysUseCase` rechaza 0 y 15 con `IllegalArgumentException` y no toca el repo (`coVerify(exactly = 0)`).
- Acepta 1 y 14 (bordes del rango).
- `SetAlertNotifyHourUseCase` rechaza -1 y 24; acepta 0 y 23.
- `SetAlertNotificationsEnabledUseCase` delega.

### 8.3 `inventory/domain/usecase/ObserveExpiringItemsUseCaseTest.kt`

- Con `todayProvider = { LocalDate(2026, 10, 11) }`, verificar que llama a
  `repository.observeExpiringFrom(LocalDate(2026, 10, 11))` y propaga el Flow (Turbine).

### 8.4 `inventory/data/InventoryRepositoryImplTest.kt` (extender, no reescribir)

- `observeExpiringFrom returns only items expiring on or after the given date`: insertar items con vencimiento
  ayer / hoy / en 5 días / sin vencimiento; observar con `fromDate = hoy` → devuelve hoy y +5, ordenados por fecha.

### 8.5 `alerts/domain/service/ExpirationNotificationSyncTest.kt`

- Fake scheduler:

```kotlin
private class FakeScheduler : NotificationScheduler {
    val calls = mutableListOf<List<ExpirationNotification>>()
    var cancelCount = 0
    override fun reconcileExpirations(notifications: List<ExpirationNotification>) { calls += notifications }
    override fun cancelAll() { cancelCount++ }
}
```

- MockK de los dos `ObserveUseCase` devolviendo `flowOf(...)`.
- Inyectar `scope = TestScope(...)` (o `backgroundScope` de `runTest`), `nowProvider` fija y
  `timeZone = TimeZone.UTC`.
- Casos:
  1. `disabled settings cancel all notifications`.
  2. `enabled with items schedules at notifyHour local leadDays before expiration` (verificar `triggerAt` exacto,
     con `AlertSettings(leadDays = 2, notifyHour = 9)` y también con un `notifyHour` distinto, p.ej. 18).
  3. `past trigger time is clamped to ~now` (item que vence mañana con leadDays=2 → trigger > now y < now+2min).
  4. `items without expiration date are ignored`.

### 8.6 Checkpoint Fase 8

```bash
./gradlew :shared:testAndroidHostTest
```

Todo verde, incluido `KoinGraphTest` actualizado en Fase 6.5.

---

## Fase 9 — Documentación

1. **`docs/ARCHITECTURE.md`**:
   - §5 (package structure): agregar el bloque del feature `alerts/` (domain: model/repository/usecase/service; data; presentation + uicomponents).
   - §7.2 (schema): agregar el bloque de `AppSettings.sq` y la query `selectExpiringFrom` en `FoodItem.sq`.
   - §12: marcar la fila **Phase 2** como implementada describiendo el enfoque real (WorkManager + UNUserNotificationCenter detrás de `NotificationScheduler`, reconcile-on-change).
2. **`docs/DESIGN.md`**: en §5 agregar subsección breve `5.6 AlertSettingsDialog` (diálogo notebook: switch + stepper de días + stepper de hora; entrada 🔔 desde el header de inventario).
3. **`AGENTS.md`** (raíz): en "Where new code lives" agregar `alerts/`; anotar que Android usa WorkManager +
   `POST_NOTIFICATIONS` y que el sync arranca en `FoodarioApplication` / `MainViewController`.

---

## Verificación final (definition of done)

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

- [ ] Todo compila y los tests pasan (incluido `KoinGraphTest`).
- [ ] Manual Android: instalar debug, conceder permiso de notificaciones, agregar item "Leche" con vencimiento
      mañana, leadDays=1 → `adb shell dumpsys jobscheduler | grep -i foodario` muestra el trabajo programado
      (nombre `exp_alert_<id>`).
- [ ] Manual: desactivar "Activar avisos" → los trabajos se cancelan (mismo dumpsys sin resultados).
- [ ] Manual: cambiar leadDays reprograma (REPLACE sobre el mismo unique work name).
- [ ] iOS (si hay entorno): correr el esquema de Xcode, agregar item con vencimiento mañana → notificación
      pendiente visible en Settings > Notifications del simulador o log de `getPendingNotificationRequests`.

## Riesgos / notas para el ejecutor

- **Exactitud:** WorkManager no garantiza hora exacta (puede demorar minutos). Aceptable en V1; no migrar a
  `AlarmManager` exacto (requiere permiso extra).
- **iOS:** máximo 64 notificaciones pendientes por app — con reconcile por item no se alcanza en uso real.
- **No** agregar deep-links ni acciones en la notificación en V1.
- Si `Clock.System` marca warning experimental, seguir la convención ya usada en `InventoryUseCases.kt`
  (el proyecto ya usa `kotlin.time.Clock`, es el mismo caso).
- No commitear cambios; el usuario revisa antes de commitear.
