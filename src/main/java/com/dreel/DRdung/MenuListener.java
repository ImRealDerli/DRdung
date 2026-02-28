package com.dreel.DRdung;

/*

 * Copyright 2026 [Влад / ImRealDerli / Drell]

 *

 * Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.

 * You may obtain a copy of the License at

 *

 *     http://www.apache.org/licenses/LICENSE-2.0

 *

 * Unless required by applicable law or agreed to in writing, software

 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 * See the License for the specific language governing permissions and

 * limitations under the License.

 */

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        String rawTitle = ChatColor.stripColor(event.getView().getTitle());
        String listTitle = ChatColor.stripColor(plugin.getLanguageManager().getString("menus.dungeon-list.title"));
        String manageTitleTemp = ChatColor.stripColor(plugin.getLanguageManager().getString("menus.dungeon-manage.title"));
        String spawnTitle = ChatColor.stripColor(plugin.getLanguageManager().getString("menus.spawn.title"));
        String managePrefix = manageTitleTemp.split("%id%")[0];

        if (rawTitle.equals(listTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasLore() && ChatColor.stripColor(meta.getLore().get(0)).contains("ID: ")) {
                String idStr = ChatColor.stripColor(meta.getLore().get(0)).replace("ID: ", "").trim();
                int id = Integer.parseInt(idStr);
                openDungeonMenu((Player) event.getWhoClicked(), id);
            }

        } else if (rawTitle.startsWith(managePrefix)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            String idStr = rawTitle.substring(managePrefix.length()).replaceAll("[^0-9]", "");
            if (idStr.isEmpty()) return;
            int id = Integer.parseInt(idStr);

            Player p = (Player) event.getWhoClicked();
            Material type = event.getCurrentItem().getType();

            if (type == Material.ENDER_PEARL) {
                Location loc = plugin.getDungeonManager().getDungeonLocation(id);
                if (loc != null) {
                    p.teleport(loc.add(0, 2, 0));
                    p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.tp-success"));
                }
                p.closeInventory();
            } else if (type == Material.ENDER_CHEST) {
                plugin.getDungeonManager().respawnChests(id);
                p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.chest-respawned"));
                openDungeonMenu(p, id);
            } else if (type == Material.TRIPWIRE_HOOK) {
                plugin.getDungeonManager().openChests(id);
                p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.chest-force-open"));
                openDungeonMenu(p, id);
            } else if (type == Material.TNT) {
                plugin.getDungeonManager().deleteDungeon(id);
                p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.dungeon-deleted"));
                p.closeInventory();
            } else if (type == Material.ARROW) {
                p.performCommand("drdung");
            }
        } else if (rawTitle.equals(spawnTitle)) {
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
        String title = plugin.getLanguageManager().getString("menus.dungeon-manage.title", "%id%", String.valueOf(id));
        Inventory inv = Bukkit.createInventory(null, 36, title);

        int[] stats = plugin.getDungeonManager().getDungeonStats(id);
        if (stats == null) {
            p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.dungeon-not-found"));
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
        m1.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.tp-name"));
        m1.setLore(plugin.getLanguageManager().getStringList("menus.dungeon-manage.tp-lore"));
        tp.setItemMeta(m1);

        ItemStack respawn = new ItemStack(Material.ENDER_CHEST);
        ItemMeta m2 = respawn.getItemMeta();
        m2.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.respawn-name"));
        m2.setLore(plugin.getLanguageManager().getStringList("menus.dungeon-manage.respawn-lore"));
        respawn.setItemMeta(m2);

        ItemStack open = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta m3 = open.getItemMeta();
        m3.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.open-name"));
        m3.setLore(plugin.getLanguageManager().getStringList("menus.dungeon-manage.open-lore"));
        open.setItemMeta(m3);

        ItemStack delete = new ItemStack(Material.TNT);
        ItemMeta m4 = delete.getItemMeta();
        m4.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.delete-name"));
        m4.setLore(plugin.getLanguageManager().getStringList("menus.dungeon-manage.delete-lore"));
        delete.setItemMeta(m4);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta m5 = info.getItemMeta();
        m5.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.info-name"));
        List<String> parsedInfoLore = plugin.getLanguageManager().getStringList("menus.dungeon-manage.info-lore",
                "%total%", String.valueOf(stats[1]),
                "%locked%", String.valueOf(stats[0])
        );
        m5.setLore(parsedInfoLore);
        info.setItemMeta(m5);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta m6 = back.getItemMeta();
        m6.setDisplayName(plugin.getLanguageManager().getString("menus.dungeon-manage.back-name"));
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
