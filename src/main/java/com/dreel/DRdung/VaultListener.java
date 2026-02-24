package com.dreel.DRdung;

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

            String closedName = plugin.color(plugin.getConfig().getString("messages.chest-closed", "&cЗакрытый Сундук"));
            String openedName = plugin.color(plugin.getConfig().getString("messages.chest-opened", "&aОткрытый Сундук"));
            String holoName = plugin.color(plugin.getConfig().getString("messages.hologram-need-key", "&eНужен ключ!"));

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

                        String unlockedMsg = plugin.color(plugin.getConfig().getString("messages.chest-unlocked", "&aХранилище открыто!"));
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
                String lockedMsg = plugin.color(plugin.getConfig().getString("messages.chest-locked", "&cЭтот сундук заперт! Нужна Реликвия Хранилища."));
                event.getPlayer().sendMessage(plugin.getPrefix() + lockedMsg);
            }
        }
    }
}