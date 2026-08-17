package net.thecommandcraft.vanishpp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.thecommandcraft.vanishpp.Vanishpp;
import net.thecommandcraft.vanishpp.utils.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RulesGUI implements Listener {

    private final Vanishpp plugin;
    private final Map<UUID, UUID> openGuis = new HashMap<>();

    public RulesGUI(Vanishpp plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer, Player target) {
        LanguageManager lang = plugin.getLanguageManager();
        String titlePrefix = lang.getMessage("gui.rules.title");
        String title = titlePrefix + target.getName();

        List<String> rules = sortedRules();
        int size = ((rules.size() / 9) + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, Math.max(size, 9),
                plugin.getMessageManager().parse(title, null));

        for (int i = 0; i < rules.size(); i++) {
            inv.setItem(i, buildItem(target, rules.get(i)));
        }

        openGuis.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID viewerUuid = viewer.getUniqueId();
        if (!openGuis.containsKey(viewerUuid)) return;

        LanguageManager lang = plugin.getLanguageManager();
        String titlePrefix = lang.getMessage("gui.rules.title");
        String viewTitle = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        String plainPrefix = titlePrefix.replaceAll("§[0-9a-fk-or]", "");
        if (!viewTitle.contains(plainPrefix)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        UUID targetUuid = openGuis.get(viewerUuid);
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) { viewer.closeInventory(); return; }

        String displayName = LegacyComponentSerializer.legacySection().serialize(meta.displayName());
        String chineseName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();

        String ruleName = getRuleKeyFromDisplay(chineseName);
        if (ruleName == null || !plugin.getRuleManager().getAvailableRules().contains(ruleName)) return;

        if (!viewer.hasPermission("vanishpp.rules")
                && (!viewer.equals(target) || !viewer.hasPermission("vanishpp.rules.others"))) return;

        boolean current = plugin.getRuleManager().getRule(target, ruleName);
        plugin.getRuleManager().setRule(target, ruleName, !current);

        event.getClickedInventory().setItem(event.getSlot(), buildItem(target, ruleName));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack buildItem(Player target, String rule) {
        LanguageManager lang = plugin.getLanguageManager();

        boolean enabled = plugin.getRuleManager().getRule(target, rule);
        Material mat = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = getRuleDisplayName(rule);
            Component displayComponent = plugin.getMessageManager().parse(displayName, null);
            displayComponent = displayComponent.colorIfAbsent(
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED);
            meta.displayName(displayComponent.decoration(TextDecoration.ITALIC, false));

            String statusKey = enabled ? "gui.rules.status_enabled" : "gui.rules.status_disabled";
            String status = lang.getMessage(statusKey);
            String toggleHint = lang.getMessage("gui.rules.toggle_hint");

            meta.lore(List.of(
                    plugin.getMessageManager().parse(status, null)
                            .colorIfAbsent(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                    plugin.getMessageManager().parse(toggleHint, null)
                            .colorIfAbsent(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── 从语言文件获取规则显示名 ──
    private String getRuleDisplayName(String ruleKey) {
        LanguageManager lang = plugin.getLanguageManager();
        String display = lang.getMessage("gui.rules.names." + ruleKey);
        if (display == null || display.startsWith("Missing:") || display.startsWith("<red>[Missing:")) {
            return ruleKey.replace('_', ' ');
        }
        return display;
    }

    // ── 从显示名反向查找英文键 ──
    private String getRuleKeyFromDisplay(String displayName) {
        LanguageManager lang = plugin.getLanguageManager();
        Set<String> availableRules = plugin.getRuleManager().getAvailableRules();
        for (String rule : availableRules) {
            String translated = lang.getMessage("gui.rules.names." + rule);
            if (translated != null && translated.equalsIgnoreCase(displayName)) {
                return rule;
            }
        }
        // 回退：尝试直接匹配英文键（防止语言文件缺失）
        for (String rule : availableRules) {
            if (rule.equalsIgnoreCase(displayName)) {
                return rule;
            }
        }
        return null;
    }

    private List<String> sortedRules() {
        List<String> list = new ArrayList<>(plugin.getRuleManager().getAvailableRules());
        Collections.sort(list);
        return list;
    }
}