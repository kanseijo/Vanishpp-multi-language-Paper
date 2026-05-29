# Phase 3 Architecture — ConfigGUI Redesign

**Scope:** Professional-grade inventory-based config GUI with categories, numerical controls, and responsive layout  
**Target:** Production-ready, clean code, fully tested  
**Estimated:** 4-5 hours implementation + testing

---

## Design Overview

### Current State
- `/vconfig <key> <value>` — Command-line config editor
- No GUI
- No category organization
- Boolean-only easy editing

### Target State
**Professional Config GUI** with:
- Category-based organization (inventory tabs)
- Wrapping layout for settings
- Numerical value adjustment (±1, ±10 buttons)
- Min/max range enforcement
- Sound feedback (success/boundary)
- Multi-page support (all settings fit)
- Fully responsive and intuitive

---

## File Structure

### New Files to Create

```
vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/gui/
├── ConfigGUI.java              [Main config GUI orchestrator, 400-500 lines]
├── ConfigCategory.java         [Enum: categories + settings mapping, 100 lines]
└── ConfigRenderer.java         [Layout rendering logic, 300-400 lines]

vanishpp-paper/src/main/resources/
└── config-gui.yml              [GUI settings: sounds, colors, limits, 50 lines]
```

### Modified Files
- `VanishConfigCommand.java` — Add `/vconfig gui` subcommand (5 lines)
- `Vanishpp.java` — Register ConfigGUI event listener (2 lines)
- `config.yml` — Add min/max bounds for each value (see config-gui.yml)

---

## Core Classes

### 1. ConfigGUI.java (400-500 lines)

**Responsibilities:**
- Manage inventory state per player
- Handle click events (category tabs, ±buttons, value displays)
- Track page numbers, current category, numeric input mode
- Sound feedback

**Key Methods:**
```java
public void open(Player player);                    // Entry point
public void render(Player player);                  // Redraw inventory
private void handleCategoryTabClick(...);           // Tab switching
private void handleValueClick(...);                 // ±1/±10 buttons
private void playSound(Player, SoundType);         // Sound feedback
private void enforceMinMax(key, newValue);         // Boundary check
```

**State Tracking:**
```java
Map<UUID, Integer> currentPage;              // Page per player
Map<UUID, String> currentCategory;           // Active category
Map<UUID, Integer> numericEditMode;          // For number input
Set<UUID> openViewers;                       // Who has GUI open
```

### 2. ConfigCategory.java (100 lines)

**Enum with categorized config keys:**
```java
GENERAL("General Settings"),
STORAGE("Database & Storage"),
PERMISSIONS("Permission System"),
SPECTATOR("Spectator Mode"),
VISIBILITY("Visibility & Rendering"),
PERFORMANCE("Performance Tuning");

// Each category has its settings list
// Settings include: key, type, min, max, description
```

### 3. ConfigRenderer.java (300-400 lines)

**Responsibilities:**
- Calculate layout (categories, wrapped settings)
- Build ItemStacks with display names and lore
- Manage pagination
- Apply color schemes

**Layout Algorithm:**
```
Row 0:    [CAT1] [CAT2] [CAT3] [CAT4] ...     (Category tabs)
Row 1:    (empty spacer)
Rows 2+:  Settings (max 7 per row, wrap with 2-indent)
          [Value] [Setting Name] [Setting]
```

**Key Methods:**
```java
Inventory renderCategory(String category, int page);
List<ItemStack> layoutSettings(List<ConfigValue>);
ItemStack createCategoryTab(String category, boolean active);
ItemStack createSettingItem(ConfigValue);
ItemStack createNumericControl(ConfigValue);
```

---

## Data Structures

### ConfigValue Class
```java
public class ConfigValue {
    String key;                          // e.g., "vanish-delay-ticks"
    ConfigType type;                     // BOOLEAN, NUMERIC, STRING
    Object currentValue;
    Object defaultValue;
    int minBound;                        // For numeric: min allowed
    int maxBound;                        // For numeric: max allowed
    String description;                  // Shown in lore
    String category;                     // Which tab it belongs to
}

public enum ConfigType {
    BOOLEAN,    // Toggle true/false
    NUMERIC,    // Integer with ±buttons
    STRING;     // Text field (for future: editable)
}
```

### ConfigSound Enum
```java
public enum ConfigSound {
    SUCCESS(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f),    // Value changed
    BOUNDARY(Sound.BLOCK_REDSTONE_TORCH_CLICK_OFF, 0.8f), // Limit reached
    ERROR(Sound.ENTITY_VILLAGER_NO, 0.9f);               // Change rejected

    public void play(Player player);
}
```

---

## Layout Details

### Inventory Layout (54 slots = 6 rows × 9 cols)

