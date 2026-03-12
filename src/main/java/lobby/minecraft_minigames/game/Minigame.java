package lobby.minecraft_minigames.game;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameResult;
import lobby.minecraft_minigames.model.GameState;
import lobby.minecraft_minigames.model.PlayerData;
import lobby.minecraft_minigames.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Minigame implements Listener {

    protected final Minecraft_minigames plugin;
    protected final GameManager gameManager;
    protected final Arena arena;
    protected final Map<UUID, PlayerData> players;
    protected GameState state;
    protected BukkitTask gameTask;
    protected int timeRemaining;

    public Minigame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.arena = arena;
        this.players = new LinkedHashMap<>();
        this.state = GameState.WAITING;
    }

    // ========== Abstract Methods ==========

    protected abstract void onStart();
    protected abstract void onEnd();
    protected abstract void onTick();
    protected abstract void onPlayerEliminate(Player player);

    public abstract String getDisplayName();
    public abstract String getDescription();
    public abstract org.bukkit.Material getIcon();
    public abstract MinigameType getType();

    // ========== Overridable Defaults ==========

    public int getGameDuration() { return 120; }
    public int getMinPlayers() { return 2; }
    public int getMaxPlayers() { return 8; }
    public boolean allowSpectating() { return true; }

    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("");
        lines.add("&7Players: &f" + getAliveCount() + "/" + players.size());
        return lines;
    }

    // ========== Game Lifecycle (final) ==========

    public final void start() {
        this.state = GameState.ACTIVE;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        List<Location> spawns = arena.getSpawnPoints();
        int i = 0;
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(spawns.get(i % spawns.size()));
                preparePlayer(p);
                i++;
            }
        }

        this.timeRemaining = getGameDuration();

        broadcastMessage("&a" + getDisplayName() + " &fが開始しました！");
        broadcastMessage("&7" + getDescription());

        onStart();

        gameTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.ACTIVE) return;
            timeRemaining--;
            onTick();
            gameManager.getScoreboardManager().updateGameScoreboard(this);
            if (timeRemaining <= 0) {
                end(determineResults());
            }
        }, 20L, 20L);
    }

    public final void end(GameResult result) {
        if (state == GameState.ENDING || state == GameState.RESETTING) return;
        this.state = GameState.ENDING;
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        onEnd();
        HandlerList.unregisterAll(this);
        announceResults(result);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            this.state = GameState.RESETTING;
            teleportAllToLobby();
            restorePlayerStates();
            gameManager.endGame(arena.getId(), result);
        }, 100L);
    }

    // ========== Player Management ==========

    public final void addPlayer(UUID playerId) {
        players.put(playerId, new PlayerData(playerId));
    }

    public final void removePlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && state == GameState.ACTIVE) {
            PlayerData data = players.get(playerId);
            if (data != null && data.isAlive()) {
                onPlayerEliminate(player);
            }
        }
        players.remove(playerId);
        if (state == GameState.ACTIVE) {
            checkWinCondition();
        }
    }

    protected final void eliminatePlayer(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null || !data.isAlive()) return;
        data.setAlive(false);
        data.setPlacement(getAliveCount() + 1);

        onPlayerEliminate(player);

        broadcastMessage("&c" + player.getName() + " &7が脱落しました！ &8(&f" + getAliveCount() + "&7人残り&8)");

        if (allowSpectating()) {
            data.setSpectating(true);
            player.setGameMode(GameMode.SPECTATOR);
            MessageUtil.send(player, "&7観戦モードになりました。");
        }

        checkWinCondition();
    }

    protected void checkWinCondition() {
        List<UUID> alive = getAlivePlayers();
        if (alive.size() <= 1 && state == GameState.ACTIVE) {
            end(determineResults());
        }
    }

    // ========== Utility Methods ==========

    protected List<UUID> getAlivePlayers() {
        return players.entrySet().stream()
                .filter(e -> e.getValue().isAlive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    protected int getAliveCount() {
        return (int) players.values().stream().filter(PlayerData::isAlive).count();
    }

    protected List<Player> getOnlineAlivePlayers() {
        return getAlivePlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Player> getAllOnlinePlayers() {
        return players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void broadcastMessage(String message) {
        Component comp = MessageUtil.colorize("&8[&6" + getDisplayName() + "&8] &r" + message);
        for (Player p : getAllOnlinePlayers()) {
            p.sendMessage(comp);
        }
    }

    protected void broadcastTitle(String title, String subtitle) {
        MessageUtil.sendTitle(getAllOnlinePlayers(), title, subtitle, 5, 40, 10);
    }

    protected void broadcastSound(Sound sound, float volume, float pitch) {
        MessageUtil.playSound(getAllOnlinePlayers(), sound, volume, pitch);
    }

    private void preparePlayer(Player p) {
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        p.setExp(0);
        p.setLevel(0);
        p.setFireTicks(0);
    }

    private void teleportAllToLobby() {
        Location lobbySpawn = gameManager.getLobbyManager().getMainLobbySpawn();
        for (Player p : getAllOnlinePlayers()) {
            p.teleport(lobbySpawn);
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(20);
            p.setFoodLevel(20);
        }
    }

    private void restorePlayerStates() {
        for (Player p : getAllOnlinePlayers()) {
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
            p.setFireTicks(0);
            p.setGlowing(false);
        }
    }

    public GameResult determineResults() {
        List<UUID> placements = new ArrayList<>();
        Map<UUID, Integer> scores = new HashMap<>();

        // Alive players first (winners)
        List<UUID> alive = getAlivePlayers();
        placements.addAll(alive);

        // Then eliminated players in reverse elimination order
        players.entrySet().stream()
                .filter(e -> !e.getValue().isAlive())
                .sorted((a, b) -> Integer.compare(a.getValue().getPlacement(), b.getValue().getPlacement()))
                .forEach(e -> placements.add(e.getKey()));

        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            scores.put(entry.getKey(), entry.getValue().getScore());
        }

        return new GameResult(getType(), placements, scores);
    }

    private void announceResults(GameResult result) {
        broadcastMessage("&6===== ゲーム終了 =====");
        List<UUID> placements = result.getPlacements();
        String[] medals = {"&e1st", "&f2nd", "&63rd"};
        for (int i = 0; i < Math.min(3, placements.size()); i++) {
            Player p = Bukkit.getPlayer(placements.get(i));
            String name = p != null ? p.getName() : "???";
            broadcastMessage(medals[i] + " &7- &f" + name);
        }
        if (!placements.isEmpty()) {
            Player winner = Bukkit.getPlayer(placements.get(0));
            if (winner != null) {
                broadcastTitle("&6" + winner.getName(), "&e優勝！");
                broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }

    protected String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    // ========== Getters ==========

    public Arena getArena() { return arena; }
    public GameState getState() { return state; }
    public Map<UUID, PlayerData> getPlayers() { return players; }
    public int getTimeRemaining() { return timeRemaining; }
    public int getPlayerCount() { return players.size(); }

    public boolean isPlaying(UUID uuid) {
        return players.containsKey(uuid);
    }

    public boolean isAlive(UUID uuid) {
        PlayerData data = players.get(uuid);
        return data != null && data.isAlive();
    }
}
