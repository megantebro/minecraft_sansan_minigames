package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameState;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class BlockShuffleGame extends Minigame {

    private static final Material[] VALID_BLOCKS = {
            Material.GRASS_BLOCK, Material.STONE, Material.OAK_LOG, Material.SAND,
            Material.GRAVEL, Material.DIRT, Material.COBBLESTONE, Material.OAK_PLANKS,
            Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.CLAY, Material.MOSSY_COBBLESTONE,
            Material.IRON_ORE, Material.COAL_ORE, Material.CRAFTING_TABLE, Material.BRICKS,
            Material.BOOKSHELF, Material.OBSIDIAN, Material.SNOW_BLOCK, Material.ICE,
            Material.PUMPKIN, Material.MELON, Material.HAY_BLOCK, Material.TERRACOTTA
    };

    private final Map<UUID, Material> assignedBlocks;
    private final Set<UUID> completedThisRound;
    private int roundTimer;
    private int roundNumber;
    private boolean roundActive;
    private int pauseTimer;
    private final Random random = new Random();

    public BlockShuffleGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.assignedBlocks = new HashMap<>();
        this.completedThisRound = new HashSet<>();
        this.roundNumber = 0;
        this.roundActive = false;
        this.pauseTimer = 3;
    }

    @Override
    protected void onStart() {
        for (Player p : getOnlineAlivePlayers()) {
            p.setGameMode(GameMode.ADVENTURE);
        }
        broadcastTitle("&2&lBlock Shuffle!", "&7指定されたブロックを探して乗れ！");
        broadcastSound(Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
        pauseTimer = 3;
    }

    @Override
    protected void onEnd() {
        // Nothing extra
    }

    @Override
    protected void onTick() {
        if (state != GameState.ACTIVE) return;

        if (!roundActive) {
            pauseTimer--;
            if (pauseTimer <= 0) {
                startNewRound();
            }
        } else {
            roundTimer--;

            if (roundTimer <= 5 && roundTimer > 0) {
                broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f + (5 - roundTimer) * 0.3f);
                for (Player p : getOnlineAlivePlayers()) {
                    if (!completedThisRound.contains(p.getUniqueId())) {
                        lobby.minecraft_minigames.util.MessageUtil.sendTitle(p,
                                "&c" + roundTimer, "&7急いで！", 0, 25, 0);
                    }
                }
            }

            if (roundTimer <= 0) {
                endRound();
            }
        }
    }

    private void startNewRound() {
        roundNumber++;
        roundActive = true;
        completedThisRound.clear();
        roundTimer = Math.max(15, 30 - roundNumber * 2);

        for (UUID uuid : getAlivePlayers()) {
            Material block = VALID_BLOCKS[random.nextInt(VALID_BLOCKS.length)];
            assignedBlocks.put(uuid, block);

            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                String blockName = formatBlockName(block);
                lobby.minecraft_minigames.util.MessageUtil.sendTitle(p,
                        "&6" + blockName, "&f" + roundTimer + "秒以内に見つけて乗れ！", 5, 40, 10);
                lobby.minecraft_minigames.util.MessageUtil.send(p,
                        "&6Round " + roundNumber + ": &f" + blockName + " &7を探して乗れ！");
            }
        }
        broadcastSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
    }

    private void endRound() {
        roundActive = false;

        List<Player> toEliminate = new ArrayList<>();
        for (UUID uuid : getAlivePlayers()) {
            if (!completedThisRound.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    toEliminate.add(p);
                }
            }
        }

        for (Player p : toEliminate) {
            broadcastMessage("&c" + p.getName() + " &7はブロックを見つけられなかった！");
            eliminatePlayer(p);
        }

        if (getAliveCount() > 1) {
            broadcastMessage("&aラウンド " + roundNumber + " 終了！");
            pauseTimer = 3;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        if (!roundActive) return;
        if (completedThisRound.contains(player.getUniqueId())) return;

        Material target = assignedBlocks.get(player.getUniqueId());
        if (target == null) return;

        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        if (below.getType() == target) {
            completedThisRound.add(player.getUniqueId());
            broadcastMessage("&a" + player.getName() + " &fがブロックを見つけた！ &8(" +
                    completedThisRound.size() + "/" + getAliveCount() + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            lobby.minecraft_minigames.util.MessageUtil.sendTitle(player, "&a完了！", "&7他のプレイヤーを待っています...", 5, 40, 10);

            // If all alive players completed, end round early
            if (completedThisRound.size() >= getAliveCount()) {
                endRound();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPlaying(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPlaying(event.getPlayer().getUniqueId())) {
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
        broadcastSound(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private String formatBlockName(Material mat) {
        String name = mat.name().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("&7Round: &f" + roundNumber);
        lines.add("");
        if (roundActive) {
            Material target = assignedBlocks.get(player.getUniqueId());
            if (target != null) {
                if (completedThisRound.contains(player.getUniqueId())) {
                    lines.add("&aFound!");
                } else {
                    lines.add("&7Find: &f" + formatBlockName(target));
                    lines.add("&7Timer: &c" + roundTimer + "s");
                }
            }
        } else {
            lines.add("&7次のラウンド準備中...");
        }
        lines.add("");
        lines.add("&7Alive: &f" + getAliveCount());
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.BLOCK_SHUFFLE.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.BLOCK_SHUFFLE.getDescription(); }
    @Override public Material getIcon() { return MinigameType.BLOCK_SHUFFLE.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.BLOCK_SHUFFLE; }
    @Override public int getGameDuration() { return 300; }
}
