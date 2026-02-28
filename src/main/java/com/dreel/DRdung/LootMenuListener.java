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

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LootMenuListener implements Listener {

    private final DRdung plugin;

    public LootMenuListener(DRdung plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String rawTitle = ChatColor.stripColor(event.getView().getTitle());
        String targetTitle = ChatColor.stripColor(plugin.getLanguageManager().getString("menus.loot.title"));

        if (rawTitle.equals(targetTitle)) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();

            if (event.getClickedInventory() == null) return;

            if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
                int slot = event.getSlot();
                if (slot < plugin.getLootManager().getLootItems().size()) {
                    LootManager.LootItem li = plugin.getLootManager().getLootItems().get(slot);
                    ClickType click = event.getClick();
                    int maxStack = li.item.getMaxStackSize();

                    if (click == ClickType.LEFT) {
                        li.chance = Math.min(100.0, li.chance + 1.0);
                    } else if (click == ClickType.RIGHT) {
                        li.chance = Math.max(0.1, li.chance - 1.0);
                    } else if (click == ClickType.SHIFT_LEFT) {
                        li.chance = Math.min(100.0, li.chance + 5.0);
                    } else if (click == ClickType.SHIFT_RIGHT) {
                        li.chance = Math.max(0.1, li.chance - 5.0);
                    } else if (click == ClickType.NUMBER_KEY) {
                        int button = event.getHotbarButton();

                        if (button == 0) {
                            if (li.minAmount < li.maxAmount) li.minAmount++;
                        } else if (button == 1) {
                            if (li.minAmount > 1) li.minAmount--;
                        } else if (button == 2) {
                            if (li.maxAmount < maxStack) li.maxAmount++;
                        } else if (button == 3) {
                            if (li.maxAmount > li.minAmount && li.maxAmount > 1) li.maxAmount--;
                        } else if (button == 8) {
                            plugin.getLootManager().getLootItems().remove(slot);
                        }
                    }

                    plugin.getLootManager().saveLoot();
                    new MainCommand(plugin).openLootMenu(p, false);
                }
            } else if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir()) {
                    if (plugin.getLootManager().getLootItems().size() >= 54) {
                        p.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getString("messages.max-items"));
                        return;
                    }

                    int amt = clicked.getAmount();
                    plugin.getLootManager().getLootItems().add(new LootManager.LootItem(clicked.clone(), 10.0, amt, amt));
                    plugin.getLootManager().saveLoot();
                    new MainCommand(plugin).openLootMenu(p, false);
                }
            }
        }
    }
}
