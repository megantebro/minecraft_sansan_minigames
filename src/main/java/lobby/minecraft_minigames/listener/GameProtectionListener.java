package lobby.minecraft_minigames.listener;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.manager.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class GameProtectionListener implements Listener {

    private final Minecraft_minigames plugin;
    private final GameManager gameManager;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "/minigame", "/mg", "/party", "/msg", "/tell", "/w", "/r"
    );

    public GameProtectionListener(Minecraft_minigames plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        // Only allow block breaking if player is in a game (game handlers decide if allowed)
        if (!gameManager.isInGame(player.getUniqueId())) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!gameManager.isInGame(player.getUniqueId())) {
            if (!player.isOp()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        if (!gameManager.isInGame(player.getUniqueId())) return;

        String command = event.getMessage().toLowerCase().split(" ")[0];
        for (String allowed : ALLOWED_COMMANDS) {
            if (command.equals(allowed)) return;
        }

        event.setCancelled(true);
        player.sendMessage(lobby.minecraft_minigames.util.MessageUtil.colorize(
                "&8[&6MiniGames&8] &cゲーム中はこのコマンドを使用できません。"));
    }
}
