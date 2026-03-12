package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameResult;
import lobby.minecraft_minigames.model.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.stream.Collectors;

public class ParkourRaceGame extends Minigame {

    private final Map<UUID, Integer> playerCheckpoints;
    private final List<UUID> finishedPlayers;
    private double deathY;
    private int totalCheckpoints;

    public ParkourRaceGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.playerCheckpoints = new HashMap<>();
        this.finishedPlayers = new ArrayList<>();
        this.deathY = arena.getCustomDouble("death-y", arena.getBounds().getMinY());
        this.totalCheckpoints = arena.getSpawnPoints().size();
    }

    @Override
    protected void onStart() {
        for (UUID uuid : players.keySet()) {
            playerCheckpoints.put(uuid, 0);
        }
        broadcastTitle("&a&lParkour Race!", "&7最速でゴールしろ！");
        broadcastSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }

    @Override
    protected void onEnd() {
        // Nothing extra
    }

    @Override
    protected void onTick() {
        // Check for players below death Y
        for (Player p : getOnlineAlivePlayers()) {
            if (p.getLocation().getY() < deathY) {
                // Teleport back to last checkpoint
                int checkpoint = playerCheckpoints.getOrDefault(p.getUniqueId(), 0);
                Location spawn = arena.getSpawnPoints().get(Math.min(checkpoint, arena.getSpawnPoints().size() - 1));
                p.teleport(spawn);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        // Not really eliminated, just finished or quit
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        if (finishedPlayers.contains(player.getUniqueId())) return;

        int currentCheckpoint = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
        List<Location> spawns = arena.getSpawnPoints();

        // Check if reached next checkpoint
        if (currentCheckpoint + 1 < spawns.size()) {
            Location nextCheckpoint = spawns.get(currentCheckpoint + 1);
            if (player.getLocation().distanceSquared(nextCheckpoint) < 4.0) { // Within 2 blocks
                playerCheckpoints.put(player.getUniqueId(), currentCheckpoint + 1);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                lobby.minecraft_minigames.util.MessageUtil.send(player,
                        "&aチェックポイント &f" + (currentCheckpoint + 2) + "/" + spawns.size());

                PlayerData data = players.get(player.getUniqueId());
                if (data != null) data.setScore(currentCheckpoint + 1);

                // Check if this is the last checkpoint (finish)
                if (currentCheckpoint + 1 >= spawns.size() - 1) {
                    finishedPlayers.add(player.getUniqueId());
                    int place = finishedPlayers.size();
                    broadcastMessage("&a" + player.getName() + " &fが &e" + place + "位 &fでゴール！");
                    broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);

                    // If all alive players finished or first player finished
                    if (finishedPlayers.size() >= getAliveCount()) {
                        end(determineResults());
                    }
                }
            }
        }

        // Fall check
        if (player.getLocation().getY() < deathY) {
            int checkpoint = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
            Location spawn = spawns.get(Math.min(checkpoint, spawns.size() - 1));
            player.teleport(spawn);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @Override
    protected void checkWinCondition() {
        // First to finish wins, or time runs out
        if (!finishedPlayers.isEmpty()) {
            end(determineResults());
        }
    }

    @Override
    public GameResult determineResults() {
        List<UUID> placements = new ArrayList<>(finishedPlayers);
        // Add players who didn't finish, sorted by checkpoint progress
        players.keySet().stream()
                .filter(uuid -> !finishedPlayers.contains(uuid))
                .sorted((a, b) -> Integer.compare(
                        playerCheckpoints.getOrDefault(b, 0),
                        playerCheckpoints.getOrDefault(a, 0)))
                .forEach(placements::add);

        Map<UUID, Integer> scores = new HashMap<>(playerCheckpoints);
        return new GameResult(getType(), placements, scores);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("");
        int cp = playerCheckpoints.getOrDefault(player.getUniqueId(), 0);
        lines.add("&7Checkpoint: &f" + (cp + 1) + "/" + totalCheckpoints);
        lines.add("");
        // Show progress of all players
        playerCheckpoints.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : "???";
                    boolean finished = finishedPlayers.contains(entry.getKey());
                    String status = finished ? "&a GOAL" : "&7 " + (entry.getValue() + 1) + "/" + totalCheckpoints;
                    lines.add("&f" + name + status);
                });
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.PARKOUR_RACE.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.PARKOUR_RACE.getDescription(); }
    @Override public Material getIcon() { return MinigameType.PARKOUR_RACE.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.PARKOUR_RACE; }
    @Override public int getGameDuration() { return 120; }
}
