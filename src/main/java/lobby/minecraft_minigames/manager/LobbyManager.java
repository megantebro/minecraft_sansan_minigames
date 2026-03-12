package lobby.minecraft_minigames.manager;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.util.CountdownTimer;
import lobby.minecraft_minigames.util.ItemBuilder;
import lobby.minecraft_minigames.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class LobbyManager {

    private final Minecraft_minigames plugin;
    private final GameManager gameManager;
    private final ScoreboardManager scoreboardManager;

    private final Map<String, Set<UUID>> lobbyPlayers;
    private final Map<String, Set<UUID>> readyPlayers;
    private final Map<String, CountdownTimer> countdownTimers;
    private final Map<UUID, String> playerArenaMap;

    private Location mainLobbySpawn;

    public LobbyManager(Minecraft_minigames plugin, GameManager gameManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.scoreboardManager = scoreboardManager;
        this.lobbyPlayers = new HashMap<>();
        this.readyPlayers = new HashMap<>();
        this.countdownTimers = new HashMap<>();
        this.playerArenaMap = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        String worldName = plugin.getConfig().getString("lobby.spawn.world", "world");
        double x = plugin.getConfig().getDouble("lobby.spawn.x", 0);
        double y = plugin.getConfig().getDouble("lobby.spawn.y", 65);
        double z = plugin.getConfig().getDouble("lobby.spawn.z", 0);
        float yaw = (float) plugin.getConfig().getDouble("lobby.spawn.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("lobby.spawn.pitch", 0);
        var world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        mainLobbySpawn = new Location(world, x, y, z, yaw, pitch);
    }

    public boolean addToLobby(Player player, String arenaId) {
        Arena arena = gameManager.getArenaManager().getArena(arenaId);
        if (arena == null) return false;

        // Check if already in a lobby
        if (playerArenaMap.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, "&c既にロビーに参加しています。先に退出してください。");
            return false;
        }

        Set<UUID> players = lobbyPlayers.computeIfAbsent(arenaId, k -> new HashSet<>());
        if (players.size() >= 8) {
            MessageUtil.send(player, "&cこのゲームは満員です。");
            return false;
        }

        players.add(player.getUniqueId());
        playerArenaMap.put(player.getUniqueId(), arenaId);

        player.teleport(arena.getLobbySpawn());
        giveLobbyItems(player);

        broadcastToLobby(arenaId, "&a" + player.getName() + " &fがロビーに参加しました。 &7(" + players.size() + "/8)");

        updateLobbyScoreboards(arenaId);
        return true;
    }

    public void removeFromLobby(Player player) {
        String arenaId = playerArenaMap.remove(player.getUniqueId());
        if (arenaId == null) return;

        Set<UUID> players = lobbyPlayers.get(arenaId);
        if (players != null) {
            players.remove(player.getUniqueId());
        }

        Set<UUID> ready = readyPlayers.get(arenaId);
        if (ready != null) {
            ready.remove(player.getUniqueId());
        }

        player.getInventory().clear();
        scoreboardManager.removeScoreboard(player);

        broadcastToLobby(arenaId, "&c" + player.getName() + " &fがロビーから退出しました。");

        // Cancel countdown if not enough players
        if (players != null && players.size() < getMinPlayers()) {
            cancelCountdown(arenaId);
        }

        updateLobbyScoreboards(arenaId);
    }

    public void toggleReady(Player player) {
        String arenaId = playerArenaMap.get(player.getUniqueId());
        if (arenaId == null) return;

        Set<UUID> ready = readyPlayers.computeIfAbsent(arenaId, k -> new HashSet<>());

        if (ready.contains(player.getUniqueId())) {
            ready.remove(player.getUniqueId());
            MessageUtil.send(player, "&c準備を解除しました。");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

            cancelCountdown(arenaId);
        } else {
            ready.add(player.getUniqueId());
            MessageUtil.send(player, "&a準備完了！");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

            broadcastToLobby(arenaId, "&a" + player.getName() + " &fが準備完了！ &7(" + ready.size() + "/" + getLobbyPlayerCount(arenaId) + ")");

            if (allReady(arenaId) && hasMinimumPlayers(arenaId)) {
                startCountdown(arenaId);
            }
        }

        updateLobbyScoreboards(arenaId);
    }

    public boolean isReady(UUID playerId) {
        String arenaId = playerArenaMap.get(playerId);
        if (arenaId == null) return false;
        Set<UUID> ready = readyPlayers.get(arenaId);
        return ready != null && ready.contains(playerId);
    }

    private boolean allReady(String arenaId) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        Set<UUID> ready = readyPlayers.get(arenaId);
        if (players == null || ready == null) return false;
        return ready.containsAll(players);
    }

    private boolean hasMinimumPlayers(String arenaId) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        return players != null && players.size() >= getMinPlayers();
    }

    private int getMinPlayers() {
        return plugin.getConfig().getInt("settings.min-players", 2);
    }

    public void startCountdown(String arenaId) {
        if (countdownTimers.containsKey(arenaId)) return;

        int seconds = plugin.getConfig().getInt("settings.countdown-seconds", 15);

        CountdownTimer timer = new CountdownTimer(plugin, seconds)
                .onTick(remaining -> {
                    if (remaining <= 5 || remaining == 10 || remaining == 15) {
                        broadcastToLobby(arenaId, "&e" + remaining + "秒 &f後にゲームが開始します！");
                    }
                    if (remaining <= 5) {
                        for (UUID uuid : lobbyPlayers.getOrDefault(arenaId, Collections.emptySet())) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                MessageUtil.sendTitle(p, "&6" + remaining, "&fゲーム開始まで", 0, 25, 0);
                                float pitch = 0.5f + ((5 - remaining) * 0.3f);
                                MessageUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                            }
                        }
                    }
                })
                .onComplete(() -> {
                    countdownTimers.remove(arenaId);
                    launchGame(arenaId);
                });

        countdownTimers.put(arenaId, timer);
        timer.start();

        broadcastToLobby(arenaId, "&6カウントダウン開始！ &e" + seconds + "秒 &f後にゲームが開始します！");
    }

    public void cancelCountdown(String arenaId) {
        CountdownTimer timer = countdownTimers.remove(arenaId);
        if (timer != null && timer.isRunning()) {
            timer.cancel();
            broadcastToLobby(arenaId, "&cカウントダウンがキャンセルされました。");
        }
    }

    private void launchGame(String arenaId) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        if (players == null || players.isEmpty()) return;

        Arena arena = gameManager.getArenaManager().getArena(arenaId);
        if (arena == null) return;

        // Transfer players to game
        Set<UUID> gamePlayers = new HashSet<>(players);

        // Clear lobby state
        for (UUID uuid : gamePlayers) {
            playerArenaMap.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                scoreboardManager.removeScoreboard(p);
            }
        }
        lobbyPlayers.remove(arenaId);
        readyPlayers.remove(arenaId);

        // Start game
        gameManager.startGame(arenaId, gamePlayers);
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemBuilder(Material.NETHER_STAR)
                .name("&a準備完了 &7(右クリック)")
                .build());
        player.getInventory().setItem(4, new ItemBuilder(Material.COMPASS)
                .name("&6ゲーム選択 &7(右クリック)")
                .build());
        player.getInventory().setItem(8, new ItemBuilder(Material.RED_DYE)
                .name("&c退出 &7(右クリック)")
                .build());
    }

    private void broadcastToLobby(String arenaId, String message) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        if (players == null) return;
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                MessageUtil.send(p, message);
            }
        }
    }

    private void updateLobbyScoreboards(String arenaId) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        if (players == null) return;
        Arena arena = gameManager.getArenaManager().getArena(arenaId);
        if (arena == null) return;

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                scoreboardManager.showLobbyScoreboard(p,
                        arena.getGameType().getDisplayName(),
                        players.size(), 8,
                        isReady(uuid));
            }
        }
    }

    public int getLobbyPlayerCount(String arenaId) {
        Set<UUID> players = lobbyPlayers.get(arenaId);
        return players != null ? players.size() : 0;
    }

    public boolean isInLobby(UUID playerId) {
        return playerArenaMap.containsKey(playerId);
    }

    public String getPlayerLobbyArena(UUID playerId) {
        return playerArenaMap.get(playerId);
    }

    public Location getMainLobbySpawn() {
        return mainLobbySpawn;
    }

    public void setMainLobbySpawn(Location location) {
        this.mainLobbySpawn = location;
    }
}
