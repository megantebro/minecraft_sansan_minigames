package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SumoGame extends Minigame {

    private double deathY;

    public SumoGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.deathY = arena.getCustomDouble("death-y", arena.getBounds().getMinY());
    }

    @Override
    protected void onStart() {
        for (Player p : getOnlineAlivePlayers()) {
            p.getInventory().setItem(0, new ItemBuilder(Material.STICK)
                    .name("&c相撲スティック")
                    .enchant(Enchantment.KNOCKBACK, 2)
                    .build());
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false));
        }
        broadcastTitle("&c&lSumo!", "&7相手を押し出せ！");
        broadcastSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
    }

    @Override
    protected void onEnd() {
        for (Player p : getAllOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.RESISTANCE);
        }
    }

    @Override
    protected void onTick() {
        for (Player p : getOnlineAlivePlayers()) {
            if (p.getLocation().getY() < deathY) {
                eliminatePlayer(p);
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        broadcastSound(Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!isPlaying(victim.getUniqueId()) || !isPlaying(attacker.getUniqueId())) return;
        if (!isAlive(victim.getUniqueId()) || !isAlive(attacker.getUniqueId())) return;
        // Allow knockback but cancel damage
        event.setDamage(0);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            eliminatePlayer(player);
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

    @Override public String getDisplayName() { return MinigameType.SUMO.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.SUMO.getDescription(); }
    @Override public Material getIcon() { return MinigameType.SUMO.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.SUMO; }
    @Override public int getGameDuration() { return 120; }
}
