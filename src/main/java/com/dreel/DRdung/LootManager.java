package com.dreel.DRdung;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LootManager {

    private final DRdung plugin;
    private final File file;
    private final FileConfiguration config;
    private final Random random = new Random();

    public static class LootItem {
        public ItemStack item;
        public double chance;
        public int minAmount;
        public int maxAmount;

        public LootItem(ItemStack i, double c, int min, int max) {
            this.item = i;
            this.chance = c;
            this.minAmount = min;
            this.maxAmount = max;
        }
    }

    private final List<LootItem> lootItems = new ArrayList<>();

    public LootManager(DRdung plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "loot.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadLoot();
    }

    public void loadLoot() {
        lootItems.clear();
        if (config.contains("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                ItemStack item = config.getItemStack("items." + key + ".item");
                double chance = config.getDouble("items." + key + ".chance", 10.0);
                int min = config.getInt("items." + key + ".min", 1);
                int max = config.getInt("items." + key + ".max", item != null ? item.getAmount() : 1);
                if (item != null) {
                    lootItems.add(new LootItem(item, chance, min, max));
                }
            }
        }
    }

    public void saveLoot() {
        config.set("items", null);
        for (int i = 0; i < lootItems.size(); i++) {
            config.set("items." + i + ".item", lootItems.get(i).item);
            config.set("items." + i + ".chance", lootItems.get(i).chance);
            config.set("items." + i + ".min", lootItems.get(i).minAmount);
            config.set("items." + i + ".max", lootItems.get(i).maxAmount);
        }
        try { config.save(file); } catch (Exception ignored) {}
    }

    public List<LootItem> getLootItems() { return lootItems; }

    public void fillInventory(Inventory inv) {
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) emptySlots.add(i);
        }
        Collections.shuffle(emptySlots);
        int added = 0;

        for (LootItem li : lootItems) {
            if (added >= emptySlots.size()) break;

            if (random.nextDouble() * 100.0 <= li.chance) {
                ItemStack toAdd = li.item.clone();
                int amt = li.minAmount;
                if (li.maxAmount > li.minAmount) {
                    amt = li.minAmount + random.nextInt(li.maxAmount - li.minAmount + 1);
                }
                toAdd.setAmount(amt);

                inv.setItem(emptySlots.get(added), toAdd);
                added++;
            }
        }
    }
}