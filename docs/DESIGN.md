# Diseño — Foodario

> Documento de decisiones de diseño de UI. Complementa a [CONCEPT.md](./CONCEPT.md) (qué hace la app) y [ARCHITECTURE.md](./ARCHITECTURE.md) (cómo se construye).
> Todo lo aquí definido se implementa en `core/presentation` (tema + componentes compartidos) del módulo `shared`.

---

## 1. Concepto de diseño: "La libreta de la heladera"

La aplicación se presenta como una **libreta de apuntes escrita a mano**: ese lugar donde uno anotaría *"queda 1 leche"* o tacharía *"comprar huevos"*. La metáfora no es decorativa — refuerza el propósito del producto: anotar y consultar rápido, sin fricción.

Los tres principios que gobiernan todas las decisiones:

| # | Principio | Origen |
|---|---|---|
| 1 | **Velocidad ante todo** | CONCEPT: *"Leche → + y listo"*. El diseño nunca debe agregar pasos; la estética no puede costar segundos. |
| 2 | **Legibilidad en contexto real** | Se usa en el supermercado, con una mano, a la luz del sol o en la heladera a las 11pm. Light y dark mode son ciudadanos de primera clase. |
| 3 | **Calidez de lo hecho a mano** | La app debe sentirse como *tu* libreta, no como un sistema de gestión de stock. Lo manuscrito aporta cercanía, nunca obstáculo. |

---

## 2. Decisiones fundacionales (acordadas)

| Decisión | Resolución |
|---|---|
| **Tipografía** | Manuscrita en títulos, cantidades y categorías + sans legible en cuerpo y labels |
| **Paleta light** | Libreta clásica: papel crema, tinta azul-oscura, margen rojo como acento, categorías tipo resaltador |
| **Nivel de metáfora** | Moderado: tipografía + paleta + detalles dibujados (líneas de cuaderno, tildes a mano, chips-sticker, divisores doodle). Sin texturas pesadas |
| **Dark mode** | "Libreta nocturna": papel oscuro cálido (no negro puro), tinta clara, mismos acentos |

---

## 3. Tipografía

### 3.1 Familias

| Rol | Fuente | Justificación |
|---|---|---|
| **Manuscrita** | **Caveat** (Google Fonts, OFL) | Manuscrita de las más legibles que existen; trazo de marcador que evoca nota de heladera. Variable en peso (400–700). |
| Alternativa manuscrita | Patrick Hand, Kalam | Fallbacks si Caveat no convence en pruebas de legibilidad. |
| **Cuerpo / sans** | **Nunito** (Google Fonts, OFL) | Sans redondeada y amable que armoniza con lo manuscrito sin competir; excelente legibilidad en tamaños chicos. |

Ambas se empaquetan como recursos (`composeResources/font/`) — la app es offline, **no se depende de fuentes descargables en runtime**.

### 3.2 Escala tipográfica

| Token | Familia | Tamaño | Uso |
|---|---|---|---|
| `displayHand` | Caveat Bold | 34sp | Títulos de pantalla ("Mi heladera", "Compras") |
| `titleHand` | Caveat SemiBold | 24sp | Nombre del alimento en lista y detalle |
| `quantityHand` | Caveat Bold | 28sp | Números de cantidad ("2", "300 g") — el dato más consultado |
| `labelHand` | Caveat Medium | 18sp | Categorías, anotaciones ("¡vence en 2 días!") |
| `body` | Nunito Regular | 16sp | Descripciones, textos secundarios |
| `label` | Nunito Medium | 14sp | Botones, chips, metadata |
| `caption` | Nunito Regular | 12sp | Fechas, hints, texto auxiliar |

**Regla dura:** la fuente manuscrita **nunca baja de 18sp**. Por debajo de ese tamaño la legibilidad se degrada y la metáfora se vuelve obstáculo (principio 2).

---

## 4. Sistema de color

### 4.1 Light mode — "Libreta clásica"

| Token | Hex | Rol |
|---|---|---|
| `paper` | `#FAF6EE` | Fondo de pantalla — papel crema |
| `paperElevated` | `#FFFFFF` | Cards, sheets, diálogos |
| `ink` | `#1F2A44` | Texto principal — tinta azul-oscura |
| `inkSoft` | `#5A6378` | Texto secundario |
| `penBlue` | `#35507E` | Primario: acciones, links, FAB |
| `marginRed` | `#D9544F` | Acento: línea de margen, alertas de vencimiento, borrar |
| `pencilGray` | `#9AA1B0` | Bordes, divisores, iconos inactivos |

**Colores de categoría (estilo resaltador, fondos suaves con tinta encima):**

| Categoría | Highlight | Emoji (del CONCEPT) |
|---|---|---|
| Lácteos | `#F3E3B2` | 🥛 |
| Carnes | `#F0C8C0` | 🥩 |
| Verduras | `#D2E8B8` | 🥦 |
| Frutas | `#F6D3A8` | 🍎 |
| Almacén | `#E4D9C8` | 🥫 |
| Congelados | `#C6E0EC` | 🧊 |
| Bebidas | `#DCD2EA` | 🥤 |
| Cocinados | `#F2CCD8` | 🍲 |

