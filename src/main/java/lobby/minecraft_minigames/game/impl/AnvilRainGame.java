package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameState;
import lobby.minecraft_minigames.util.Cuboid;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AnvilRainGame extends Minigame {

    private int anvilsPerWave;
    private int spawnInterval;
    private BukkitTask anvilSpawnTask;
    private final Set<UUID> activeAnvils;
    private double spawnY;

    public AnvilRainGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.anvilsPerWave = 3;
        this.spawnInterval = 40;
        this.activeAnvils = new HashSet<>();
        this.spawnY = arena.getBounds().getMaxY() + 15;
    }

    @Override
    protected void onStart() {
        broadcastTitle("&4&lAnvil Rain!", "&7降ってくる金床を避けろ！");
        broadcastSound(Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);

        startAnvilSpawning();
    }

    @Override
    protected void onEnd() {
        if (anvilSpawnTask != null) {
            anvilSpawnTask.cancel();
        }
        // Clean up remaining anvils
        World world = arena.getWorld();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof FallingBlock fb && activeAnvils.contains(fb.getUniqueId())) {
                entity.remove();
            }
        }
        activeAnvils.clear();
    }

    @Override
    protected void onTick() {
        // Increase difficulty every 15 seconds
        if (timeRemaining % 15 == 0 && timeRemaining < getGameDuration()) {
            anvilsPerWave = Math.min(anvilsPerWave + 2, 20);
            spawnInterval = Math.max(spawnInterval - 5, 5);

            // Restart spawning with new interval
            if (anvilSpawnTask != null) anvilSpawnTask.cancel();
            startAnvilSpawning();

            broadcastMessage("&4難易度上昇！ &7金床がさらに多く降ってきます！");
            broadcastSound(Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
        }

        // Check proximity for players near falling anvils
        for (Player p : getOnlineAlivePlayers()) {
            checkAnvilProximity(p);
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3);
        broadcastSound(Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
    }

    private void startAnvilSpawning() {
        anvilSpawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.ACTIVE) return;
            Cuboid bounds = arena.getBounds();
            Random random = new Random();

            for (int i = 0; i < anvilsPerWave; i++) {
                double x = bounds.getMinX() + random.nextInt(bounds.getMaxX() - bounds.getMinX() + 1) + 0.5;
                double z = bounds.getMinZ() + random.nextInt(bounds.getMaxZ() - bounds.getMinZ() + 1) + 0.5;
                Location spawnLoc = new Location(arena.getWorld(), x, spawnY, z);

                FallingBlock anvil = arena.getWorld().spawnFallingBlock(
                        spawnLoc, Material.ANVIL.createBlockData());
                anvil.setDropItem(false);
                anvil.setHurtEntities(true);
                anvil.setMaxDamage(100);
                activeAnvils.add(anvil.getUniqueId());

                // Spawn warning particle below
                Location warningLoc = new Location(arena.getWorld(), x, bounds.getMaxY() + 1, z);
                arena.getWorld().spawnParticle(Particle.DUST,
                        warningLoc, 5, 0.3, 0, 0.3, new Particle.DustOptions(Color.RED, 2.0f));
            }
        }, 20L, spawnInterval);
    }

    private void checkAnvilProximity(Player player) {
        Location pLoc = player.getLocation();
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof FallingBlock fb && activeAnvils.contains(fb.getUniqueId())) {
                if (fb.getLocation().distanceSquared(pLoc) < 1.5) {
                    eliminatePlayer(player);
                    entity.remove();
                    activeAnvils.remove(fb.getUniqueId());
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fb && activeAnvils.contains(fb.getUniqueId())) {
            event.setCancelled(true);
            activeAnvils.remove(fb.getUniqueId());
            event.getEntity().remove();
            // Small impact effect
            Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            event.getBlock().getWorld().spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.1, 0.2);
            event.getBlock().getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            event.setCancelled(true);
            eliminatePlayer(player);
        } else {
            event.setCancelled(true);
        }
    }

    @Override public String getDisplayName() { return MinigameType.ANVIL_RAIN.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.ANVIL_RAIN.getDescription(); }
    @Override public Material getIcon() { return MinigameType.ANVIL_RAIN.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.ANVIL_RAIN; }
    @Override public int getGameDuration() { return 180; }
}
