# Design — Foodario

> UI design decisions. Companion to [ARCHITECTURE.md](./ARCHITECTURE.md) (how it is built).
> Everything defined here is implemented under `core/presentation` (theme + shared components) in the `shared` module.

---

## 1. Design concept: "The fridge notebook"

The app presents itself as a **hand-written notebook**: that place where you'd write *"1 milk left"* or strike through *"buy eggs"*. The metaphor is not decorative — it reinforces the product's purpose: write things down and check them fast, without friction.

The three principles that govern every decision:

| # | Principle | Origin |
|---|---|---|
| 1 | **Speed first** | CONCEPT: *"Milk → + and done"*. Design must never add steps; aesthetics can't cost seconds. |
| 2 | **Legibility in real context** | Used at the supermarket, one-handed, in sunlight or in the fridge at 11pm. Light and dark mode are first-class citizens. |
| 3 | **The warmth of something hand-made** | The app should feel like *your* notebook, not a stock-management system. Handwriting brings closeness, never friction. |

---

## 2. Foundational decisions (agreed)

| Decision | Resolution |
|---|---|
| **Typography** | Hand-written for titles, quantities and categories + readable sans for body and labels |
| **Light palette** | Classic notebook: cream paper, dark blue ink, red margin as accent, highlighter-style category swatches |
| **Metaphor level** | Moderate: typography + palette + drawn details (notebook lines, hand-drawn checks, sticker chips, doodle dividers). No heavy textures |
| **Dark mode** | "Night notebook": warm dark paper (never pure black), light ink, same accents |

---

## 3. Typography

### 3.1 Families

| Role | Font | Rationale |
|---|---|---|
| **Hand-written** | **Caveat** (Google Fonts, OFL) | One of the most legible hand-written fonts; marker stroke that evokes a fridge note. Loaded weights: Medium / SemiBold / Bold (covers the whole hand-written scale of the app). |
| Hand-written fallback | Patrick Hand, Kalam | Conceptual fallbacks (not bundled) if Caveat doesn't convince in legibility tests. |
| **Body / sans** | **Nunito** (Google Fonts, OFL) | Rounded, friendly sans that harmonizes with the hand-written without competing. Loaded weights: Regular / Medium. |

Both are bundled as resources (`composeResources/font/`); references are made through `Res.font.caveat_*` / `Res.font.nunito_*` and resolved with `FontFamily(...)` inside `core/presentation/theme/Type.kt`. The app is offline — **no runtime font downloads**.

### 3.2 Type scale

| Token | Family | Size | Use |
|---|---|---|---|
| `displayHand` | Caveat Bold | 34sp | Screen titles ("My fridge", "Shopping") |
| `titleHand` | Caveat SemiBold | 24sp | Food name in list and detail |
| `quantityHand` | Caveat Bold | 28sp | Quantity numbers ("2", "300 g") — the most-consulted data point |
| `labelHand` | Caveat Medium | 18sp | Categories, annotations ("expires in 2 days!") |
| `body` | Nunito Regular | 16sp | Descriptions, secondary text |
| `label` | Nunito Medium | 14sp | Buttons, chips, metadata |
| `caption` | Nunito Regular | 12sp | Dates, hints, auxiliary text |

**Hard rule:** the hand-written font **never goes below 18sp**. Below that, legibility collapses and the metaphor becomes friction (principle 2).

---

## 4. Color system

### 4.1 Light mode — "Classic notebook"

| Token | Hex | Role |
|---|---|---|
| `paper` | `#FAF6EE` | Screen background — cream paper |
| `paperElevated` | `#FFFFFF` | Cards, sheets, dialogs |
| `ink` | `#1F2A44` | Primary text — dark blue ink |
| `inkSoft` | `#5A6378` | Secondary text |
| `penBlue` | `#35507E` | Primary: actions, links, FAB |
| `marginRed` | `#D9544F` | Accent: margin line, expiration alerts, delete |
| `pencilGray` | `#9AA1B0` | Borders, dividers, inactive icons |

**Category colors (highlighter style, soft backgrounds with ink on top):**