### 4.2 Dark mode — "Libreta nocturna"

| Token | Hex | Rol |
|---|---|---|
| `paper` | `#242119` | Fondo — papel kraft oscuro, cálido (nunca negro puro) |
| `paperElevated` | `#2F2B22` | Cards, sheets |
| `ink` | `#ECE6D8` | Texto principal — tinta clara |
| `inkSoft` | `#A8A294` | Texto secundario |
| `penBlue` | `#9DB8DE` | Primario — tinta clara |
| `marginRed` | `#E08A82` | Acento — mismo rol, versión luminosa |
| `pencilGray` | `#6B665A` | Bordes, divisores |

Los highlights de categoría en dark mode son los mismos tonos **al 25–30% de opacidad** sobre el papel oscuro, manteniendo la asociación color ↔ categoría sin reventar el contraste.

### 4.3 Reglas de color

1. **El rojo (`marginRed`) se reserva para dos cosas:** la línea de margen de la libreta y las alertas (vencimiento, eliminar). Nada más — su escasez le da significado.
2. **El color de categoría nunca viaja solo:** siempre acompañado de emoji o texto (accesibilidad, §9).
3. **Contraste mínimo WCAG AA** (4.5:1 texto, 3:1 elementos grandes) verificado en ambos modos antes de mergear cualquier cambio de paleta.

---

## 5. Componentes

La metáfora "moderada" se concreta en estos componentes (todos en `core/presentation/components/`):

### 5.1 `NotebookListItem` — fila de inventario

```
─────────────────────────────────────────────
 🥛  Leche                    2 unidades
     Lácteos                        ✎
─────────────────────────────────────────────  ← línea de cuaderno (1dp, pencilGray 40%)
```

- Separador = **línea de cuaderno** dibujada (`drawBehind`), no un `Divider` estándar.
- Nombre en `titleHand`, cantidad en `quantityHand` — la cantidad es el dato estrella (CONCEPT: *"¿ya tengo esto?"*).
- Emoji de categoría como "sello" a la izquierda; categoría en `labelHand` con su highlight detrás, estilo resaltador.
- **Ítem congelado:** ícono ❄ pequeño junto al nombre.
- **Vencimiento próximo:** anotación en `marginRed` estilo nota al margen: *"¡vence en 2 días!"* con un doodle de flechita. Nunca un badge institucional.

### 5.2 `QuickAddBar` — carga rápida (la pieza clave del CONCEPT)

```
┌─────────────────────────────────────┐
│  ✏️  Escribí un alimento...      ➕  │
└─────────────────────────────────────┘
[ 🥛 ] [ 🥩 ] [ 🥦 ] [ 🍎 ] [ 🥫 ] [ 🧊 ] [ 🥤 ]
```

- Campo de texto que parece una **línea de cuaderno** (underline punteado, sin caja).
- Botón `➕` en `penBlue`: agrega con defaults (cantidad 1, unidad) — el flujo *"Leche → +"*.
- Chips de categoría = **stickers redondeados** con emoji, borde sutil tipo recorte, highlight de su color. Un tap setea la categoría del próximo ingreso.
- Completar detalles (vencimiento, congelado) es siempre **opcional y posterior**, desde el detalle.

### 5.3 `HandDrawnCheckbox` — lista de compras

- Checkbox cuadrado con borde de trazo irregular (dibujado) y **tilde a mano** animada (stroke que se dibuja en ~200ms) al marcar.
- Ítem comprado: texto **tachado con trazo de marcador** (línea ligeramente irregular, no `textDecoration` recto) + atenuación.
- Botón **"Agregar a mi heladera"**: estilo nota adhesiva (paperElevated, sombra suave, leve rotación −1°).

### 5.4 `QuantityStepper` — detalle

```
        [ − ]   2 unidades   [ + ]
```

- Botones circulares con trazo de lápiz; el número en `quantityHand` (28sp) — protagonista de la pantalla de detalle.
- Mantener presionado `+`/`−` incrementa/decrementa continuamente (usar en el super con una mano).

### 5.5 Navegación y acciones globales

- **Bottom bar** (2 destinos: 🧊 Heladera / 🛒 Compras) con íconos de trazo manuscrito y label en `labelHand`.
- **FAB** = lápiz ✏️ en `penBlue` para agregar (alternativa al QuickAddBar en pantallas sin él).
- **Divisores de sección** = doodles (garabato, flecha, asterisco dibujado) en vez de líneas rectas.

---

## 6. Layout y espaciado

- Sistema de espaciado en múltiplos de 4: `4, 8, 12, 16, 24, 32`.
- **Margen de libreta:** las pantallas de lista reservan un margen izquierdo de 40dp con la **línea roja vertical** (`marginRed`, 1dp, 60% opacidad) — el gesto visual más reconocible de una libreta. El contenido vive a la derecha de esa línea; los emojis de categoría pueden "invadir" el margen como anotaciones.
- Cards con `cornerRadius` 12dp y sombra muy suave (el papel no flota alto).
- Objetivos táctiles ≥ 48dp siempre (principio 2: uso con una mano).

