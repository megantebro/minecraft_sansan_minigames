package lobby.minecraft_minigames.listener;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.manager.LobbyManager;
import lobby.minecraft_minigames.util.ItemBuilder;
import lobby.minecraft_minigames.util.MessageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyListener implements Listener {

    private final Minecraft_minigames plugin;
    private final LobbyManager lobbyManager;
    private final GameManager gameManager;

    private static final String GAME_SELECTOR_TITLE = "Game Selector";

    public LobbyListener(Minecraft_minigames plugin, LobbyManager lobbyManager, GameManager gameManager) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Material type = item.getType();

        if (type == Material.NETHER_STAR) {
            event.setCancelled(true);
            if (lobbyManager.isInLobby(player.getUniqueId())) {
                lobbyManager.toggleReady(player);
            }
        } else if (type == Material.COMPASS) {
            event.setCancelled(true);
            openGameSelector(player);
        } else if (type == Material.RED_DYE) {
            event.setCancelled(true);
            if (lobbyManager.isInLobby(player.getUniqueId())) {
                lobbyManager.removeFromLobby(player);
                player.teleport(lobbyManager.getMainLobbySpawn());
                MessageUtil.send(player, "&7ロビーから退出しました。");
            }
        }
    }

    private void openGameSelector(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, MessageUtil.colorize("&6&l" + GAME_SELECTOR_TITLE));

        MinigameType[] types = MinigameType.values();
        for (int i = 0; i < types.length && i < 27; i++) {
            MinigameType type = types[i];
            int arenaCount = gameManager.getArenaManager().getArenasByType(type).size();
            ItemStack icon = new ItemBuilder(type.getIcon())
                    .name("&6" + type.getDisplayName())
                    .lore(
                            "&7" + type.getDescription(),
                            "",
                            "&fArenas: &a" + arenaCount,
                            "",
                            "&eクリックで参加！"
                    )
                    .build();
            gui.setItem(i, icon);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains(GAME_SELECTOR_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Find the MinigameType by icon
        MinigameType selectedType = null;
        for (MinigameType type : MinigameType.values()) {
            if (type.getIcon() == clicked.getType()) {
                selectedType = type;
                break;
            }
        }

        if (selectedType != null) {
            player.closeInventory();

            // If in lobby, leave first
            if (lobbyManager.isInLobby(player.getUniqueId())) {
                lobbyManager.removeFromLobby(player);
            }

            gameManager.joinGame(player, selectedType);
        }
    }
}
