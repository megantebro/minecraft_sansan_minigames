package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SnowballFightGame extends Minigame {

    private static final double MAX_HEALTH = 10.0; // 5 hearts
    private static final double SNOWBALL_DAMAGE = 2.0; // 1 heart

    public SnowballFightGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
    }

    @Override
    protected void onStart() {
        for (Player p : getOnlineAlivePlayers()) {
            p.setHealth(MAX_HEALTH);
            p.setMaxHealth(MAX_HEALTH);
            p.getInventory().setItem(0, new ItemStack(Material.SNOWBALL, 16));
        }
        broadcastTitle("&f&lSnowball Fight!", "&7雪玉で相手を倒せ！");
        broadcastSound(Sound.ENTITY_SNOWBALL_THROW, 1.0f, 1.0f);
    }

    @Override
    protected void onEnd() {
        for (Player p : getAllOnlinePlayers()) {
            p.setMaxHealth(20);
            p.setHealth(20);
        }
    }

    @Override
    protected void onTick() {
        // Replenish snowballs every 3 seconds
        if (timeRemaining % 3 == 0) {
            for (Player p : getOnlineAlivePlayers()) {
                int snowballs = countSnowballs(p);
                if (snowballs < 16) {
                    int toAdd = Math.min(8, 16 - snowballs);
                    p.getInventory().addItem(new ItemStack(Material.SNOWBALL, toAdd));
                }
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.setMaxHealth(20);
        broadcastSound(Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!isPlaying(victim.getUniqueId()) || !isAlive(victim.getUniqueId())) return;

        if (event.getDamager() instanceof Snowball snowball) {
            if (!(snowball.getShooter() instanceof Player attacker)) return;
            if (!isPlaying(attacker.getUniqueId()) || !isAlive(attacker.getUniqueId())) return;
            if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

            event.setCancelled(true);
            double newHealth = victim.getHealth() - SNOWBALL_DAMAGE;

            PlayerData attackerData = players.get(attacker.getUniqueId());
            if (attackerData != null) attackerData.addKill();

            if (newHealth <= 0) {
                victim.setHealth(MAX_HEALTH);
                eliminatePlayer(victim);
                broadcastMessage("&c" + victim.getName() + " &7は &a" + attacker.getName() + " &7に倒された！");
            } else {
                victim.setHealth(newHealth);
                victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
            }
        } else {
            // Cancel non-snowball damage between players
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }

    private int countSnowballs(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SNOWBALL) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("");
        lines.add("&7Players Alive: &f" + getAliveCount());
        lines.add("");
        PlayerData data = players.get(player.getUniqueId());
        if (data != null) {
            lines.add("&7Kills: &f" + data.getKills());
        }
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.SNOWBALL_FIGHT.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.SNOWBALL_FIGHT.getDescription(); }
    @Override public Material getIcon() { return MinigameType.SNOWBALL_FIGHT.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.SNOWBALL_FIGHT; }
    @Override public int getGameDuration() { return 180; }
}