---

## 7. Iconografía e ilustración

- **Íconos de sistema:** Material Symbols Rounded como base, pero con `strokeWidth` reducido y esquinas irregulares donde sea viable; a mediano plazo, set propio de trazo manuscrito.
- **Emojis de categoría** (🥛🥩🥦🍎🥫🧊🥤🍲) son parte del sistema de diseño — vienen del CONCEPT y funcionan como "sellos" de la libreta.
- **Empty states:** doodles dibujados a mano (heladera abierta con una nota pegada, carrito con garabato) + mensaje en `titleHand`:
  - Inventario vacío: *"Tu heladera está vacía… ¡empezá a anotar!"*
  - Compras vacía: *"Nada para comprar. Tachá todo ✓"*
- **Sin texturas de papel ni espirales** (nivel moderado): la metáfora vive en tipografía, líneas y trazos, no en fondos pesados.

---

## 8. Motion

| Interacción | Animación |
|---|---|
| Agregar alimento (quick add) | El ítem "se escribe" en la lista: fade + slide suave (150–200ms), sin rebote exagerado |
| Tilde en checkbox | Stroke que se dibuja (200ms) |
| Tachar comprado | Trazo de marcador animado (250ms) |
| Cambio de cantidad | El número "salta" levemente (scale 1.0 → 1.15 → 1.0, 150ms) |
| Navegación | Transiciones M3 estándar — la personalidad está en los componentes, no en transiciones exóticas |

**Regla:** ninguna animación supera los 300ms ni bloquea una acción (principio 1). Respetar `Reduce Motion` del sistema desactivando las animaciones de trazo.

---

## 9. Accesibilidad

1. **Manuscrita con límites:** solo ≥ 18sp, nunca en cuerpos de texto ni información crítica pequeña (§3.2).
2. **Contraste AA** en ambos modos (§4.3).
3. **No depender del color:** vencimiento = color + texto + doodle; categorías = color + emoji + nombre.
4. **Content descriptions** en todos los íconos/emoji con rol semántico (el emoji decorativo se excluye del árbol de accesibilidad).
5. **Dynamic type:** la escala tipográfica respeta el fontScale del sistema; la manuscrita escala igual que la sans.
6. **Touch targets ≥ 48dp** y stepper usable con una mano (§5.4).

---

## 10. Implementación en Compose

Mapeo directo con la arquitectura (`core/presentation`):

```
core/presentation/
├── theme/
│   ├── FoodarioTheme.kt        # lightColorScheme / darkColorScheme + FoodarioColors (extensión)
│   ├── Color.kt                # tokens de §4 (paper, ink, penBlue, marginRed, highlights)
│   ├── Type.kt                 # escala de §3.2 (Caveat + Nunito desde composeResources/font)
│   └── CategoryColors.kt       # color por FoodCategory, resuelto por tema (light/dark)
└── components/
    ├── NotebookListItem.kt
    ├── QuickAddBar.kt
    ├── HandDrawnCheckbox.kt
    ├── QuantityStepper.kt
    ├── NotebookMargin.kt       # modifier que dibuja la línea roja de margen
    └── DoodleDivider.kt
```

- **Tema base = Material 3** (`lightColorScheme`/`darkColorScheme`) mapeando: `primary → penBlue`, `error → marginRed`, `background → paper`, `surface → paperElevated`, `onBackground → ink`. Los tokens propios (`paper`, `ink`, highlights de categoría) se exponen vía `CompositionLocal` como extensión del tema.
- **Líneas de cuaderno y margen:** `Modifier.drawBehind` — cero assets de imagen.
- **Tilde/tachado a mano:** `Canvas` con `Path` + `PathMeasure` para la animación de trazo.
- **Fuentes:** `composeResources/font/caveat_*.ttf`, `nunito_*.ttf` — empaquetadas, offline (consistente con ARCHITECTURE §2).
- **Previews:** todo componente con `@Preview` en light y dark — la dualidad de modos es requisito de diseño, no opción.

---

## 11. Resumen de decisiones

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| 1 | Manuscrita (Caveat) en títulos + sans (Nunito) en cuerpo | Todo manuscrito / solo acentos | Identidad sin sacrificar legibilidad en uso real |
| 2 | Paleta "libreta clásica" (papel crema, tinta azul, margen rojo) | Blanco puro minimalista / marcadores vivos | Metáfora reconocible y sobria |
| 3 | Metáfora moderada (líneas, tildes, stickers) | Sutil (solo tipografía) / full libreta (texturas, espiral) | Identidad fuerte sin saturar ni encarecer assets |
| 4 | Dark mode "libreta nocturna" (papel oscuro cálido) | Gris estándar M3 / pizarra | La metáfora sobrevive al dark mode |
| 5 | Fuentes empaquetadas (Caveat + Nunito, OFL) | Fuentes descargables en runtime | App offline-first (ARCHITECTURE §2) |
| 6 | Detalles dibujados con `drawBehind`/`Canvas` | Assets de imagen | Cero peso de recursos, escalable a cualquier densidad |
