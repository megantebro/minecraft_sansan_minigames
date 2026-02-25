package lobby.minecraft_minigames.listener;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.manager.LobbyManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GlobalListener implements Listener {

    private final Minecraft_minigames plugin;
    private final GameManager gameManager;

    public GlobalListener(Minecraft_minigames plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LobbyManager lobbyManager = gameManager.getLobbyManager();
        if (lobbyManager != null && lobbyManager.getMainLobbySpawn() != null) {
            player.teleport(lobbyManager.getMainLobbySpawn());
        }
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LobbyManager lobbyManager = gameManager.getLobbyManager();

        if (gameManager.isInGame(player.getUniqueId())) {
            gameManager.leaveGame(player);
        } else if (lobbyManager != null && lobbyManager.isInLobby(player.getUniqueId())) {
            lobbyManager.removeFromLobby(player);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Prevent hunger in lobby
        if (!gameManager.isInGame(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Prevent damage in lobby
        if (!gameManager.isInGame(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Prevent dropping items
        event.setCancelled(true);
    }
}
