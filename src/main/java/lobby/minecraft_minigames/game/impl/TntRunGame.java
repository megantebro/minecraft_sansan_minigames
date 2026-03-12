package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TntRunGame extends Minigame {

    private final Set<Location> scheduledBlocks;
    private final List<BukkitTask> removalTasks;
    private double deathY;

    private static final Set<Material> FLOOR_MATERIALS = Set.of(
            Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.TNT, Material.SANDSTONE, Material.RED_SANDSTONE
    );

    public TntRunGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.scheduledBlocks = new HashSet<>();
        this.removalTasks = new ArrayList<>();
        this.deathY = arena.getCustomDouble("death-y", arena.getBounds().getMinY());
    }

    @Override
    protected void onStart() {
        broadcastTitle("&c&lTNT Run!", "&7踏んだ床が消える！走れ！");
        broadcastSound(Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
    }

    @Override
    protected void onEnd() {
        for (BukkitTask task : removalTasks) {
            if (!task.isCancelled()) task.cancel();
        }
        removalTasks.clear();
        scheduledBlocks.clear();
    }

    @Override
    protected void onTick() {
        for (Player p : getOnlineAlivePlayers()) {
            if (p.getLocation().getY() < deathY) {
                eliminatePlayer(p);
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        broadcastSound(Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        if (state != lobby.minecraft_minigames.model.GameState.ACTIVE) return;

        // Only trigger on block change
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        Location below = player.getLocation().subtract(0, 1, 0);
        scheduleBlockRemoval(below);

        // Also check adjacent blocks for smoother experience
        scheduleBlockRemoval(below.clone().add(1, 0, 0));
        scheduleBlockRemoval(below.clone().add(-1, 0, 0));
        scheduleBlockRemoval(below.clone().add(0, 0, 1));
        scheduleBlockRemoval(below.clone().add(0, 0, -1));
    }

    private void scheduleBlockRemoval(Location loc) {
        Block block = loc.getBlock();
        if (!isFloorMaterial(block.getType())) return;
        if (!arena.getBounds().contains(loc)) return;

        Location blockLoc = block.getLocation();
        if (scheduledBlocks.contains(blockLoc)) return;
        scheduledBlocks.add(blockLoc);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (block.getType().isAir()) return;
            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5),
                    10, 0.3, 0.1, 0.3, block.getBlockData());
            block.setType(Material.AIR, false);

            // Also remove block below (multi-layer)
            Block below = block.getRelative(0, -1, 0);
            if (isFloorMaterial(below.getType()) && arena.getBounds().contains(below.getLocation())) {
                Location belowLoc = below.getLocation();
                if (!scheduledBlocks.contains(belowLoc)) {
                    scheduledBlocks.add(belowLoc);
                    BukkitTask belowTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!below.getType().isAir()) {
                            below.getWorld().spawnParticle(Particle.BLOCK, below.getLocation().add(0.5, 0.5, 0.5),
                                    10, 0.3, 0.1, 0.3, below.getBlockData());
                            below.setType(Material.AIR, false);
                        }
                    }, 5L);
                    removalTasks.add(belowTask);
                }
            }
        }, 8L);
        removalTasks.add(task);
    }

    private boolean isFloorMaterial(Material mat) {
        return FLOOR_MATERIALS.contains(mat);
    }

    @Override public String getDisplayName() { return MinigameType.TNT_RUN.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.TNT_RUN.getDescription(); }
    @Override public Material getIcon() { return MinigameType.TNT_RUN.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.TNT_RUN; }
    @Override public int getGameDuration() { return 180; }
}
