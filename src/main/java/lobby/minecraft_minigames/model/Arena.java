package lobby.minecraft_minigames.model;

import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arena {

    private final String id;
    private final String displayName;
    private final MinigameType gameType;
    private final World world;
    private final Cuboid bounds;
    private final List<Location> spawnPoints;
    private final Location lobbySpawn;
    private final Map<String, Object> customData;
    private boolean available;

    public Arena(String id, String displayName, MinigameType gameType, World world,
                 Cuboid bounds, List<Location> spawnPoints, Location lobbySpawn) {
        this.id = id;
        this.displayName = displayName;
        this.gameType = gameType;
        this.world = world;
        this.bounds = bounds;
        this.spawnPoints = spawnPoints;
        this.lobbySpawn = lobbySpawn;
        this.customData = new HashMap<>();
        this.available = true;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public MinigameType getGameType() { return gameType; }
    public World getWorld() { return world; }
    public Cuboid getBounds() { return bounds; }
    public List<Location> getSpawnPoints() { return spawnPoints; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public void setCustomData(String key, Object value) {
        customData.put(key, value);
    }

    public Object getCustomData(String key) {
        return customData.get(key);
    }

    public double getCustomDouble(String key, double defaultValue) {
        Object val = customData.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultValue;
    }

    public int getCustomInt(String key, int defaultValue) {
        Object val = customData.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }
}
