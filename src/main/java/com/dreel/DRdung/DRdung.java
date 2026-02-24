package com.dreel.DRdung;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DRdung extends JavaPlugin {

    private static DRdung instance;
    private DungeonManager dungeonManager;
    private LootManager lootManager;
    private File locationsFile;
    private FileConfiguration locationsConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        loadLocations();

        this.lootManager = new LootManager(this);
        this.dungeonManager = new DungeonManager(this);

        MainCommand cmd = new MainCommand(this);
        getCommand("drdung").setExecutor(cmd);
        getCommand("drdung").setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new VaultListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this, cmd), this);
        Bukkit.getPluginManager().registerEvents(new LootMenuListener(this), this);

        getLogger().info("Плагин DRdang успешно запущен.");
    }

    public void loadLocations() {
        locationsFile = new File(getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            try { locationsFile.createNewFile(); } catch (Exception ignored) {}
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
    }

    public static DRdung getInstance() {
        return instance;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public FileConfiguration getLocationsConfig() {
        return locationsConfig;
    }

    public void saveLocationsConfig() {
        try {
            locationsConfig.save(locationsFile);
        } catch (Exception ignored) {}
    }

    public String color(String msg) {
        if (msg == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    public String getPrefix() {
        return color(getConfig().getString("prefix", "&#FFAA00[DRdung] &r"));
    }
}