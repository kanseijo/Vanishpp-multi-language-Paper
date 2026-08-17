package net.thecommandcraft.vanishpp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Admin dashboard showing all currently vanished players.
 *
 * <p>Left-click a skull → open that player's rules GUI.<br>
 * Right-click a skull → unvanish that player.
 */
public class AdminDashboardGUI implements Listener {

    private static final int SIZE = 54;

    private final Vanishpp plugin;
    private final Set<UUID> openViewers = new HashSet<>();

    public AdminDashboardGUI(Vanishpp plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer) {
        LanguageManager lang = plugin.getConfigManager().getLanguageManager();
        String title = lang.getMessage("gui.admin.title");
        Inventory inv = Bukkit.createInventory(null, SIZE, plugin.getMessageManager().parse(title, null));
        populateInventory(inv);

        // Info panel in last row
        inv.setItem(49, buildInfoItem());
        inv.setItem(53, buildCloseItem());

        openViewers.add(viewer.getUniqueId());
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!openViewers.contains(viewer.getUniqueId())) return;

        LanguageManager lang = plugin.getConfigManager().getLanguageManager();
        String title = lang.getMessage("gui.admin.title");
        // 检查标题是否匹配（忽略颜色代码）
        String viewTitle = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(event.getView().title());
        if (!viewTitle.contains(title.replaceAll("§[0-9a-fk-or]", ""))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Extract UUID from lore
        String uuidStr = getLoreValue(meta, "§8UUID: ");
        if (uuidStr == null) return;
        UUID targetUuid;
        try { targetUuid = UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { return; }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            viewer.closeInventory();
            return;
        }

        boolean rightClick = event.isRightClick();
        if (rightClick) {
            // Right-click → unvanish
            if (viewer.hasPermission("vanishpp.vanish.others")) {
                viewer.closeInventory();
                plugin.unvanishPlayer(target, viewer);
            }
        } else {
            // Left-click → open rules GUI
            if (viewer.hasPermission("vanishpp.rules.others")) {
                viewer.closeInventory();
                new RulesGUI(plugin).open(viewer, target);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViewers.remove(event.getPlayer().getUniqueId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void populateInventory(Inventory inv) {
        int slot = 0;
        for (UUID uuid : plugin.getRawVanishedPlayers()) {
            if (slot >= 45) break; // Leave last row for controls
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            inv.setItem(slot++, buildPlayerHead(p));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack buildPlayerHead(Player p) {
        LanguageManager lang = plugin.getConfigManager().getLanguageManager();

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        meta.setOwningPlayer(p);

        String reason = plugin.getVanishReason(p.getUniqueId());
        long elapsedMs = System.currentTimeMillis()
                - plugin.vanishStartTimes.getOrDefault(p.getUniqueId(), System.currentTimeMillis());
        long secs = elapsedMs / 1000;
        // 构建时间字符串（使用语言文件中的 "分" 和 "秒"）
        String minuteStr = lang.getMessage("time.minute");
        String secondStr = lang.getMessage("time.second");
        String elapsed = (secs / 60) + minuteStr + " " + (secs % 60) + secondStr;
        int level = plugin.getStorageProvider().getVanishLevel(p.getUniqueId());

        // 玩家名字（显示名）
        meta.displayName(Component.text(p.getName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // UUID 行保留不变（用于内部解析）
        lore.add(Component.text("§8UUID: " + p.getUniqueId()).decoration(TextDecoration.ITALIC, false));

        // 等级
        String levelMsg = lang.getMessage("gui.admin.level", "level", String.valueOf(level));
        lore.add(plugin.getMessageManager().parse(levelMsg, null).decoration(TextDecoration.ITALIC, false));

        // 已持续
        String elapsedMsg = lang.getMessage("gui.admin.elapsed", "time", elapsed);
        lore.add(plugin.getMessageManager().parse(elapsedMsg, null).decoration(TextDecoration.ITALIC, false));

        // 原因
        if (reason != null && !reason.isBlank()) {
            String reasonMsg = lang.getMessage("gui.admin.reason", "reason", reason);
            lore.add(plugin.getMessageManager().parse(reasonMsg, null).decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());

        // 左键提示
        String leftClickMsg = lang.getMessage("gui.admin.left_click");
        lore.add(plugin.getMessageManager().parse(leftClickMsg, null).decoration(TextDecoration.ITALIC, false));

        // 右键提示
        String rightClickMsg = lang.getMessage("gui.admin.right_click");
        lore.add(plugin.getMessageManager().parse(rightClickMsg, null).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildInfoItem() {
        LanguageManager lang = plugin.getConfigManager().getLanguageManager();
        int count = plugin.getRawVanishedPlayers().size();
        String msg = lang.getMessage("gui.admin.vanished_count", "count", String.valueOf(count));

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(msg, null)
                    .colorIfAbsent(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCloseItem() {
        LanguageManager lang = plugin.getConfigManager().getLanguageManager();
        String msg = lang.getMessage("gui.admin.close_button");

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().parse(msg, null)
                    .colorIfAbsent(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getLoreValue(ItemMeta meta, String prefix) {
        if (!meta.hasLore() || meta.lore() == null) return null;
        for (Component line : meta.lore()) {
            String s = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(line);
            if (s.startsWith(prefix)) return s.substring(prefix.length()).trim();
        }
        return null;
    }
}