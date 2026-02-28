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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DRdung extends JavaPlugin {

    private static DRdung instance;
    private LanguageManager languageManager;
    private DungeonManager dungeonManager;
    private LootManager lootManager;
    private File locationsFile;
    private FileConfiguration locationsConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.languageManager = new LanguageManager(this);
        loadLocations();

        this.lootManager = new LootManager(this);
        this.dungeonManager = new DungeonManager(this);

        MainCommand cmd = new MainCommand(this);
        getCommand("drdung").setExecutor(cmd);
        getCommand("drdung").setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new VaultListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this, cmd), this);
        Bukkit.getPluginManager().registerEvents(new LootMenuListener(this), this);

        getLogger().info("DRdung successfully started.");
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

    public LanguageManager getLanguageManager() {
        return languageManager;
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

        Pattern gPattern = Pattern.compile("<gradient:([0-9A-Fa-f&#]+):([0-9A-Fa-f&#]+)>(.*?)</gradient>");
        Matcher gMatcher = gPattern.matcher(msg);
        StringBuffer gBuffer = new StringBuffer();
        while (gMatcher.find()) {
            String startHex = gMatcher.group(1).replace("#", "").replace("&", "");
            String endHex = gMatcher.group(2).replace("#", "").replace("&", "");
            String text = gMatcher.group(3);
            gMatcher.appendReplacement(gBuffer, Matcher.quoteReplacement(applyGradient(text, startHex, endHex)));
        }
        msg = gMatcher.appendTail(gBuffer).toString();

        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    private String applyGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return "";
        int startR = Integer.parseInt(startHex.substring(0, 2), 16);
        int startG = Integer.parseInt(startHex.substring(2, 4), 16);
        int startB = Integer.parseInt(startHex.substring(4, 6), 16);
        int endR = Integer.parseInt(endHex.substring(0, 2), 16);
        int endG = Integer.parseInt(endHex.substring(2, 4), 16);
        int endB = Integer.parseInt(endHex.substring(4, 6), 16);

        StringBuilder builder = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float ratio = len <= 1 ? 0 : (float) i / (len - 1);
            int r = (int) (startR + ratio * (endR - startR));
            int g = (int) (startG + ratio * (endG - startG));
            int b = (int) (startB + ratio * (endB - startB));
            builder.append(net.md_5.bungee.api.ChatColor.of(String.format("#%02x%02x%02x", r, g, b)));
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }

    public String getPrefix() {
        return languageManager.getString("prefix");
    }
}