| Category | Highlight | Emoji (from CONCEPT) |
|---|---|---|
| Dairy | `#F3E3B2` | 🥛 |
| Meat | `#F0C8C0` | 🥩 |
| Vegetables | `#D2E8B8` | 🥦 |
| Fruits | `#F6D3A8` | 🍎 |
| Pantry | `#E4D9C8` | 🥫 |
| Frozen | `#C6E0EC` | 🧊 |
| Beverages | `#DCD2EA` | 🥤 |
| Cooked | `#F2CCD8` | 🍲 |

### 4.2 Dark mode — "Night notebook"

| Token | Hex | Role |
|---|---|---|
| `paper` | `#242119` | Background — warm dark kraft paper (never pure black) |
| `paperElevated` | `#2F2B22` | Cards, sheets |
| `ink` | `#ECE6D8` | Primary text — light ink |
| `inkSoft` | `#A8A294` | Secondary text |
| `penBlue` | `#9DB8DE` | Primary — light ink |
| `marginRed` | `#E08A82` | Accent — same role, brighter version |
| `pencilGray` | `#6B665A` | Borders, dividers |

In dark mode, the category highlights are the same tones at **25–30% opacity** over the dark paper, preserving the color ↔ category association without blowing up contrast.

### 4.3 Color rules

1. **Red (`marginRed`) is reserved for two things:** the notebook margin line and alerts (expiration, delete). Nothing else — its scarcity is what gives it meaning.
2. **The category color never travels alone:** always paired with an emoji or text (accessibility, §9).
3. **WCAG AA minimum contrast** (4.5:1 text, 3:1 large elements) verified in both modes before merging any palette change.

---

## 5. Components

The "moderate" metaphor is materialized in these components (all in `core/presentation/components/`):

### 5.1 `NotebookListItem` — inventory row

```
─────────────────────────────────────────────
 🥛  Milk                    2 units
     Dairy                        ✎
─────────────────────────────────────────────  ← notebook line (1dp, pencilGray 40%)
```

- Separator = **drawn notebook line** (`drawBehind`), not a standard `Divider`.
- Name in `titleHand`, quantity in `quantityHand` — the quantity is the star data point (CONCEPT: *"do I already have this?"*).
- Category emoji as a "stamp" on the left; category in `labelHand` with its highlight behind, highlighter style.
- **Frozen item:** small ❄ icon next to the name.
- **Expiring soon:** margin-note style annotation in `marginRed`: *"expires in 2 days!"* with a little arrow doodle. Never an institutional badge.

### 5.2 `QuickAddBar` — quick add (the key CONCEPT piece)

```
┌─────────────────────────────────────┐
│  ✏️  Type a food…              ➕  │
└─────────────────────────────────────┘
[ 🥛 ] [ 🥩 ] [ 🥦 ] [ 🍎 ] [ 🥫 ] [ 🧊 ] [ 🥤 ]
```

- Text field that looks like a **notebook line** (dotted underline, no box).
- `➕` button in `penBlue`: adds with defaults (quantity 1, unit) — the *"Milk → +"* flow.
- Category chips = **rounded stickers** with emoji, subtle cut-out border, their own highlight. One tap sets the category for the next entry.
- Completing details (expiration, frozen) is always **optional and later**, from the detail screen.

### 5.3 `HandDrawnCheckbox` — shopping list

- Square checkbox with an irregular (drawn) border and **animated hand-drawn check** (stroke drawn in ~200ms) on toggle.
- Checked item: text **struck through with a marker stroke** (slightly irregular line, not straight `textDecoration`) + dimmed.
- **"Add to my fridge"** button: sticky-note style (paperElevated, soft shadow, slight −1° rotation).

### 5.4 `QuantityStepper` — detail

```
        [ − ]   2 units   [ + ]
```

- Circular buttons with pencil stroke; the number in `quantityHand` (28sp) — protagonist of the detail screen.
- Long-pressing `+`/`−` increments/decrements continuously (for one-handed use at the supermarket).

### 5.5 Navigation and global actions

- **Bottom bar** (2 destinations: 🧊 Fridge / 🛒 Shopping) implemented in `navigation/FoodarioNavHost.kt` with type-safe routes (`InventoryRoute`, `ShoppingListRoute`). Labels and selection use `labelHand`; icons are a hand-drawn `FridgeIllustration` and the 🛒 emoji.
- **No global FAB.** Adding items is always done from the `QuickAddBar` at the **top** of each screen's content (`InventoryContent`, `ShoppingListContent`). The selected category for the next quick add is a `StateFlow<FoodCategory>` exposed by the ViewModel (`quickAddCategory`).
- **Item detail** is pushed with `navigate(FoodDetailRoute(itemId))`; on delete, `FoodDetailEffect.Deleted` is emitted and `navController.popBackStack()` closes the screen.
- **Section dividers** = doodles (scribble, arrow, hand-drawn asterisk) instead of straight lines.

