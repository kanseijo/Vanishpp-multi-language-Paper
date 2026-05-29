# Phase 3 Implementation Guide — ConfigGUI Step-by-Step

**Document Purpose:** Step-by-step implementation checklist for Phase 3  
**Format:** Code locations, exact changes, testing per step  
**Review Before Coding:** Ensure architect makes sense, then proceed

---

## Step 1: Create ConfigCategory Enum (100 lines)

**File:** `vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/gui/ConfigCategory.java`

**Responsibilities:**
- Define all config categories
- Map config keys to categories
- Store setting metadata (type, min, max, description)

**Implementation:**
```java
public enum ConfigCategory {
    GENERAL("General Settings", new String[]{
        "vanish-delay-ticks",
        "double-shift-window",
        "spectator-gamemode",
        // ... other general settings
    }),
    STORAGE("Database & Storage", new String[]{
        "storage.type",
        "storage.yaml.file",
        // ... storage settings
    }),
    // ... other categories
}
```

**Nested ConfigValue class:**
```java
public static class ConfigValue {
    public String key;
    public String type;      // "boolean", "numeric", "string"
    public Object current;
    public int minBound;     // for numeric
    public int maxBound;     // for numeric
    public String description;
}
```

**Checklist:**
- [ ] All config keys mapped to categories
- [ ] Min/max bounds set for all numeric values
- [ ] Descriptions populated from config.yml comments
- [ ] No typos in key names
- [ ] Compiles without errors

---

## Step 2: Create ConfigRenderer Class (300-400 lines)

**File:** `vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/gui/ConfigRenderer.java`

**Responsibilities:**
- Calculate inventory layout
- Build ItemStacks for display
- Handle pagination

**Key Methods:**

### `buildCategoryInventory(String category, int page)`
```
Returns: Inventory with proper layout
Algorithm:
1. Create 54-slot inventory
2. Place category tabs in row 0 (slots 0-8)
3. Leave row 1 empty
4. Get all settings for category
5. Calculate pagination (max 7 per row)
6. Place settings starting row 2 with 2-indent wrapping
7. Place navigation in row 5
```

### `createCategoryTab(String category, boolean isActive)`
```
Returns: ItemStack representing category tab
Material:
  - Active: YELLOW_STAINED_GLASS
  - Inactive: BLUE_STAINED_GLASS
Display Name: Category name
Lore: Setting count in category
```

### `createSettingItem(ConfigValue value)`
```
For BOOLEAN:
  Material: REDSTONE (true) or GRAY_CONCRETE (false)
  Display: Setting name
  Lore: "Current: TRUE/FALSE" | "Click to toggle"

For NUMERIC:
  Material: ORANGE_CONCRETE
  Display: Setting name
  Lore: 
    "Current: 150"
    "Left-Click: -1"
    "Right-Click: +1"
    "Shift-Click: ±10"
    "Min: 0, Max: 1000"
```

### `createNumericControl(ConfigValue value)`
```
Returns: ItemStack for numeric adjustment
Material: COMPARATOR
Lore shows current value with color coding:
  - Green if mid-range
  - Yellow if approaching boundary
  - Red if at boundary
```

**Checklist:**
- [ ] Layout calculation correct (7 per row, 2-indent wrap)
- [ ] All material choices visually distinct
- [ ] Pagination logic handles edge cases (empty categories, last page)
- [ ] Lore text formatted correctly with color codes
- [ ] No inventory overflow (never place beyond slot 53)

---

## Step 3: Create ConfigGUI Main Class (400-500 lines)

**File:** `vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/gui/ConfigGUI.java`

**Responsibilities:**
- Manage player state (category, page, open viewers)
- Handle all click events
- Save changes to config
- Play sounds

**Key State Maps:**
```java
private final Map<UUID, String> playerCategory;      // Current category per player
private final Map<UUID, Integer> playerPage;         // Current page per player
private final Set<UUID> openViewers;                 // Who has GUI open
```

**Key Methods:**

### `public void open(Player player)`
```
1. Check permission (vanishpp.config)
2. Set category to GENERAL, page to 0
3. Call render(player)
4. Add to openViewers
```

### `public void render(Player player)`
```
1. Get current category and page
2. Call ConfigRenderer.buildCategoryInventory()
3. Open inventory for player
```

### `@EventHandler onInventoryClick(InventoryClickEvent)`
```
1. Verify event is from ConfigGUI (title check)
2. Cancel default behavior
3. Determine click type (slot, category, setting, navigation)
4. Route to appropriate handler
```

