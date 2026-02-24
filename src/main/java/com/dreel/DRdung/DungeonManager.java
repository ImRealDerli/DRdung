package com.dreel.DRdung;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonManager {

    private final DRdung plugin;
    private final File schematicsFolder;

    public DungeonManager(DRdung plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public String getRandomSchematic() {
        if (!plugin.getConfig().contains("schematics")) {
            return plugin.getConfig().getString("schematic-name", "dungeon.schem");
        }
        double totalChance = 0;
        for (String key : plugin.getConfig().getConfigurationSection("schematics").getKeys(false)) {
            totalChance += plugin.getConfig().getDouble("schematics." + key + ".chance", 100.0);
        }
        double rand = new Random().nextDouble() * totalChance;
        double current = 0;
        for (String key : plugin.getConfig().getConfigurationSection("schematics").getKeys(false)) {
            double chance = plugin.getConfig().getDouble("schematics." + key + ".chance", 100.0);
            current += chance;
            if (rand <= current) {
                return plugin.getConfig().getString("schematics." + key + ".file", "dungeon.schem");
            }
        }
        return "dungeon.schem";
    }

    public boolean pasteRandomDungeon(World world, int x, int y, int z) {
        String schemName = getRandomSchematic();
        File schematicFile = new File(schematicsFolder, schemName);

        if (!schematicFile.exists()) {
            Bukkit.getLogger().warning("[DRdung] Схематика " + schemName + " не найдена!");
            return false;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) return false;

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboard = reader.read();

            int yOffset = clipboard.getOrigin().getBlockY() - clipboard.getMinimumPoint().getBlockY();
            int pasteY = y + yOffset - 1;

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(x, pasteY, z))
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(operation);
            }

            BlockVector3 origin = clipboard.getOrigin();
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();

            int offsetX = x - origin.getX();
            int offsetY = pasteY - origin.getY();
            int offsetZ = z - origin.getZ();

            int minX = min.getX() + offsetX;
            int minY = min.getY() + offsetY;
            int minZ = min.getZ() + offsetZ;
            int maxX = max.getX() + offsetX;
            int maxY = max.getY() + offsetY;
            int maxZ = max.getZ() + offsetZ;

            buildFoundation(world, minX, minY, minZ, maxX, maxY, maxZ);

            processLoot(world, minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1, true);

            List<String> locs = plugin.getLocationsConfig().getStringList("dungeons");
            locs.add(world.getName() + " - X:" + x + " Y:" + y + " Z:" + z);
            plugin.getLocationsConfig().set("dungeons", locs);

            String regionData = world.getName() + ";" + (minX - 1) + ";" + (maxX + 1) + ";" + (minY - 1) + ";" + (maxY + 1) + ";" + (minZ - 1) + ";" + (maxZ + 1);
            List<String> regions = plugin.getLocationsConfig().getStringList("regions");
            regions.add(regionData);
            plugin.getLocationsConfig().set("regions", regions);

            plugin.saveLocationsConfig();

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager wgRegions = container.get(BukkitAdapter.adapt(world));
            if (wgRegions != null) {
                String regionName = "dungeon_" + x + "_" + y + "_" + z;
                ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, BlockVector3.at(minX - 2, minY - 2, minZ - 2), BlockVector3.at(maxX + 2, maxY + 2, maxZ + 2));
                region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
                region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
                region.setFlag(Flags.TNT, StateFlag.State.DENY);
                region.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
                region.setFlag(Flags.OTHER_EXPLOSION, StateFlag.State.DENY);
                wgRegions.addRegion(region);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void buildFoundation(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int lowestSolidY = -1;
                for (int y = minY; y <= maxY; y++) {
                    Material m = world.getBlockAt(x, y, z).getType();
                    if (m.isSolid() && !m.name().contains("LEAVES") && !m.name().contains("LOG") && !m.name().contains("WOOD") && m != Material.BARRIER) {
                        lowestSolidY = y;
                        break;
                    }
                }

                if (lowestSolidY == -1) continue;

                if (lowestSolidY > minY + 1) continue;

                int startY = lowestSolidY - 1;
                int searchY = startY;
                Material fillMat = Material.DIRT;
                Material deepMat = Material.STONE;

                while (searchY > 0) {
                    Material m = world.getBlockAt(x, searchY, z).getType();
                    if (m.isSolid() && !m.name().contains("LEAVES") && !m.name().contains("LOG") && !m.name().contains("WOOD")) {
                        if (m == Material.SAND) { fillMat = Material.SAND; deepMat = Material.SANDSTONE; }
                        else if (m == Material.RED_SAND) { fillMat = Material.RED_SAND; deepMat = Material.RED_SANDSTONE; }
                        else if (m.name().contains("TERRACOTTA")) { fillMat = m; deepMat = m; }
                        else if (m == Material.SNOW_BLOCK) { fillMat = Material.SNOW_BLOCK; deepMat = Material.STONE; }
                        break;
                    }
                    searchY--;
                }

                int currentY = startY;
                while (currentY > searchY) {
                    world.getBlockAt(x, currentY, z).setType((startY - currentY <= 3) ? fillMat : deepMat);
                    currentY--;
                }
            }
        }
    }

    private void processLoot(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean convertShulkers) {
        String closedName = plugin.color(plugin.getConfig().getString("messages.chest-closed", "&cЗакрытый Сундук"));
        String holoName = plugin.color(plugin.getConfig().getString("messages.hologram-need-key", "&eНужен ключ!"));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    boolean isChest = block.getType() == Material.CHEST;

                    if (convertShulkers && block.getType().name().contains("SHULKER_BOX")) {
                        block.setType(Material.CHEST);
                        isChest = true;
                    }

                    if (isChest) {
                        Chest chest = (Chest) block.getState();
                        chest.setCustomName(closedName);
                        chest.update(true);

                        Chest filledChest = (Chest) block.getState();
                        filledChest.getInventory().clear();
                        plugin.getLootManager().fillInventory(filledChest.getInventory());

                        for (Entity ent : world.getNearbyEntities(block.getLocation().add(0.5, 1.2, 0.5), 0.5, 0.5, 0.5)) {
                            if (ent instanceof ArmorStand) ent.remove();
                        }

                        ArmorStand stand = (ArmorStand) world.spawnEntity(block.getLocation().add(0.5, 1.2, 0.5), EntityType.ARMOR_STAND);
                        stand.setVisible(false);
                        stand.setGravity(false);
                        stand.setMarker(true);
                        stand.setCustomNameVisible(true);
                        stand.setCustomName(holoName);
                    }
                }
            }
        }
    }

    private void loadChunks(World w, int minX, int minZ, int maxX, int maxZ) {
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                w.getChunkAt(cx, cz);
            }
        }
    }

    public int[] getDungeonStats(int id) {
        List<String> dRegs = plugin.getLocationsConfig().getStringList("regions");
        if (id >= dRegs.size()) return null;

        String[] parts = dRegs.get(id).split(";");
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;

        int minX = Integer.parseInt(parts[1]); int maxX = Integer.parseInt(parts[2]);
        int minY = Integer.parseInt(parts[3]); int maxY = Integer.parseInt(parts[4]);
        int minZ = Integer.parseInt(parts[5]); int maxZ = Integer.parseInt(parts[6]);

        loadChunks(w, minX, minZ, maxX, maxZ);

        String closedName = plugin.color(plugin.getConfig().getString("messages.chest-closed", "&cЗакрытый Сундук"));

        int total = 0, locked = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == Material.CHEST) {
                        total++;
                        Chest c = (Chest) b.getState();
                        if (c.getCustomName() != null && c.getCustomName().equals(closedName)) {
                            locked++;
                        }
                    }
                }
            }
        }
        return new int[]{locked, total};
    }

    public Location getDungeonLocation(int id) {
        List<String> dLocs = plugin.getLocationsConfig().getStringList("dungeons");
        if (id >= dLocs.size()) return null;
        String[] parts = dLocs.get(id).split(" ");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[2].substring(2)), Integer.parseInt(parts[3].substring(2)), Integer.parseInt(parts[4].substring(2)));
    }

    public void deleteDungeon(int id) {
        List<String> dLocs = plugin.getLocationsConfig().getStringList("dungeons");
        List<String> dRegs = plugin.getLocationsConfig().getStringList("regions");
        if (id >= dLocs.size() || id >= dRegs.size()) return;

        String[] locParts = dLocs.get(id).split(" ");
        String[] regParts = dRegs.get(id).split(";");
        World w = Bukkit.getWorld(regParts[0]);

        if (w != null) {
            int minX = Integer.parseInt(regParts[1]); int maxX = Integer.parseInt(regParts[2]);
            int minY = Integer.parseInt(regParts[3]); int maxY = Integer.parseInt(regParts[4]);
            int minZ = Integer.parseInt(regParts[5]); int maxZ = Integer.parseInt(regParts[6]);

            loadChunks(w, minX, minZ, maxX, maxZ);

            String holoName = plugin.color(plugin.getConfig().getString("messages.hologram-need-key", "&eНужен ключ!"));

            for (Entity ent : w.getEntities()) {
                if (ent instanceof ArmorStand && ent.getCustomName() != null && ent.getCustomName().equals(holoName)) {
                    Location l = ent.getLocation();
                    if (l.getBlockX() >= minX && l.getBlockX() <= maxX && l.getBlockY() >= minY && l.getBlockY() <= maxY && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ) {
                        ent.remove();
                    }
                }
            }

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager wgRegions = container.get(BukkitAdapter.adapt(w));
            if (wgRegions != null) {
                wgRegions.removeRegion("dungeon_" + locParts[2].substring(2) + "_" + locParts[3].substring(2) + "_" + locParts[4].substring(2));
            }

            List<int[]> chunks = new ArrayList<>();
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
                for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                    chunks.add(new int[]{cx, cz});
                }
            }
            regenerateChunksSlowly(w, chunks, 0);
        }

        dLocs.remove(id);
        dRegs.remove(id);
        plugin.getLocationsConfig().set("dungeons", dLocs);
        plugin.getLocationsConfig().set("regions", dRegs);
        plugin.saveLocationsConfig();
    }

    private void regenerateChunksSlowly(World w, List<int[]> chunks, int index) {
        if (index >= chunks.size()) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int[] chunk = chunks.get(index);
            w.regenerateChunk(chunk[0], chunk[1]);
            regenerateChunksSlowly(w, chunks, index + 1);
        }, 5L);
    }

    public void respawnChests(int id) {
        List<String> dRegs = plugin.getLocationsConfig().getStringList("regions");
        if (id >= dRegs.size()) return;
        String[] parts = dRegs.get(id).split(";");
        World w = Bukkit.getWorld(parts[0]);
        if (w != null) {
            loadChunks(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[5]), Integer.parseInt(parts[2]), Integer.parseInt(parts[6]));
            processLoot(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[3]), Integer.parseInt(parts[5]), Integer.parseInt(parts[2]), Integer.parseInt(parts[4]), Integer.parseInt(parts[6]), false);
        }
    }

    public void openChests(int id) {
        List<String> dRegs = plugin.getLocationsConfig().getStringList("regions");
        if (id >= dRegs.size()) return;
        String[] parts = dRegs.get(id).split(";");
        World w = Bukkit.getWorld(parts[0]);
        if (w != null) {
            int minX = Integer.parseInt(parts[1]); int maxX = Integer.parseInt(parts[2]);
            int minY = Integer.parseInt(parts[3]); int maxY = Integer.parseInt(parts[4]);
            int minZ = Integer.parseInt(parts[5]); int maxZ = Integer.parseInt(parts[6]);

            loadChunks(w, minX, minZ, maxX, maxZ);

            String openedName = plugin.color(plugin.getConfig().getString("messages.chest-opened", "&aОткрытый Сундук"));
            String holoName = plugin.color(plugin.getConfig().getString("messages.hologram-need-key", "&eНужен ключ!"));

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block b = w.getBlockAt(x, y, z);
                        if (b.getType() == Material.CHEST) {
                            Chest c = (Chest) b.getState();
                            c.setCustomName(openedName);
                            c.update();
                            for (Entity ent : w.getNearbyEntities(b.getLocation().add(0.5, 1.2, 0.5), 0.5, 0.5, 0.5)) {
                                if (ent instanceof ArmorStand && ent.getCustomName() != null && ent.getCustomName().equals(holoName)) {
                                    ent.remove();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}