---

## 6. Layout and spacing

- **Dimension tokens** live in `FoodarioDimensions` (`core/presentation/theme/Dimensions.kt`) and are the only reference for spacing/sizing: `xxs=2, xs=4, sm=8, md=12, lg=16, xl=24, xxl=32`, plus `touchTarget=48, rowHeight=56, bottomBar=64` (all in `dp`). Base system in multiples of 4; the `2dp` (`xxs`) is reserved for hairline separators and inner icon padding.
- **Notebook margin:** list screens reserve a 40dp left margin with the **vertical red line** (`marginRed`, 1dp, 60% opacity) — applied via `Modifier.notebookMargin()` from `core/presentation/components/NotebookMargin.kt`. Content lives to the right of that line; category emojis can "invade" the margin as annotations.
- Cards with `cornerRadius` 12dp (`dimensions.lg`) and very soft shadow (paper doesn't float high).
- **Touch targets ≥ 48dp** enforced with `dimensions.touchTarget` on every interactive button / icon (principle 2: one-handed use).

---

## 7. Iconography and illustration

- **System icons:** Material Symbols Rounded as a base, but with reduced `strokeWidth` and irregular corners where feasible; in the medium term, an in-house hand-drawn set.
- **Category emojis** (🥛🥩🥦🍎🥫🧊🥤🍲) are part of the design system — they come from CONCEPT and work as "stamps" on the notebook page.
- **Empty states:** hand-drawn doodles (open fridge with a note stuck on it, shopping cart with a scribble) + message in `titleHand`:
  - Empty inventory: *"Your fridge is empty… start writing it down!"*
  - Empty shopping: *"Nothing to buy. Check everything off ✓"*
- **No paper textures or spirals** (moderate level): the metaphor lives in typography, lines and strokes, not in heavy backgrounds.
- **App icon — "Note on the fridge":** ink-stroke fridge with a sticky note (highlight `#F3E3B2`) taped on, plus a hand-drawn check, on `paper` background. It summarizes the two metaphors of the product (fridge + notebook) in a single image. The composition respects the adaptive-icon safe zone (66dp circle).
  - **Android** (`androidApp/src/main/res/`): adaptive icon with `<background>` + `<foreground>` + `<monochrome>` (themed icons Android 13+).
  - **iOS** (`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`): variants `app-icon-1024.png` (light) and `app-icon-dark-1024.png` (dark).
  - **Editable sources** (`docs/icon-concepts/`): in this snapshot the directory is empty — vector masters are not versioned; when added, use the names `icon-final-*.svg` so the pipeline can find them.

---

## 8. Motion

| Interaction | Animation |
|---|---|
| Quick add | Item "writes itself" into the list: fade + soft slide (150–200ms), no exaggerated bounce |
| Checkbox tick | Stroke drawn (200ms) |
| Strike-through | Marker stroke animated (250ms) |
| Quantity change | Number "jumps" slightly (scale 1.0 → 1.15 → 1.0, 150ms) |
| Navigation | Standard M3 transitions — personality lives in the components, not in exotic transitions |

**Rule:** no animation exceeds 300ms or blocks an action (principle 1). Respect system `Reduce Motion` by disabling stroke animations.

---

## 9. Accessibility

1. **Hand-written with limits:** only ≥ 18sp, never in body text or small critical info (§3.2).
2. **AA contrast** in both modes (§4.3).
3. **Don't rely on color alone:** expiration = color + text + doodle; categories = color + emoji + name.
4. **Content descriptions** on every icon/emoji with semantic role (decorative emoji is excluded from the accessibility tree).
5. **Dynamic type:** the type scale respects the system fontScale; hand-written scales the same as sans.
6. **Touch targets ≥ 48dp** and stepper usable with one hand (§5.4).

---

## 10. Compose implementation

Direct mapping with the architecture (`core/presentation`):

```
core/presentation/
├── theme/
│   ├── FoodarioTheme.kt        # lightColorScheme / darkColorScheme + FoodarioColors (CompositionLocal)
│   ├── Color.kt                # §4 tokens (paper* / ink* / penBlue* / marginRed* / pencilGray*)
│   ├── Type.kt                 # §3.2 scale (Caveat + Nunito via Res.font.*)
│   ├── Dimensions.kt           # §6 tokens (xxs … xxl + touchTarget/rowHeight/bottomBar)
│   ├── CategoryColors.kt       # FoodCategory → highlight map (shared with the UI)
│   └── ThemePreview.kt         # master light/dark preview
├── components/
│   ├── NotebookListItem.kt + NotebookListItemPreview.kt
│   ├── QuickAddBar.kt                       # quick add (CONCEPT §3.2)
│   ├── HandDrawnCheckbox.kt                 # animated hand-drawn check (CONCEPT §3.3)
│   ├── QuantityStepper.kt + QuantityStepperPreview.kt   # with repeat-press (hold-to-step)
│   ├── NotebookMargin.kt                    # Modifier.notebookMargin() — red line + padding
│   ├── DoodleDivider.kt                     # scribble separators
│   ├── CategoryGrid.kt + CategoryEmoji.kt   # category grid (sticker chips)
│   └── FridgeIllustration.kt                # hand-drawn SVG-equivalent (bottom-bar icon)
└── preview/
    └── PreviewUseCases.kt                   # fake data for previews: `PreviewFoodItems.milk`, …
```

- **Theme base = Material 3** (`lightColorScheme`/`darkColorScheme`) mapping: `primary → penBlue`, `error → marginRed`, `background → paper`, `surface → paperElevated`, `onBackground → ink`. The custom tokens (`paper`, `ink`, category highlights) are exposed via `CompositionLocal` (`LocalFoodarioColors`, `LocalFoodarioTypography`, `LocalFoodarioDimensions` implicitly via `FoodarioTheme.dimensions`).
- **Notebook lines and margin:** `Modifier.drawBehind` — zero image assets.
- **Hand-drawn check / strike:** `Canvas` with `Path` + `PathMeasure` for stroke animation.
- **Stepper with "hold to step":** `QuantityStepper` wraps `pointerInput { awaitEachGesture { … delay(400) while (held) currentAction(); delay(90) } }` — initial delay 400ms, then 90ms between repeats. Lets you sweep through the range with one finger, in line with principle 2 (one-handed use).
- **Contrast verified by test:** `commonTest/.../core/presentation/theme/ColorContrastTest.kt` runs in CI and validates AA (4.5:1 text, 3:1 large elements) on the `paper/ink/penBlue/marginRed` palette in both modes. Palette changes that break the test don't get merged.
- **Fonts:** `composeResources/font/caveat_*.ttf`, `nunito_*.ttf` — bundled, offline (consistent with ARCHITECTURE §2).
- **Previews:** every component has `@Preview` light + dark (some are in separate `*Preview.kt` files). `ThemePreview.kt` gives a panoramic view of the full palette. Dual-mode is a design requirement, not a nice-to-have.

---

## 11. Decision summary

| # | Decision | Discarded alternative | Reason |
|---|---|---|---|
| 1 | Hand-written (Caveat) for titles + sans (Nunito) for body | All hand-written / accents only | Identity without sacrificing real-world legibility |
| 2 | "Classic notebook" palette (cream paper, blue ink, red margin) | Pure white minimal / bright markers | Recognizable and sober metaphor |
| 3 | Moderate metaphor (lines, checks, stickers) | Subtle (typography only) / full notebook (textures, spiral) | Strong identity without saturating or paying for assets |
| 4 | "Night notebook" dark mode (warm dark paper) | Standard M3 grey / slate | Metaphor survives dark mode |
| 5 | Bundled fonts (Caveat + Nunito, OFL) | Runtime-downloaded fonts | Offline-first app (ARCHITECTURE §2) |
| 6 | Drawn details via `drawBehind`/`Canvas` | Image assets | Zero resource weight, scales to any density |
| 7 | "Note on the fridge" icon (§7) | Just a check / "F" monogram / milk carton | The only proposal that fuses domain (fridge) with identity (note/notebook) |
