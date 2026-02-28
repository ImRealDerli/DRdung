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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final DRdung plugin;
    private final Random random = new Random();

    public MainCommand(DRdung plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length == 0) {
            if (!(sender instanceof Player)) return true;
            openDungeonList((Player) sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sender.sendMessage(lang.getString("help.header"));
                sender.sendMessage(lang.getString("help.list"));
                sender.sendMessage(lang.getString("help.spawn"));
                sender.sendMessage(lang.getString("help.loot"));
                sender.sendMessage(lang.getString("help.key"));
                sender.sendMessage(lang.getString("help.reload"));
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.loadLocations();
                plugin.getLanguageManager().loadLanguage();
                plugin.getLootManager().loadLoot();
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.config-reloaded"));
                break;

            case "key":
                Player target = null;
                int keyAmount = 1;

                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getPrefix() + lang.getString("messages.player-not-found"));
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getPrefix() + lang.getString("messages.usage-key"));
                        return true;
                    }
                    target = (Player) sender;
                }

                if (args.length >= 3) {
                    try {
                        keyAmount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getPrefix() + lang.getString("messages.amount-number"));
                        return true;
                    }
                }

                Material keyMat = Material.getMaterial(plugin.getConfig().getString("key.material", "NETHER_STAR"));
                if (keyMat == null) keyMat = Material.NETHER_STAR;

                ItemStack key = new ItemStack(keyMat, keyAmount);
                ItemMeta meta = key.getItemMeta();
                meta.setDisplayName(lang.getString("key.name"));
                meta.setLore(lang.getStringList("key.lore"));

                NamespacedKey nbtKey = new NamespacedKey(plugin, "dungeon_key");
                meta.getPersistentDataContainer().set(nbtKey, PersistentDataType.BYTE, (byte) 1);

                key.setItemMeta(meta);
                target.getInventory().addItem(key);

                if (sender != target) {
                    sender.sendMessage(plugin.getPrefix() + lang.getString("messages.key-given",
                            "%amount%", String.valueOf(keyAmount),
                            "%player%", target.getName()));
                }
                target.sendMessage(plugin.getPrefix() + lang.getString("messages.key-received",
                        "%amount%", String.valueOf(keyAmount)));
                break;

            case "loot":
                if (!(sender instanceof Player)) return true;
                openLootMenu((Player) sender, true);
                break;

            case "spawn":
                if (!(sender instanceof Player)) return true;
                openSpawnMenu((Player) sender);
                break;

            default:
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.unknown-command"));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("spawn", "reload", "loot", "key", "help");
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("key")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("key")) {
                completions.addAll(Arrays.asList("1", "16", "32", "64"));
            }
        }
        return completions;
    }

    private void openDungeonList(Player player) {
        LanguageManager lang = plugin.getLanguageManager();
        plugin.loadLocations();
        List<String> locs = plugin.getLocationsConfig().getStringList("dungeons");

        if (locs.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + lang.getString("messages.no-dungeons"));
            return;
        }

        int size = 54;
        String title = lang.getString("menus.dungeon-list.title");
        Inventory inv = Bukkit.createInventory(null, size, title);

        ItemStack glass = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < size; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, glass);
        }

        int slot = 10;
        for (int i = 0; i < locs.size(); i++) {
            if (slot == 17 || slot == 26 || slot == 35 || slot == 44) slot += 2;
            if (slot >= 44) break;

            ItemStack item = new ItemStack(Material.FILLED_MAP);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(lang.getString("menus.dungeon-list.item-name", "%id%", String.valueOf(i)));
            String coords = locs.get(i).replace(" - ", " | ");

            List<String> lore = lang.getStringList("menus.dungeon-list.item-lore",
                    "%id%", String.valueOf(i),
                    "%loc%", coords);

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }
        player.openInventory(inv);
    }

    public void openLootMenu(Player p, boolean showMessage) {
        LanguageManager lang = plugin.getLanguageManager();
        Inventory inv = Bukkit.createInventory(null, 54, lang.getString("menus.loot.title"));
        List<LootManager.LootItem> items = plugin.getLootManager().getLootItems();

        for (int i = 0; i < items.size(); i++) {
            if (i >= 54) break;
            ItemStack display = items.get(i).item.clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = lang.getStringList("menus.loot.lore",
                    "%chance%", String.format("%.1f", items.get(i).chance).replace(",", "."),
                    "%min%", String.valueOf(items.get(i).minAmount),
                    "%max%", String.valueOf(items.get(i).maxAmount));

            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }
        p.openInventory(inv);
        if (showMessage) {
            p.sendMessage(plugin.getPrefix() + lang.getString("messages.click-to-add"));
        }
    }

    public void openSpawnMenu(Player p) {
        LanguageManager lang = plugin.getLanguageManager();
        MenuListener.SpawnSettings settings = MenuListener.spawnSettings.computeIfAbsent(p.getUniqueId(), k -> new MenuListener.SpawnSettings(plugin));
        Inventory inv = Bukkit.createInventory(null, 27, lang.getString("menus.spawn.title"));

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        ItemStack amount = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta am = amount.getItemMeta();
        am.setDisplayName(lang.getString("menus.spawn.amount-name", "%amount%", String.valueOf(settings.amount)));
        am.setLore(lang.getStringList("menus.spawn.amount-lore"));
        amount.setItemMeta(am);

        ItemStack rad = new ItemStack(Material.COMPASS);
        ItemMeta rm = rad.getItemMeta();
        rm.setDisplayName(lang.getString("menus.spawn.radius-name", "%radius%", String.valueOf(settings.radius)));
        rm.setLore(lang.getStringList("menus.spawn.radius-lore"));
        rad.setItemMeta(rm);

        ItemStack dist = new ItemStack(Material.OAK_FENCE);
        ItemMeta dm = dist.getItemMeta();
        dm.setDisplayName(lang.getString("menus.spawn.dist-name", "%dist%", String.valueOf(settings.minDistance)));
        dm.setLore(lang.getStringList("menus.spawn.dist-lore"));
        dist.setItemMeta(dm);

        ItemStack start = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sm = start.getItemMeta();
        sm.setDisplayName(lang.getString("menus.spawn.start-name"));
        start.setItemMeta(sm);

        inv.setItem(10, amount);
        inv.setItem(12, rad);
        inv.setItem(14, dist);
        inv.setItem(16, start);

        p.openInventory(inv);
    }

    public void startSpawnProcess(Player p, MenuListener.SpawnSettings settings) {
        LanguageManager lang = plugin.getLanguageManager();

        if (plugin.getLootManager().getLootItems().isEmpty()) {
            p.sendMessage(plugin.getPrefix() + lang.getString("messages.error-no-loot"));
            p.sendMessage(plugin.getPrefix() + lang.getString("messages.add-loot-hint"));
            return;
        }

        World world = p.getWorld();
        p.sendMessage(plugin.getPrefix() + lang.getString("messages.start-search", "%dist%", String.valueOf(settings.minDistance)));

        String initTitle = lang.getString("bossbar.search-title",
                "%found%", "0",
                "%amount%", String.valueOf(settings.amount),
                "%time%", "00:00");
        BossBar bossBar = Bukkit.createBossBar(initTitle, BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(p);

        searchNextLocation(world, settings.amount, new ArrayList<>(), 0, settings.radius, settings.minDistance, p, System.currentTimeMillis(), bossBar);
    }

    private int getGroundY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        while (y > 0) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (m.isSolid() && !m.name().contains("LEAVES") && !m.name().contains("LOG") && !m.name().contains("WOOD")) {
                return y;
            }
            y--;
        }
        return y;
    }

    private void searchNextLocation(World world, int amount, List<Location> foundLocations, int attempts, int radius, int minDistance, CommandSender sender, long startTime, BossBar bossBar) {
        LanguageManager lang = plugin.getLanguageManager();

        if (bossBar != null) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
            String title = lang.getString("bossbar.search-title",
                    "%found%", String.valueOf(foundLocations.size()),
                    "%amount%", String.valueOf(amount),
                    "%time%", timeStr);
            bossBar.setTitle(title);
            bossBar.setProgress(Math.min(1.0, (double) foundLocations.size() / amount));
        }

        if (foundLocations.size() >= amount || attempts >= amount * 80) {
            if (foundLocations.isEmpty()) {
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.cancel-search"));
                if (bossBar != null) bossBar.removeAll();
                return;
            }
            if (foundLocations.size() < amount) {
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.found-some",
                        "%found%", String.valueOf(foundLocations.size()),
                        "%amount%", String.valueOf(amount)));
            } else {
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.found-all",
                        "%amount%", String.valueOf(amount)));
            }
            pasteNextDungeon(world, foundLocations, 0, sender, startTime, bossBar);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            int y = getGroundY(world, x, z);

            if (!isTooClose(world, x, z, minDistance, foundLocations) && isValidLocation(world, x, y, z) && !isNearPlayerRegion(world, x, y, z)) {
                foundLocations.add(new Location(world, x, y + 1, z));
            }
            searchNextLocation(world, amount, foundLocations, attempts + 1, radius, minDistance, sender, startTime, bossBar);
        }, 2L);
    }

    private void pasteNextDungeon(World world, List<Location> locations, int index, CommandSender sender, long startTime, BossBar bossBar) {
        LanguageManager lang = plugin.getLanguageManager();

        if (bossBar != null) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            String timeStr = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60);
            String title = lang.getString("bossbar.gen-title",
                    "%index%", String.valueOf(index),
                    "%total%", String.valueOf(locations.size()),
                    "%time%", timeStr);
            bossBar.setTitle(title);
            bossBar.setProgress(Math.min(1.0, (double) index / locations.size()));
        }

        if (index >= locations.size()) {
            sender.sendMessage(plugin.getPrefix() + lang.getString("messages.gen-complete", "%amount%", String.valueOf(locations.size())));
            if (bossBar != null) bossBar.removeAll();
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location loc = locations.get(index);
            boolean success = plugin.getDungeonManager().pasteRandomDungeon(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            if (!success) {
                sender.sendMessage(plugin.getPrefix() + lang.getString("messages.error-schem"));
                if (bossBar != null) bossBar.removeAll();
                return;
            }
            pasteNextDungeon(world, locations, index + 1, sender, startTime, bossBar);
        }, 10L);
    }

    private boolean isTooClose(World world, int x, int z, int minDistance, List<Location> temporaryFound) {
        for (Location loc : temporaryFound) {
            if (Math.hypot(x - loc.getBlockX(), z - loc.getBlockZ()) < minDistance) return true;
        }
        for (String loc : plugin.getLocationsConfig().getStringList("dungeons")) {
            if (loc.startsWith(world.getName())) {
                try {
                    String[] parts = loc.split(" ");
                    int dx = Integer.parseInt(parts[2].substring(2));
                    int dz = Integer.parseInt(parts[4].substring(2));
                    if (Math.hypot(x - dx, z - dz) < minDistance) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private boolean isNearPlayerRegion(World world, int x, int y, int z) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions != null) {
                int pad = 50;
                BlockVector3 min = BlockVector3.at(x - pad, -64, z - pad);
                BlockVector3 max = BlockVector3.at(x + pad, 320, z + pad);
                ProtectedCuboidRegion checkRegion = new ProtectedCuboidRegion("temp_check", min, max);

                for (ProtectedRegion region : regions.getApplicableRegions(checkRegion)) {
                    if (!region.getId().equals("__global__")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean isValidLocation(World world, int x, int y, int z) {
        if (y < 20 || y > 250) return false;

        for (int dy = y + 1; dy <= y + 15; dy++) {
            Material m = world.getBlockAt(x, dy, z).getType();
            if (m.isSolid() && !m.name().contains("LEAVES") && !m.name().contains("LOG") && !m.name().contains("WOOD")) {
                return false;
            }
        }

        for (int dx = -15; dx <= 15; dx += 5) {
            for (int dz = -15; dz <= 15; dz += 5) {
                int highestY = world.getHighestBlockYAt(x + dx, z + dz);
                Material highestMat = world.getBlockAt(x + dx, highestY, z + dz).getType();
                if (highestMat == Material.WATER || highestMat == Material.LAVA || highestMat.name().contains("ICE")) return false;

                int groundY = getGroundY(world, x + dx, z + dz);
                if (Math.abs(groundY - y) > 5) return false;

                Material m = world.getBlockAt(x + dx, groundY, z + dz).getType();
                if (m == Material.WATER || m == Material.LAVA || m.name().contains("ICE")) return false;

                Material mAbove = world.getBlockAt(x + dx, groundY + 1, z + dz).getType();
                if (mAbove == Material.WATER || mAbove == Material.LAVA || mAbove.name().contains("ICE")) return false;
            }
        }
        return true;
    }
}
