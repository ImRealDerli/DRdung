package com.dreel.DRdung;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final DRdung plugin;
    private final MainCommand cmd;

    public static class SpawnSettings {
        public int amount = 1;
        public int radius;
        public int minDistance;

        public SpawnSettings(DRdung plugin) {
            this.radius = plugin.getConfig().getInt("spawn-radius", 5000);
            this.minDistance = plugin.getConfig().getInt("min-distance", 300);
        }
    }

    public static final Map<UUID, SpawnSettings> spawnSettings = new HashMap<>();

    public MenuListener(DRdung plugin, MainCommand cmd) {
        this.plugin = plugin;
        this.cmd = cmd;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.DARK_AQUA + "Список Данжей")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().get(0).contains("ID: ")) {
                String idStr = ChatColor.stripColor(meta.getLore().get(0)).replace("ID: ", "");
                int id = Integer.parseInt(idStr);
                openDungeonMenu((Player) event.getWhoClicked(), id);
            }

        } else if (title.startsWith(ChatColor.DARK_AQUA + "Управление Данжем #")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            int id = Integer.parseInt(ChatColor.stripColor(title).replace("Управление Данжем #", ""));
            Player p = (Player) event.getWhoClicked();
            Material type = event.getCurrentItem().getType();

            if (type == Material.ENDER_PEARL) {
                Location loc = plugin.getDungeonManager().getDungeonLocation(id);
                if (loc != null) {
                    p.teleport(loc.add(0, 2, 0));
                    p.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Вы телепортировались к данжу.");
                }
                p.closeInventory();
            } else if (type == Material.ENDER_CHEST) {
                plugin.getDungeonManager().respawnChests(id);
                p.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Сундуки восстановлены!");
                openDungeonMenu(p, id);
            } else if (type == Material.TRIPWIRE_HOOK) {
                plugin.getDungeonManager().openChests(id);
                p.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Сундуки принудительно открыты!");
                openDungeonMenu(p, id);
            } else if (type == Material.TNT) {
                plugin.getDungeonManager().deleteDungeon(id);
                p.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Данж полностью удален!");
                p.closeInventory();
            } else if (type == Material.ARROW) {
                p.performCommand("drdung");
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Настройка спавна")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            Player p = (Player) event.getWhoClicked();
            SpawnSettings settings = spawnSettings.get(p.getUniqueId());
            if (settings == null) return;

            int slot = event.getSlot();
            if (slot == 10) {
                if (event.getClick().isShiftClick()) {
                    settings.amount += event.getClick().isLeftClick() ? 5 : -5;
                } else {
                    settings.amount += event.getClick().isLeftClick() ? 1 : -1;
                }
                if (settings.amount < 1) settings.amount = 1;
                cmd.openSpawnMenu(p);
            } else if (slot == 12) {
                if (event.getClick().isShiftClick()) {
                    settings.radius += event.getClick().isLeftClick() ? 500 : -500;
                } else {
                    settings.radius += event.getClick().isLeftClick() ? 100 : -100;
                }
                if (settings.radius < 100) settings.radius = 100;
                cmd.openSpawnMenu(p);
            } else if (slot == 14) {
                if (event.getClick().isShiftClick()) {
                    settings.minDistance += event.getClick().isLeftClick() ? 50 : -50;
                } else {
                    settings.minDistance += event.getClick().isLeftClick() ? 10 : -10;
                }
                if (settings.minDistance < 10) settings.minDistance = 10;
                cmd.openSpawnMenu(p);
            } else if (slot == 16) {
                p.closeInventory();
                cmd.startSpawnProcess(p, settings);
            }
        }
    }

    private void openDungeonMenu(Player p, int id) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_AQUA + "Управление Данжем #" + id);

        int[] stats = plugin.getDungeonManager().getDungeonStats(id);
        if (stats == null) {
            p.sendMessage(plugin.getPrefix() + ChatColor.RED + "Не удалось загрузить данные данжа. Возможно, он удален.");
            p.closeInventory();
            return;
        }

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 36; i++) {
            if (i < 9 || i > 26 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }

        ItemStack tp = new ItemStack(Material.ENDER_PEARL);
        ItemMeta m1 = tp.getItemMeta();
        m1.setDisplayName(ChatColor.AQUA + "▶ Телепортироваться");
        m1.setLore(Arrays.asList(ChatColor.GRAY + "Переместит вас к центру", ChatColor.GRAY + "этой постройки."));
        tp.setItemMeta(m1);

        ItemStack respawn = new ItemStack(Material.ENDER_CHEST);
        ItemMeta m2 = respawn.getItemMeta();
        m2.setDisplayName(ChatColor.YELLOW + "▶ Восстановить сундуки");
        m2.setLore(Arrays.asList(ChatColor.GRAY + "Превратит пустые сундуки", ChatColor.GRAY + "обратно в закрытые с лутом."));
        respawn.setItemMeta(m2);

        ItemStack open = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta m3 = open.getItemMeta();
        m3.setDisplayName(ChatColor.GREEN + "▶ Открыть все сундуки");
        m3.setLore(Arrays.asList(ChatColor.GRAY + "Удалит замки (голограммы)", ChatColor.GRAY + "со всех сундуков в данже."));
        open.setItemMeta(m3);

        ItemStack delete = new ItemStack(Material.TNT);
        ItemMeta m4 = delete.getItemMeta();
        m4.setDisplayName(ChatColor.RED + "▶ Удалить данж");
        m4.setLore(Arrays.asList(ChatColor.DARK_RED + "ВНИМАНИЕ!", ChatColor.GRAY + "Заменит все блоки данжа на воздух", ChatColor.GRAY + "и удалит приват WorldGuard."));
        delete.setItemMeta(m4);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta m5 = info.getItemMeta();
        m5.setDisplayName(ChatColor.GOLD + "✦ Статистика Данжа ✦");
        m5.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Всего сундуков: " + ChatColor.WHITE + stats[1],
                ChatColor.GRAY + "Осталось закрытых: " + ChatColor.RED + stats[0]
        ));
        info.setItemMeta(m5);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta m6 = back.getItemMeta();
        m6.setDisplayName(ChatColor.RED + "Назад");
        back.setItemMeta(m6);

        inv.setItem(10, tp);
        inv.setItem(12, respawn);
        inv.setItem(14, open);
        inv.setItem(16, delete);
        inv.setItem(22, info);
        inv.setItem(31, back);

        p.openInventory(inv);
    }
}