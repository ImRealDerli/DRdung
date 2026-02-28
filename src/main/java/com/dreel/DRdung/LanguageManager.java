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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private final DRdung plugin;
    private FileConfiguration langConfig;

    public LanguageManager(DRdung plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        String lang = plugin.getConfig().getString("language", "ru");
        File langFile = new File(plugin.getDataFolder() + "/language", lang + ".yml");

        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            try {
                InputStream in = plugin.getResource("language/" + lang + ".yml");
                if (in != null) {
                    Files.copy(in, langFile.toPath());
                } else {
                    langFile.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getString(String path) {
        return plugin.color(langConfig.getString(path, path));
    }

    public String getString(String path, String... placeholders) {
        String text = langConfig.getString(path, path);
        if (text != null && placeholders != null) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    text = text.replace(placeholders[i], placeholders[i + 1]);
                }
            }
        }
        return plugin.color(text);
    }

    public List<String> getStringList(String path) {
        List<String> list = langConfig.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String s : list) {
            colored.add(plugin.color(s));
        }
        return colored;
    }

    public List<String> getStringList(String path, String... placeholders) {
        List<String> list = langConfig.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String s : list) {
            if (placeholders != null) {
                for (int i = 0; i < placeholders.length; i += 2) {
                    if (i + 1 < placeholders.length) {
                        s = s.replace(placeholders[i], placeholders[i + 1]);
                    }
                }
            }
            colored.add(plugin.color(s));
        }
        return colored;
    }
}
