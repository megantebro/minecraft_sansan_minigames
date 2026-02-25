package lobby.minecraft_minigames.manager;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final Minecraft_minigames plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String[] INVISIBLE_ENTRIES = {
            "\u00a70", "\u00a71", "\u00a72", "\u00a73",
            "\u00a74", "\u00a75", "\u00a76", "\u00a77",
            "\u00a78", "\u00a79", "\u00a7a", "\u00a7b",
            "\u00a7c", "\u00a7d", "\u00a7e", "\u00a7f"
    };

    public ScoreboardManager(Minecraft_minigames plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
    }

    public void showLobbyScoreboard(Player player, String gameName, int playerCount, int maxPlayers, boolean ready) {
        Scoreboard board = getOrCreateScoreboard(player);
        Objective obj = board.getObjective("display");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("display", Criteria.DUMMY, LEGACY.deserialize("&6&lMiniGames"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        lines.add("&7&m              ");
        lines.add("");
        lines.add("&fGame: &a" + gameName);
        lines.add("&fPlayers: &a" + playerCount + "&7/&a" + maxPlayers);
        lines.add("");
        lines.add("&fStatus: " + (ready ? "&aReady" : "&cNot Ready"));
        lines.add("");
        lines.add("&7&m               ");

        setLines(board, obj, lines);
        player.setScoreboard(board);
    }

    public void updateGameScoreboard(Minigame game) {
        for (Player player : game.getAllOnlinePlayers()) {
            Scoreboard board = getOrCreateScoreboard(player);
            Objective obj = board.getObjective("display");
            if (obj != null) obj.unregister();
            obj = board.registerNewObjective("display", Criteria.DUMMY,
                    LEGACY.deserialize("&6&l" + game.getDisplayName()));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            List<String> lines = game.getScoreboardLines(player);
            setLines(board, obj, lines);
            player.setScoreboard(board);
        }
    }

    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private Scoreboard getOrCreateScoreboard(Player player) {
        return playerScoreboards.computeIfAbsent(player.getUniqueId(),
                k -> Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void setLines(Scoreboard board, Objective obj, List<String> lines) {
        // Clear old teams
        for (int i = 0; i < INVISIBLE_ENTRIES.length; i++) {
            Team team = board.getTeam("line_" + i);
            if (team != null) team.unregister();
        }

        int score = lines.size();
        for (int i = 0; i < Math.min(lines.size(), INVISIBLE_ENTRIES.length); i++) {
            Team team = board.registerNewTeam("line_" + i);
            team.addEntry(INVISIBLE_ENTRIES[i]);
            team.prefix(LEGACY.deserialize(lines.get(i)));
            obj.getScore(INVISIBLE_ENTRIES[i]).setScore(score - i);
        }
    }
}
