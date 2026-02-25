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

import java.util.*;

public class ColorSwapGame extends Minigame {

    private static final Material[] WOOL_COLORS = {
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL,
            Material.BLUE_WOOL, Material.BROWN_WOOL, Material.GREEN_WOOL,
            Material.RED_WOOL
    };

    private Material currentColor;
    private int roundDelay;
    private int roundNumber;
    private boolean roundActive;
    private int roundCountdown;
    private int phaseCooldown;
    private final Random random = new Random();

    public ColorSwapGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.roundDelay = 8;
        this.roundNumber = 0;
        this.roundActive = false;
        this.phaseCooldown = 3;
    }

    @Override
    protected void onStart() {
        randomizeFloor();
        broadcastTitle("&d&lColor Swap!", "&7指定された色の羊毛に乗れ！");
        broadcastSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        phaseCooldown = 3;
    }

    @Override
    protected void onEnd() {
        // Snapshot restore handled by GameManager
    }

    @Override
    protected void onTick() {
        if (state != GameState.ACTIVE) return;

        if (!roundActive) {
            phaseCooldown--;
            if (phaseCooldown <= 0) {
                startRound();
            }
        } else {
            roundCountdown--;

            if (roundCountdown <= 3 && roundCountdown > 0) {
                broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f + (3 - roundCountdown) * 0.3f);
                for (Player p : getOnlineAlivePlayers()) {
                    lobby.minecraft_minigames.util.MessageUtil.sendTitle(p,
                            "&c" + roundCountdown, "&7" + getColorName(currentColor) + " &fに乗れ！", 0, 25, 0);
                }
            }

            if (roundCountdown <= 0) {
                endRound();
            }
        }
    }

    private void startRound() {
        roundNumber++;
        roundActive = true;
        currentColor = WOOL_COLORS[random.nextInt(WOOL_COLORS.length)];
        roundDelay = Math.max(3, 8 - roundNumber / 2);
        roundCountdown = roundDelay;

        broadcastMessage("&d" + getColorName(currentColor) + " &fに乗れ！ &7(" + roundCountdown + "秒)");
        broadcastTitle("&6" + getColorName(currentColor), "&f" + roundCountdown + "秒以内に乗れ！");
        broadcastSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f);
    }

    private void endRound() {
        roundActive = false;

        // Check who's standing on the correct color
        List<Player> toEliminate = new ArrayList<>();
        for (Player p : getOnlineAlivePlayers()) {
            Block below = p.getLocation().subtract(0, 1, 0).getBlock();
            if (below.getType() != currentColor) {
                toEliminate.add(p);
            }
        }

        for (Player p : toEliminate) {
            broadcastMessage("&c" + p.getName() + " &7は正しい色に乗っていなかった！");
            eliminatePlayer(p);
        }

        if (getAliveCount() > 1) {
            broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            // Randomize floor for next round
            randomizeFloor();
            phaseCooldown = 3;
        }
    }

    private void randomizeFloor() {
        int floorY = arena.getCustomInt("floor-y", arena.getBounds().getMinY());
        World world = arena.getWorld();

        for (int x = arena.getBounds().getMinX(); x <= arena.getBounds().getMaxX(); x++) {
            for (int z = arena.getBounds().getMinZ(); z <= arena.getBounds().getMaxZ(); z++) {
                Block block = world.getBlockAt(x, floorY, z);
                if (!block.getType().isAir()) {
                    block.setType(WOOL_COLORS[random.nextInt(WOOL_COLORS.length)], false);
                }
            }
        }
    }

    private String getColorName(Material wool) {
        return switch (wool) {
            case WHITE_WOOL -> "&f白";
            case ORANGE_WOOL -> "&6オレンジ";
            case MAGENTA_WOOL -> "&dマゼンタ";
            case LIGHT_BLUE_WOOL -> "&b水色";
            case YELLOW_WOOL -> "&e黄色";
            case LIME_WOOL -> "&aライム";
            case PINK_WOOL -> "&dピンク";
            case CYAN_WOOL -> "&3シアン";
            case PURPLE_WOOL -> "&5紫";
            case BLUE_WOOL -> "&1青";
            case BROWN_WOOL -> "&4茶色";
            case GREEN_WOOL -> "&2緑";
            case RED_WOOL -> "&c赤";
            default -> wool.name();
        };
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 10);
        broadcastSound(Sound.ENTITY_BLAZE_HURT, 0.5f, 0.5f);
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
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("&7Round: &f" + roundNumber);
        lines.add("");
        if (roundActive) {
            lines.add("&7Color: " + getColorName(currentColor));
            lines.add("&7Remaining: &c" + roundCountdown + "s");
        } else {
            lines.add("&7次のラウンド準備中...");
        }
        lines.add("");
        lines.add("&7Alive: &f" + getAliveCount());
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.COLOR_SWAP.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.COLOR_SWAP.getDescription(); }
    @Override public Material getIcon() { return MinigameType.COLOR_SWAP.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.COLOR_SWAP; }
    @Override public int getGameDuration() { return 300; }
}