### `private void handleCategoryTabClick(Player, int categoryIndex)`
```
1. Update playerCategory[uuid] to new category
2. Set playerPage[uuid] to 0 (reset to first page)
3. Call render(player)
4. No sound needed (just navigation)
```

### `private void handleSettingClick(Player, ConfigValue, boolean isRightClick)`
```
If BOOLEAN:
  1. Toggle value (true → false, false → true)
  2. Save to config via ConfigManager.setAndSave()
  3. Play SUCCESS sound
  4. Re-render
  
If NUMERIC:
  1. Determine delta: -1, +1, -10, or +10 (based on click/shift)
  2. Calculate newValue = current + delta
  3. Call enforceMinMax(key, newValue)
  4. If valid: save, play SUCCESS, re-render
  5. If invalid: play BOUNDARY, don't change
```

### `private boolean enforceMinMax(String key, int newValue)`
```
1. Get ConfigValue for key
2. Check: newValue >= min && newValue <= max
3. Return true if valid, false if out of bounds
4. Log warning if rejected
```

### `private void playSound(Player player, ConfigSound soundType)`
```
1. Check if sounds enabled in config
2. Get sound from ConfigSound enum
3. player.playSound(location, sound, volume, pitch)
4. Catch exceptions silently (sounds optional)
```

### `private void saveConfig(String key, Object value)`
```
1. Call plugin.getConfigManager().setAndSave(key, value)
2. If proxy detected: forward change via proxy bridge
3. Log change for audit trail (optional)
```

**Checklist:**
- [ ] All click event routes correct
- [ ] Permission checks in place
- [ ] State maps updated correctly
- [ ] Sound plays on all appropriate actions
- [ ] Config saves synchronously (no async issues)
- [ ] Re-render happens after every change
- [ ] No NPE on edge cases (null checks everywhere)

---

## Step 4: Create config-gui.yml Configuration File

**File:** `vanishpp-paper/src/main/resources/config-gui.yml`

**Content:**
```yaml
# GUI Configuration for /vconfig GUI

gui:
  # Sound settings
  sounds:
    enabled: true
    success: "ENTITY_EXPERIENCE_ORB_PICKUP"
    boundary: "BLOCK_REDSTONE_TORCH_CLICK_OFF"
    error: "ENTITY_VILLAGER_NO"
    volume: 1.0
    pitch: 1.0

  # Color scheme (material choices)
  colors:
    category-active: "YELLOW_STAINED_GLASS"
    category-inactive: "BLUE_STAINED_GLASS"
    boolean-true: "LIME_CONCRETE"
    boolean-false: "RED_CONCRETE"
    numeric: "ORANGE_CONCRETE"
    navigation: "GRAY_STAINED_GLASS"

  # Layout settings
  layout:
    items-per-row: 7
    indent-wrapping: 2
    category-row: 0
    spacer-row: 1
    settings-start-row: 2
    navigation-row: 5

# Per-setting bounds and metadata
settings:
  vanish-delay-ticks:
    type: "numeric"
    min: 0
    max: 200
    description: "Delay before vanish takes effect"
  
  double-shift-window:
    type: "numeric"
    min: 50
    max: 1000
    description: "Window for double-shift detection"
  
  # ... more settings ...
```

**Checklist:**
- [ ] All sounds have valid Bukkit Sound names
- [ ] All materials have valid Bukkit Material names
- [ ] All settings have bounds defined
- [ ] No comments with special characters
- [ ] File parses cleanly (no YAML errors)

---

## Step 5: Modify VanishConfigCommand (5 lines)

**File:** `vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/commands/VanishConfigCommand.java`

**Change:**
Add subcommand check at start of onCommand():
```java
// After permission check, before args.length check:
if (args.length >= 1 && args[0].equalsIgnoreCase("gui")) {
    new ConfigGUI(plugin).open((Player) sender);
    return true;
}
```

**Checklist:**
- [ ] `/vconfig gui` works
- [ ] Only players can use (not console)
- [ ] Opens ConfigGUI successfully
- [ ] Existing `/vconfig <key> <value>` still works

---

## Step 6: Register ConfigGUI in Vanishpp.java (2 lines)

**File:** `vanishpp-paper/src/main/java/net/thecommandcraft/vanishpp/Vanishpp.java`

**Change:**
In onEnable() or similar registration section:
```java
// Register ConfigGUI event listener
new ConfigGUI(this);  // Constructor registers events
```

