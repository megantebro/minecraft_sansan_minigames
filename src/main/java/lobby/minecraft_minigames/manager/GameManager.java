package lobby.minecraft_minigames.manager;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameRegistry;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameResult;
import lobby.minecraft_minigames.model.GameState;
import lobby.minecraft_minigames.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final Minecraft_minigames plugin;
    private final ArenaManager arenaManager;
    private final ScoreboardManager scoreboardManager;
    private final MinigameRegistry registry;

    private LobbyManager lobbyManager;

    private final Map<String, Minigame> activeGames;    // arenaId -> Minigame
    private final Map<UUID, String> playerGameMap;       // playerId -> arenaId

    public GameManager(Minecraft_minigames plugin, ArenaManager arenaManager, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.scoreboardManager = scoreboardManager;
        this.registry = new MinigameRegistry();
        this.activeGames = new HashMap<>();
        this.playerGameMap = new HashMap<>();
    }

    public void setLobbyManager(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    public void initialize() {
        // Capture snapshots for destructible arenas
        for (Arena arena : arenaManager.getAllArenas()) {
            MinigameType type = arena.getGameType();
            if (type == MinigameType.TNT_RUN || type == MinigameType.SPLEEF || type == MinigameType.COLOR_SWAP) {
                arenaManager.captureSnapshot(arena.getId());
            }
        }
    }

    public void shutdown() {
        for (Map.Entry<String, Minigame> entry : new HashMap<>(activeGames).entrySet()) {
            forceStop(entry.getKey());
        }
    }

    public boolean joinGame(Player player, MinigameType type) {
        if (isInGame(player.getUniqueId())) {
            MessageUtil.send(player, "&c既にゲームに参加しています。");
            return false;
        }
        if (lobbyManager != null && lobbyManager.isInLobby(player.getUniqueId())) {
            MessageUtil.send(player, "&c既にロビーに参加しています。先に退出してください。");
            return false;
        }

        Arena arena = arenaManager.findAvailableArena(type);
        if (arena == null) {
            MessageUtil.send(player, "&cこのゲームのアリーナが見つかりません。");
            return false;
        }

        if (lobbyManager != null) {
            return lobbyManager.addToLobby(player, arena.getId());
        }
        return false;
    }

    public void startGame(String arenaId, Set<UUID> playerIds) {
        Arena arena = arenaManager.getArena(arenaId);
        if (arena == null) return;

        arena.setAvailable(false);

        // Capture snapshot for destructible games
        MinigameType type = arena.getGameType();
        if (type == MinigameType.TNT_RUN || type == MinigameType.SPLEEF || type == MinigameType.COLOR_SWAP) {
            arenaManager.captureSnapshot(arenaId);
        }

        Minigame game = registry.createGame(type, plugin, this, arena);

        for (UUID uuid : playerIds) {
            game.addPlayer(uuid);
            playerGameMap.put(uuid, arenaId);
        }

        activeGames.put(arenaId, game);
        game.start();
    }

    public void endGame(String arenaId, GameResult result) {
        Minigame game = activeGames.remove(arenaId);
        if (game == null) return;

        // Remove all players from game map
        for (UUID uuid : game.getPlayers().keySet()) {
            playerGameMap.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                scoreboardManager.removeScoreboard(p);
                if (lobbyManager != null) {
                    p.teleport(lobbyManager.getMainLobbySpawn());
                }
            }
        }

        // Restore arena
        MinigameType type = game.getType();
        if (type == MinigameType.TNT_RUN || type == MinigameType.SPLEEF || type == MinigameType.COLOR_SWAP) {
            arenaManager.restoreSnapshot(arenaId);
        }

        Arena arena = arenaManager.getArena(arenaId);
        if (arena != null) {
            arena.setAvailable(true);
        }
    }

    public void forceStop(String arenaId) {
        Minigame game = activeGames.get(arenaId);
        if (game != null && game.getState() == GameState.ACTIVE) {
            game.end(game.determineResults());
        }
    }

    public void leaveGame(Player player) {
        String arenaId = playerGameMap.get(player.getUniqueId());
        if (arenaId != null) {
            Minigame game = activeGames.get(arenaId);
            if (game != null) {
                game.removePlayer(player.getUniqueId());
                playerGameMap.remove(player.getUniqueId());
                scoreboardManager.removeScoreboard(player);
                if (lobbyManager != null) {
                    player.teleport(lobbyManager.getMainLobbySpawn());
                }
                player.setGameMode(org.bukkit.GameMode.ADVENTURE);
                player.getInventory().clear();
            }
        }
    }

    public boolean isInGame(UUID playerId) {
        return playerGameMap.containsKey(playerId);
    }

    public Minigame getPlayerGame(UUID playerId) {
        String arenaId = playerGameMap.get(playerId);
        if (arenaId == null) return null;
        return activeGames.get(arenaId);
    }

    public Minigame getGame(String arenaId) {
        return activeGames.get(arenaId);
    }

    public Collection<Minigame> getActiveGames() {
        return activeGames.values();
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public MinigameRegistry getRegistry() { return registry; }
}
