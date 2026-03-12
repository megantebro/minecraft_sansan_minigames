package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameState;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MusicalMinecartsGame extends Minigame {

    private final List<Minecart> activeMinecarts;
    private boolean musicPlaying;
    private int musicTimer;
    private boolean checkPhase;
    private int checkTimer;
    private int roundNumber;
    private BukkitTask musicTask;
    private final Random random = new Random();

    public MusicalMinecartsGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.activeMinecarts = new ArrayList<>();
        this.musicPlaying = false;
        this.roundNumber = 0;
    }

    @Override
    protected void onStart() {
        broadcastTitle("&e&lMusical Minecarts!", "&7音楽が止まったらトロッコに乗れ！");
        broadcastSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
        startNewRound();
    }

    @Override
    protected void onEnd() {
        if (musicTask != null) musicTask.cancel();
        removeAllMinecarts();
    }

    @Override
    protected void onTick() {
        if (state != GameState.ACTIVE) return;

        if (musicPlaying) {
            musicTimer--;
            if (musicTimer <= 0) {
                stopMusic();
            }
        } else if (checkPhase) {
            checkTimer--;
            if (checkTimer <= 0) {
                evaluateRound();
            }
        }
    }

    private void startNewRound() {
        roundNumber++;
        removeAllMinecarts();

        // Spawn minecarts (alive players - 1)
        int cartCount = getAliveCount() - 1;
        List<Location> spawns = arena.getSpawnPoints();

        // Shuffle spawn positions
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < spawns.size(); i++) indices.add(i);
        Collections.shuffle(indices);

        for (int i = 0; i < Math.min(cartCount, spawns.size()); i++) {
            Location loc = spawns.get(indices.get(i)).clone();
            Minecart cart = arena.getWorld().spawn(loc, Minecart.class);
            cart.setMaxSpeed(0); // Don't let them move
            cart.setSlowWhenEmpty(true);
            activeMinecarts.add(cart);
        }

        // Start music
        musicPlaying = true;
        musicTimer = 5 + random.nextInt(11); // 5-15 seconds
        checkPhase = false;

        broadcastMessage("&eラウンド " + roundNumber + " - 音楽スタート！");

        // Play music notes periodically
        if (musicTask != null) musicTask.cancel();
        musicTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!musicPlaying || state != GameState.ACTIVE) return;
            for (Player p : getOnlineAlivePlayers()) {
                Sound[] notes = {Sound.BLOCK_NOTE_BLOCK_HARP, Sound.BLOCK_NOTE_BLOCK_BELL,
                        Sound.BLOCK_NOTE_BLOCK_CHIME, Sound.BLOCK_NOTE_BLOCK_FLUTE};
                p.playSound(p.getLocation(), notes[random.nextInt(notes.length)],
                        0.5f, 0.5f + random.nextFloat() * 1.5f);
            }
        }, 5L, 5L);
    }

    private void stopMusic() {
        musicPlaying = false;
        checkPhase = true;
        checkTimer = 4; // 4 seconds to find a minecart

        if (musicTask != null) musicTask.cancel();

        broadcastMessage("&c音楽が止まった！トロッコに乗れ！");
        broadcastTitle("&c&lSTOP!", "&eトロッコに乗れ！");
        broadcastSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
    }

    private void evaluateRound() {
        checkPhase = false;

        // Find players NOT in a minecart
        Set<UUID> inMinecart = new HashSet<>();
        for (Minecart cart : activeMinecarts) {
            for (Entity passenger : cart.getPassengers()) {
                if (passenger instanceof Player p && isAlive(p.getUniqueId())) {
                    inMinecart.add(p.getUniqueId());
                }
            }
        }

        List<Player> toEliminate = new ArrayList<>();
        for (Player p : getOnlineAlivePlayers()) {
            if (!inMinecart.contains(p.getUniqueId())) {
                toEliminate.add(p);
            }
        }

        // Eject all passengers
        for (Minecart cart : activeMinecarts) {
            cart.eject();
        }

        for (Player p : toEliminate) {
            broadcastMessage("&c" + p.getName() + " &7はトロッコに乗れなかった！");
            eliminatePlayer(p);
        }

        if (getAliveCount() > 1) {
            // Short delay before next round
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (state == GameState.ACTIVE && getAliveCount() > 1) {
                    startNewRound();
                }
            }, 60L);
        }
    }

    private void removeAllMinecarts() {
        for (Minecart cart : activeMinecarts) {
            cart.eject();
            cart.remove();
        }
        activeMinecarts.clear();
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!(event.getVehicle() instanceof Minecart)) return;
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;

        // Only allow entering during check phase
        if (musicPlaying || !checkPhase) {
            event.setCancelled(true);
            return;
        }

        // Check if minecart already has a passenger
        if (!event.getVehicle().getPassengers().isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;

        // Prevent exiting during check phase
        if (checkPhase) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (isPlaying(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        broadcastSound(Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("&7Round: &f" + roundNumber);
        lines.add("");
        if (musicPlaying) {
            lines.add("&a音楽再生中...");
        } else if (checkPhase) {
            lines.add("&cトロッコに乗れ！ &f" + checkTimer + "s");
        } else {
            lines.add("&7準備中...");
        }
        lines.add("");
        lines.add("&7Minecarts: &f" + activeMinecarts.size());
        lines.add("&7Alive: &f" + getAliveCount());
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.MUSICAL_MINECARTS.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.MUSICAL_MINECARTS.getDescription(); }
    @Override public Material getIcon() { return MinigameType.MUSICAL_MINECARTS.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.MUSICAL_MINECARTS; }
    @Override public int getGameDuration() { return 300; }
}
