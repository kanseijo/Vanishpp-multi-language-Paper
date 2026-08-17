package net.thecommandcraft.vanishpp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.thecommandcraft.vanishpp.Vanishpp;
import net.thecommandcraft.vanishpp.utils.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Renders the ConfigGUI inventory with categories, settings, and pagination.
 * Handles layout calculations, item creation, and responsive design.
 */
public class ConfigRenderer {

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_ROW = 7;
    private static final int INDENT_WRAPPING = 2;
    private static final int CATEGORY_ROW = 0;
    private static final int SPACER_ROW = 1;
    private static final int SETTINGS_START_ROW = 2;
    private static final int NAVIGATION_ROW = 5;
    private static final int SETTINGS_CONTENT_ROWS = 3;  // Rows 2, 3, 4
    private static final int ITEMS_PER_PAGE = (SETTINGS_CONTENT_ROWS * 9) - INDENT_WRAPPING;

    private static final Material CATEGORY_ACTIVE = Material.YELLOW_STAINED_GLASS;
    private static final Material CATEGORY_INACTIVE = Material.BLUE_STAINED_GLASS;
    private static final Material BOOLEAN_TRUE = Material.LIME_CONCRETE;
    private static final Material BOOLEAN_FALSE = Material.RED_CONCRETE;
    private static final Material NUMERIC = Material.ORANGE_CONCRETE;
    private static final Material NAVIGATION = Material.GRAY_STAINED_GLASS;

    private final Vanishpp plugin;
    private final LanguageManager lang;

    public ConfigRenderer(Vanishpp plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    /**
     * Build the inventory for a specific category and page, returning both inventory and slot mapping.
     *
     * @param category Current category to display
     * @param page Current page number
     * @return Object array: [Inventory, Map<Integer slot, String key>]
     */
    public Object[] buildCategoryInventory(String category, int page) {
        String title = lang.getMessage("gui.config.title", "category", getCategoryDisplayName(category));
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, plugin.getMessageManager().parse(title, null));
        Map<Integer, String> slotToKey = new HashMap<>();

        // Row 0: Category tabs
        placeCategoryTabs(inv, category);

        // Rows 2+: Settings with wrapping layout
        ConfigCategory cat = ConfigCategory.valueOf(category);
        List<ConfigCategory.ConfigValue> settings = new ArrayList<>(cat.getSettings().values());
        placeSettings(inv, settings, page, slotToKey);

        // Row 5: Navigation buttons
        placeNavigation(inv, page, settings.size());

        return new Object[]{inv, slotToKey};
    }

    /**
     * Place category tabs in row 0.
     */
    private void placeCategoryTabs(Inventory inv, String activeCategory) {
        int slot = 0;
        for (ConfigCategory category : ConfigCategory.values()) {
            if (slot >= 9) break;  // Only 9 slots in row 0
            boolean isActive = category.name().equals(activeCategory);
            String displayName = getCategoryDisplayName(category.name());
            ItemStack tab = createCategoryTab(displayName, isActive);
            inv.setItem(slot++, tab);
        }
    }

