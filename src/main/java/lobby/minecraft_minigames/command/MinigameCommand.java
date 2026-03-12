package lobby.minecraft_minigames.command;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.manager.LobbyManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MinigameCommand implements CommandExecutor, TabCompleter {

    private final Minecraft_minigames plugin;
    private final GameManager gameManager;
    private final LobbyManager lobbyManager;

    public MinigameCommand(Minecraft_minigames plugin, GameManager gameManager, LobbyManager lobbyManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.lobbyManager = lobbyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player);
            case "stop" -> handleStop(player, args);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, "&c使い方: /minigame join <ゲーム名>");
            MessageUtil.send(player, "&7利用可能なゲーム:");
            for (MinigameType type : MinigameType.values()) {
                MessageUtil.sendNoPrefix(player, "&8 - &6" + type.name() + " &7(" + type.getDisplayName() + ")");
            }
            return;
        }

        MinigameType type = MinigameType.fromString(args[1]);
        if (type == null) {
            MessageUtil.send(player, "&cゲーム '" + args[1] + "' が見つかりません。");
            return;
        }

        gameManager.joinGame(player, type);
    }

    private void handleLeave(Player player) {
        if (gameManager.isInGame(player.getUniqueId())) {
            gameManager.leaveGame(player);
            MessageUtil.send(player, "&7ゲームから退出しました。");
        } else if (lobbyManager.isInLobby(player.getUniqueId())) {
            lobbyManager.removeFromLobby(player);
            player.teleport(lobbyManager.getMainLobbySpawn());
            MessageUtil.send(player, "&7ロビーから退出しました。");
        } else {
            MessageUtil.send(player, "&cゲームにもロビーにも参加していません。");
        }
    }

    private void handleStart(Player player) {
        if (!player.hasPermission("minigames.admin")) {
            MessageUtil.send(player, "&c権限がありません。");
            return;
        }

        String arenaId = lobbyManager.getPlayerLobbyArena(player.getUniqueId());
        if (arenaId == null) {
            MessageUtil.send(player, "&cロビーに参加していません。");
            return;
        }

        lobbyManager.startCountdown(arenaId);
        MessageUtil.send(player, "&aゲームを強制開始しました。");
    }

    private void handleStop(Player player, String[] args) {
        if (!player.hasPermission("minigames.admin")) {
            MessageUtil.send(player, "&c権限がありません。");
            return;
        }

        if (args.length >= 2) {
            String arenaId = args[1];
            Minigame game = gameManager.getGame(arenaId);
            if (game != null) {
                gameManager.forceStop(arenaId);
                MessageUtil.send(player, "&aアリーナ " + arenaId + " のゲームを停止しました。");
            } else {
                MessageUtil.send(player, "&cアリーナ '" + arenaId + "' でアクティブなゲームはありません。");
            }
        } else if (gameManager.isInGame(player.getUniqueId())) {
            Minigame game = gameManager.getPlayerGame(player.getUniqueId());
            if (game != null) {
                gameManager.forceStop(game.getArena().getId());
                MessageUtil.send(player, "&aゲームを停止しました。");
            }
        } else {
            MessageUtil.send(player, "&c使い方: /minigame stop [arena_id]");
        }
    }

    private void handleList(Player player) {
        MessageUtil.send(player, "&6=== ミニゲーム一覧 ===");
        for (MinigameType type : MinigameType.values()) {
            int arenaCount = gameManager.getArenaManager().getArenasByType(type).size();
            long activeCount = gameManager.getActiveGames().stream()
                    .filter(g -> g.getType() == type)
                    .count();
            MessageUtil.sendNoPrefix(player, "&6" + type.getDisplayName() + " &7- " +
                    type.getDescription() + " &8[&fArenas: " + arenaCount + " &8| &fActive: " + activeCount + "&8]");
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&6=== MiniGames ヘルプ ===");
        MessageUtil.sendNoPrefix(player, "&e/minigame join <game> &7- ゲームに参加");
        MessageUtil.sendNoPrefix(player, "&e/minigame leave &7- ゲームから退出");
        MessageUtil.sendNoPrefix(player, "&e/minigame list &7- ゲーム一覧");
        MessageUtil.sendNoPrefix(player, "&e/minigame start &7- ゲーム強制開始 (管理者)");
        MessageUtil.sendNoPrefix(player, "&e/minigame stop [arena] &7- ゲーム停止 (管理者)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("join", "leave", "start", "stop", "list"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return filterStartsWith(
                    Arrays.stream(MinigameType.values()).map(t -> t.name().toLowerCase()).collect(Collectors.toList()),
                    args[1]
            );
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stop")) {
            return filterStartsWith(
                    gameManager.getActiveGames().stream().map(g -> g.getArena().getId()).collect(Collectors.toList()),
                    args[1]
            );
        }
        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