```
┌─────────────────────────────────┐
│ [CAT1][CAT2][CAT3][CAT4][CAT5] │  Row 0: Categories
├─────────────────────────────────┤
│ (empty spacer)                  │  Row 1: Visual break
├─────────────────────────────────┤
│ [Setting1] [Setting2] [Setting3]│  Row 2: Settings
│ [Setting4] [Setting5] [Setting6]│  (max 7 per row)
│ [Setting7]                      │
│ [Setting8] [Setting9] [Setting10]│ Row 3-4: Wrapped
│ ...                             │
├─────────────────────────────────┤
│ [<PREV]        [INFO]        [NEXT>]  Row 5: Navigation
└─────────────────────────────────┘
```

### Setting Item Display (for numeric)

```
Single Slot Format:
[Display Name]
[Current Value: 150]
[Adjust: Click -1 | [Value] | +1]
[Shift-Click: -10 or +10]
[Min: 0, Max: 1000]
```

For boolean:
```
[Display Name]
[Current: TRUE]
[Click to toggle]
```

---

## Click Logic

### Row 0 (Categories)
- Left-click → Switch category (re-render)
- Visual feedback: highlighted material for active category

### Row 2+ (Settings)
- For BOOLEAN:
  - Left-click → Toggle true/false, save, play sound
- For NUMERIC:
  - Left-click → Decrease by 1
  - Right-click → Increase by 1
  - Shift+Left → Decrease by 10
  - Shift+Right → Increase by 10
  - Boundary reached → Play BOUNDARY sound, don't change

### Row 5 (Navigation)
- [<PREV] → Previous page (if page > 0)
- [INFO] → Display info (current category, total settings, etc.)
- [NEXT>] → Next page (if more settings available)

---

## Sound Feedback

**Configured in config-gui.yml:**
```yaml
gui:
  sounds:
    success: "ENTITY_EXPERIENCE_ORB_PICKUP"  # Value successfully changed
    boundary: "BLOCK_REDSTONE_TORCH_CLICK_OFF" # Min/max limit reached
    error: "ENTITY_VILLAGER_NO"             # Change rejected (invalid)
  enabled: true
  volume: 1.0
  pitch: 1.0
```

---

## Min/Max Range Configuration

**In config.yml, each value gets bounds:**
```yaml
vanish-delay-ticks:
  type: numeric
  default: 0
  min: 0
  max: 200

double-shift-window:
  type: numeric
  default: 150
  min: 50
  max: 1000
```

**Bounds can be:**
- Positive integers
- Allow negative (if meaningful)
- Allow overflow/underflow (no auto-clamping, but GUI enforces)

---

## Error Handling

**Invalid Changes:**
1. Value out of bounds → Play ERROR sound, reject change, show "Out of range" in lore
2. Type mismatch → Catch in handler, play ERROR sound
3. Permission denied → Close GUI with message
4. Config file I/O error → Log warning, keep memory version, show error msg

---

## Testing Checklist

- [ ] Category switching works (all 5+ categories)
- [ ] Settings wrap correctly (7 per row, 2-indent)
- [ ] Boolean toggle changes value and plays sound
- [ ] Numeric ±1 button works
- [ ] Numeric ±10 button works
- [ ] Boundary enforcement (can't go below min/above max)
- [ ] Sound plays on valid change (SUCCESS)
- [ ] Sound plays on boundary (BOUNDARY)
- [ ] Page 2+ works (all pages clickable)
- [ ] Navigation buttons work
- [ ] Values persist to config.yml on change
- [ ] Proxy sync works (if multi-server)
- [ ] Permission checks work
- [ ] Responsiveness (no lag opening/switching)

---

## Code Quality Standards

✅ **Clean Code:**
- Method names: `createNumericItem()`, `calculateLayout()`, `enforceMinMax()`
- No magic numbers (use constants)
- Docstrings on public methods only
- Max 50 lines per method

✅ **Professional:**
- No debug prints (use logger if needed)
- Proper exception handling
- Thread-safe where needed (concurrent maps for player state)
- Follows existing codebase style (Vanishpp conventions)

✅ **Responsive:**
- Inventory renders in <100ms
- Click feedback immediate
- No blocking I/O in event handlers (save config async)

✅ **Production-Ready:**
- Full localization support (messages from lang/en.yml)
- Fallback values for missing config
- Graceful degradation if sounds disabled
- No hardcoded strings (use LanguageManager)

---

## Success Criteria

1. ✅ All 8 Phase 3 issues from checklist fixed
2. ✅ GUI layout professional and intuitive
3. ✅ All config values editable via GUI
4. ✅ Numerical controls with ±1/±10
5. ✅ Min/max enforcement with feedback
6. ✅ Sound feedback configurable
7. ✅ Multi-page navigation works
8. ✅ No errors in startup/gameplay
9. ✅ Code passes style checks
10. ✅ Ready for production deployment