    /**
     * Create a category tab item.
     */
    private ItemStack createCategoryTab(String categoryName, boolean isActive) {
        ItemStack item = new ItemStack(isActive ? CATEGORY_ACTIVE : CATEGORY_INACTIVE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component parsed = plugin.getMessageManager().parse(categoryName, null);
            if (isActive) {
                parsed = parsed.colorIfAbsent(NamedTextColor.YELLOW);
            } else {
                parsed = parsed.colorIfAbsent(NamedTextColor.BLUE);
            }
            meta.displayName(parsed.decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Place settings with wrapping layout and pagination.
     */
    private void placeSettings(Inventory inv, List<ConfigCategory.ConfigValue> allSettings, int page,
                               Map<Integer, String> slotToKey) {
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allSettings.size());

        if (startIndex >= allSettings.size()) return;

        int slot = SETTINGS_START_ROW * 9;
        int row = SETTINGS_START_ROW;
        int colInRow = 0;

        for (int i = startIndex; i < endIndex; i++) {
            ConfigCategory.ConfigValue value = allSettings.get(i);

            // Wrap with 2-indent if exceeding items per row
            if (colInRow >= ITEMS_PER_ROW) {
                row++;
                slot = row * 9 + INDENT_WRAPPING;
                colInRow = 0;
            }

            // Safety check: don't overflow inventory (BEFORE placing item)
            if (slot >= NAVIGATION_ROW * 9) break;

            ItemStack setting = createSettingItem(value);
            inv.setItem(slot, setting);
            slotToKey.put(slot, value.key);  // Track slot->key mapping

            slot++;
            colInRow++;
        }
    }

    /**
     * Create a setting item based on its type.
     */
    private ItemStack createSettingItem(ConfigCategory.ConfigValue value) {
        ItemStack item;

        if (value.type.isBoolean()) {
            // Boolean: show true/false state
            Object defaultVal = value.defaultValue;
            boolean isTrue = defaultVal instanceof Boolean && (Boolean) defaultVal;
            item = new ItemStack(isTrue ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        } else if (value.type.isNumeric()) {
            // Numeric: show range and current value
            item = new ItemStack(NUMERIC);
        } else {
            // String: display only
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Display name
            meta.displayName(Component.text(value.key, NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));

            // Lore with instructions
            List<Component> lore = new ArrayList<>();
            // Description (still from enum, but we can later externalize)
            lore.add(Component.text(value.description, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            if (value.type.isBoolean()) {
                Object val = value.defaultValue;
                String status = (val instanceof Boolean && (Boolean) val) ? "§aTRUE" : "§cFALSE";
                String currentMsg = lang.getMessage("gui.config.setting.current", "value", status);
                lore.add(plugin.getMessageManager().parse(currentMsg, null));
                String toggleMsg = lang.getMessage("gui.config.setting.click_toggle");
                lore.add(plugin.getMessageManager().parse(toggleMsg, null));
            } else if (value.type.isNumeric()) {
                Object val = value.defaultValue;
                String current = val != null ? String.valueOf(val) : "?";
                String currentMsg = lang.getMessage("gui.config.setting.current", "value", current);
                lore.add(plugin.getMessageManager().parse(currentMsg, null));
                lore.add(Component.empty());
                String adjustMsg = lang.getMessage("gui.config.setting.adjust");
                lore.add(plugin.getMessageManager().parse(adjustMsg, null));
                String shiftMsg = lang.getMessage("gui.config.setting.shift_adjust");
                lore.add(plugin.getMessageManager().parse(shiftMsg, null));
                lore.add(Component.empty());
                String rangeMsg = lang.getMessage("gui.config.setting.range",
                        "min", String.valueOf(value.minBound),
                        "max", String.valueOf(value.maxBound));
                lore.add(plugin.getMessageManager().parse(rangeMsg, null));
            } else {
                String displayOnlyMsg = lang.getMessage("gui.config.setting.display_only");
                lore.add(plugin.getMessageManager().parse(displayOnlyMsg, null));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Place navigation buttons in row 5.
     */
    private void placeNavigation(Inventory inv, int currentPage, int totalSettings) {
        int totalPages = (totalSettings + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

        // Previous button (slot 45)
        if (currentPage > 0) {
            String label = lang.getMessage("gui.config.navigation.prev");
            ItemStack prev = createNavigationButton(label, NAVIGATION);
            inv.setItem(45, prev);
        }

        // Info button (slot 49)
        String info = lang.getMessage("gui.config.navigation.info",
                "page", String.valueOf(currentPage + 1),
                "total", String.valueOf(totalPages));
        ItemStack infoItem = createNavigationButton(info, NAVIGATION);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(plugin.getMessageManager().parse(info, null)
                    .colorIfAbsent(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(49, infoItem);

        // Next button (slot 53)
        if (currentPage < totalPages - 1) {
            String label = lang.getMessage("gui.config.navigation.next");
            ItemStack next = createNavigationButton(label, NAVIGATION);
            inv.setItem(53, next);
        }
    }

    /**
     * Create a navigation button.
     */
    private ItemStack createNavigationButton(String label, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(label, null)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get the slot index for a category tab by name.
     */
    public int getCategoryTabSlot(String categoryName) {
        int index = 0;
        for (ConfigCategory cat : ConfigCategory.values()) {
            if (cat.name().equals(categoryName)) {
                return index;
            }
            index++;
            if (index >= 9) break;
        }
        return -1;
    }

    /**
     * Determine if a slot is a category tab (row 0).
     */
    public boolean isCategoryTab(int slot) {
        return slot >= 0 && slot < 9;
    }

    /**
     * Determine if a slot is a navigation button (row 5).
     */
    public boolean isNavigation(int slot) {
        return slot >= 45 && slot < 54;
    }

    /**
     * Get category name from tab slot.
     */
    public String getCategoryFromSlot(int slot) {
        ConfigCategory[] cats = ConfigCategory.values();
        if (slot >= 0 && slot < cats.length) {
            return cats[slot].name();
        }
        return null;
    }

    /**
     * Calculate total pages for a category.
     */
    public int getTotalPages(String categoryName) {
        ConfigCategory cat = ConfigCategory.valueOf(categoryName);
        int totalSettings = cat.getSettingCount();
        return Math.max(1, (totalSettings + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    // Helper: get display name from language file
    private String getCategoryDisplayName(String categoryName) {
        return lang.getMessage("gui.config.category." + categoryName);
    }
}