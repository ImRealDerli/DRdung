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

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class VaultListener implements Listener {

    private final DRdung plugin;

    public VaultListener(DRdung plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVaultOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();

            String closedName = plugin.getLanguageManager().getString("messages.chest-closed");
            String openedName = plugin.getLanguageManager().getString("messages.chest-opened");
            String holoName = plugin.getLanguageManager().getString("messages.need-key");

            if (chest.getCustomName() != null && chest.getCustomName().equals(closedName)) {
                ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

                Material keyMat = Material.getMaterial(plugin.getConfig().getString("key.material", "NETHER_STAR"));
                if (keyMat == null) keyMat = Material.NETHER_STAR;

                if (item.getType() == keyMat && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    NamespacedKey nbtKey = new NamespacedKey(plugin, "dungeon_key");

                    if (meta.getPersistentDataContainer().has(nbtKey, PersistentDataType.BYTE)) {
                        item.setAmount(item.getAmount() - 1);
                        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

                        String unlockedMsg = plugin.getLanguageManager().getString("messages.chest-unlocked");
                        event.getPlayer().sendMessage(plugin.getPrefix() + unlockedMsg);

                        chest.setCustomName(openedName);
                        chest.update(true);

                        for (Entity ent : block.getLocation().getWorld().getNearbyEntities(block.getLocation().add(0.5, 1.2, 0.5), 1, 2, 1)) {
                            if (ent instanceof ArmorStand && ent.getCustomName() != null && ent.getCustomName().equals(holoName)) {
                                ent.remove();
                            }
                        }

                        event.setCancelled(true);
                        event.getPlayer().openInventory(chest.getInventory());
                        return;
                    }
                }

                event.setCancelled(true);
                String lockedMsg = plugin.getLanguageManager().getString("messages.chest-locked");
                event.getPlayer().sendMessage(plugin.getPrefix() + lockedMsg);
            }
        }
    }
}