**Checklist:**
- [ ] ConfigGUI registered without errors
- [ ] Plugin starts up successfully
- [ ] No duplicate event handler warnings

---

## Step 7: Test Phase 3 (All 8 Issues)

### Test 1: Non-Boolean Value Adjustment
```
1. Open /vconfig gui
2. Find numeric value (e.g., vanish-delay-ticks)
3. Left-click: decrease by 1
4. Right-click: increase by 1
5. Shift+Left: decrease by 10
6. Shift+Right: increase by 10
7. Verify: value changes, sound plays
```
✅ **PASS:** All adjustments work

### Test 2: Min/Max Boundary Enforcement
```
1. Find numeric setting with min=50, max=1000
2. Click left until value = 50
3. Click left again
4. Verify: value stays 50, BOUNDARY sound plays
5. Click right until value = 1000
6. Click right again
7. Verify: value stays 1000, BOUNDARY sound plays
```
✅ **PASS:** Boundaries enforced

### Test 3: Category Organization
```
1. Open /vconfig gui
2. Verify Row 0 has category tabs
3. Verify Row 1 is empty (spacer)
4. Verify Row 2+ has settings
5. Click different category tab
6. Verify: settings change, page resets to 0
```
✅ **PASS:** Categories work

### Test 4: Setting Wrapping
```
1. Go to category with >7 settings
2. Verify: first 7 displayed
3. Verify: wrapping with 2-indent
4. Click NEXT button
5. Verify: next page shown
6. Verify: PREV button available
```
✅ **PASS:** Wrapping and pagination work

### Test 5: Boolean Toggle
```
1. Find boolean setting
2. Left-click
3. Verify: toggles between TRUE/FALSE
4. Verify: SUCCESS sound plays
5. Verify: config file updates
```
✅ **PASS:** Boolean works

### Test 6: Sound Feedback
```
1. Make valid change (boolean or numeric)
2. Verify: SUCCESS sound plays
3. Try to exceed boundary
4. Verify: BOUNDARY sound plays
5. Disable sounds in config
6. Verify: no sounds play
```
✅ **PASS:** Sounds work

### Test 7: Page 2+ Navigation
```
1. Go to category with many settings
2. Navigate through all pages
3. Click on setting on each page
4. Verify: changes take effect from any page
5. Verify: value updates immediately
```
✅ **PASS:** Multi-page works

### Test 8: Permission Checks
```
1. As non-admin player: try /vconfig gui
2. Verify: permission denied message
3. As admin: /vconfig gui
4. Verify: GUI opens
```
✅ **PASS:** Permissions enforced

---

## Build and Deployment

### Build Phase 3
```bash
mvn clean package -DskipTests -q
```

### Verify JAR
```bash
ls -lh vanishpp-paper/target/vanishpp-*.jar
```

### Deploy to Docker
```bash
cp vanishpp-paper/target/vanishpp-*.jar docker/plugins/vanishpp.jar
```

### Test Server
```bash
/vconfig gui  # Should open without errors
```

---

## Code Review Checklist

- [ ] No hardcoded strings (all use LanguageManager)
- [ ] No magic numbers (constants defined)
- [ ] Method names follow convention (camelCase, verb-first)
- [ ] Max 50 lines per method
- [ ] All exceptions caught and handled
- [ ] No console spam (use logger)
- [ ] Thread-safe collections where needed
- [ ] Following Vanishpp code style
- [ ] Null checks on all object references
- [ ] No unused imports or variables

---

## Production Readiness

✅ **Code Quality**
- [ ] Compiles cleanly (zero warnings)
- [ ] Follows existing conventions
- [ ] Well-organized and readable

✅ **Functionality**
- [ ] All 8 issues fixed
- [ ] All tests pass
- [ ] Edge cases handled

✅ **Performance**
- [ ] GUI renders fast (<100ms)
- [ ] Clicks responsive
- [ ] No memory leaks

✅ **Stability**
- [ ] No crashes or errors
- [ ] Config saves reliably
- [ ] Graceful degradation

✅ **Professional**
- [ ] Clean architecture
- [ ] Proper error handling
- [ ] Full logging
- [ ] Ready for clients

---

## Success Confirmation

When all tests pass and code review complete:

```bash
git add -A
git commit -m "feat: Phase 3 complete — ConfigGUI professional redesign

- New inventory-based config GUI with category organization
- Numerical value adjustment with ±1/±10 buttons
- Min/max range enforcement with sound feedback
- Multi-page navigation for settings
- Professional layout with proper wrapping and spacing
- All Phase 3 issues fixed and tested"
```

