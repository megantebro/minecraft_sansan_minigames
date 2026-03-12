package lobby.minecraft_minigames.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class LocationUtil {

    public static Location fromConfig(ConfigurationSection section, World defaultWorld) {
        if (section == null) return null;
        String worldName = section.getString("world", defaultWorld != null ? defaultWorld.getName() : "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = defaultWorld;
        if (world == null) return null;

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void toConfig(ConfigurationSection section, Location loc) {
        if (section == null || loc == null) return;
        if (loc.getWorld() != null) {
            section.set("world", loc.getWorld().getName());
        }
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    public static String serialize(Location loc) {
        if (loc == null) return "null";
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                loc.getWorld() != null ? loc.getWorld().getName() : "world",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public static Location deserialize(String str) {
        if (str == null || str.equals("null")) return null;
        String[] parts = str.split(",");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
