package lobby.minecraft_minigames.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Cuboid {

    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public Cuboid(Location corner1, Location corner2) {
        this.world = corner1.getWorld();
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() != world) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public Location getRandomLocation() {
        int x = minX + (int) (Math.random() * (maxX - minX + 1));
        int z = minZ + (int) (Math.random() * (maxZ - minZ + 1));
        return new Location(world, x + 0.5, maxY, z + 0.5);
    }

    public World getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
