package lobby.minecraft_minigames.manager;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.util.Cuboid;
import lobby.minecraft_minigames.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final Minecraft_minigames plugin;
    private final Map<String, Arena> arenas;
    private final Map<String, Map<Long, BlockData>> arenaSnapshots;
    private File arenaFile;
    private FileConfiguration arenaConfig;

    public ArenaManager(Minecraft_minigames plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenaSnapshots = new HashMap<>();
    }

    public void loadArenas() {
        arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);

        ConfigurationSection arenasSection = arenaConfig.getConfigurationSection("arenas");
        if (arenasSection == null) return;

        for (String id : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(id);
            if (section == null) continue;

            String displayName = section.getString("display-name", id);
            String typeStr = section.getString("game-type", "TNT_RUN");
            MinigameType gameType = MinigameType.fromString(typeStr);
            if (gameType == null) {
                plugin.getLogger().warning("Unknown game type: " + typeStr + " for arena: " + id);
                continue;
            }

            String worldName = section.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found: " + worldName + " for arena: " + id);
                continue;
            }

            ConfigurationSection boundsSection = section.getConfigurationSection("bounds");
            Location corner1 = LocationUtil.fromConfig(boundsSection.getConfigurationSection("corner1"), world);
            Location corner2 = LocationUtil.fromConfig(boundsSection.getConfigurationSection("corner2"), world);
            if (corner1 == null || corner2 == null) {
                plugin.getLogger().warning("Invalid bounds for arena: " + id);
                continue;
            }
            Cuboid bounds = new Cuboid(corner1, corner2);

            Location lobbySpawn = LocationUtil.fromConfig(section.getConfigurationSection("lobby-spawn"), world);
            if (lobbySpawn == null) {
                lobbySpawn = new Location(world, 0, 65, 0);
            }

            List<Location> spawnPoints = new ArrayList<>();
            List<?> spawnList = section.getList("spawn-points");
            if (spawnList != null) {
                ConfigurationSection spawnSection = section.getConfigurationSection("spawn-points");
                if (spawnSection != null) {
                    for (String key : spawnSection.getKeys(false)) {
                        Location loc = LocationUtil.fromConfig(spawnSection.getConfigurationSection(key), world);
                        if (loc != null) spawnPoints.add(loc);
                    }
                }
            }

            if (spawnPoints.isEmpty()) {
                spawnPoints.add(lobbySpawn);
            }

            Arena arena = new Arena(id, displayName, gameType, world, bounds, spawnPoints, lobbySpawn);

            // Load custom data
            ConfigurationSection customSection = section.getConfigurationSection("custom-points");
            if (customSection != null) {
                for (String key : customSection.getKeys(false)) {
                    arena.setCustomData(key, customSection.get(key));
                }
            }

            arenas.put(id, arena);
            plugin.getLogger().info("Loaded arena: " + id + " (" + gameType.getDisplayName() + ")");
        }
    }

    public void captureSnapshot(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;

        Map<Long, BlockData> snapshot = new HashMap<>();
        for (Block block : arena.getBounds().getBlocks()) {
            if (!block.getType().isAir()) {
                long key = encodeBlockPos(block.getX(), block.getY(), block.getZ());
                snapshot.put(key, block.getBlockData().clone());
            }
        }
        arenaSnapshots.put(arenaId, snapshot);
        plugin.getLogger().info("Captured snapshot for arena: " + arenaId + " (" + snapshot.size() + " blocks)");
    }

    public void restoreSnapshot(String arenaId) {
        Arena arena = arenas.get(arenaId);
        Map<Long, BlockData> snapshot = arenaSnapshots.get(arenaId);
        if (arena == null || snapshot == null) return;

        // First set all blocks in bounds to air
        for (Block block : arena.getBounds().getBlocks()) {
            if (!block.getType().isAir()) {
                long key = encodeBlockPos(block.getX(), block.getY(), block.getZ());
                if (!snapshot.containsKey(key)) {
                    block.setType(org.bukkit.Material.AIR, false);
                }
            }
        }

        // Then restore snapshot blocks
        World world = arena.getWorld();
        for (Map.Entry<Long, BlockData> entry : snapshot.entrySet()) {
            int[] pos = decodeBlockPos(entry.getKey());
            Block block = world.getBlockAt(pos[0], pos[1], pos[2]);
            block.setBlockData(entry.getValue(), false);
        }

        plugin.getLogger().info("Restored snapshot for arena: " + arenaId);
    }

    private long encodeBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private int[] decodeBlockPos(long encoded) {
        int x = (int) (encoded >> 38);
        int z = (int) ((encoded >> 12) & 0x3FFFFFF);
        int y = (int) (encoded & 0xFFF);
        // Sign extension
        if (x >= 0x2000000) x -= 0x4000000;
        if (z >= 0x2000000) z -= 0x4000000;
        return new int[]{x, y, z};
    }

    public Arena getArena(String id) {
        return arenas.get(id);
    }

    public Arena findAvailableArena(MinigameType type) {
        for (Arena arena : arenas.values()) {
            if (arena.getGameType() == type && arena.isAvailable()) {
                return arena;
            }
        }
        return null;
    }

    public List<Arena> getArenasByType(MinigameType type) {
        List<Arena> result = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.getGameType() == type) {
                result.add(arena);
            }
        }
        return result;
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }
}
