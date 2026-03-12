package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameState;
import lobby.minecraft_minigames.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class SpleefGame extends Minigame {

    private double deathY;

    public SpleefGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.deathY = arena.getCustomDouble("death-y", arena.getBounds().getMinY());
    }

    @Override
    protected void onStart() {
        for (Player p : getOnlineAlivePlayers()) {
            p.getInventory().setItem(0, new ItemBuilder(Material.DIAMOND_SHOVEL)
                    .name("&bSpleef ショベル")
                    .enchant(Enchantment.EFFICIENCY, 5)
                    .unbreakable()
                    .build());
            p.getInventory().setItem(1, new ItemStack(Material.SNOWBALL, 16));
            p.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
        broadcastTitle("&b&lSpleef!", "&7雪ブロックを壊して相手を落とせ！");
        broadcastSound(Sound.BLOCK_SNOW_BREAK, 1.0f, 1.0f);
    }

    @Override
    protected void onEnd() {
        // Nothing extra needed
    }

    @Override
    protected void onTick() {
        for (Player p : getOnlineAlivePlayers()) {
            if (p.getLocation().getY() < deathY) {
                eliminatePlayer(p);
            }
        }
        // Replenish snowballs every 5 seconds
        if (timeRemaining % 5 == 0) {
            for (Player p : getOnlineAlivePlayers()) {
                int count = 0;
                for (ItemStack item : p.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.SNOWBALL) count += item.getAmount();
                }
                if (count < 16) {
                    p.getInventory().addItem(new ItemStack(Material.SNOWBALL, Math.min(4, 16 - count)));
                }
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        broadcastSound(Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        if (state != GameState.ACTIVE) return;

        Block block = event.getBlock();
        if (!arena.getBounds().contains(block.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (block.getType() != Material.SNOW_BLOCK && block.getType() != Material.WHITE_WOOL) {
            event.setCancelled(true);
            return;
        }
        // Allow the break, don't drop items
        event.setDropItems(false);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player shooter)) return;
        if (!isPlaying(shooter.getUniqueId())) return;
        if (state != GameState.ACTIVE) return;

        if (event.getHitBlock() != null) {
            Block hitBlock = event.getHitBlock();
            if (arena.getBounds().contains(hitBlock.getLocation())) {
                destroyBlock(hitBlock);
                // Destroy adjacent blocks
                for (Block adjacent : new Block[]{
                        hitBlock.getRelative(1, 0, 0),
                        hitBlock.getRelative(-1, 0, 0),
                        hitBlock.getRelative(0, 0, 1),
                        hitBlock.getRelative(0, 0, -1)
                }) {
                    if (arena.getBounds().contains(adjacent.getLocation())) {
                        destroyBlock(adjacent);
                    }
                }
            }
        }
    }

    private void destroyBlock(Block block) {
        if (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.WHITE_WOOL) {
            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5),
                    8, 0.3, 0.1, 0.3, block.getBlockData());
            block.setType(Material.AIR, false);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        if (player.getLocation().getY() < deathY) {
            eliminatePlayer(player);
        }
    }

    @Override public String getDisplayName() { return MinigameType.SPLEEF.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.SPLEEF.getDescription(); }
    @Override public Material getIcon() { return MinigameType.SPLEEF.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.SPLEEF; }
    @Override public int getGameDuration() { return 180; }
}
