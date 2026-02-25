package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameState;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HotPotatoGame extends Minigame {

    private UUID currentHolder;
    private int roundTimer;
    private int roundNumber;
    private int maxRoundTime;
    private final Random random = new Random();

    public HotPotatoGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.maxRoundTime = 20;
        this.roundNumber = 0;
    }

    @Override
    protected void onStart() {
        // Give all players resistance (no damage from hits)
        for (Player p : getOnlineAlivePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false));
        }
        broadcastTitle("&c&lHot Potato!", "&7TNTを持っている人が爆発する！");
        broadcastSound(Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
        startNewRound();
    }

    @Override
    protected void onEnd() {
        for (Player p : getAllOnlinePlayers()) {
            p.removePotionEffect(PotionEffectType.RESISTANCE);
            p.setGlowing(false);
        }
    }

    @Override
    protected void onTick() {
        if (state != GameState.ACTIVE) return;

        roundTimer--;

        // Warning sounds
        if (roundTimer <= 5 && roundTimer > 0) {
            broadcastSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f + ((5 - roundTimer) * 0.3f));
            Player holder = Bukkit.getPlayer(currentHolder);
            if (holder != null) {
                showHolderWarning();
            }
        }

        // Explosion!
        if (roundTimer <= 0) {
            Player holder = Bukkit.getPlayer(currentHolder);
            if (holder != null && isAlive(currentHolder)) {
                // Explosion effect
                holder.getWorld().spawnParticle(Particle.EXPLOSION, holder.getLocation(), 3, 0.5, 0.5, 0.5);
                holder.getWorld().playSound(holder.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                holder.setGlowing(false);
                holder.getInventory().clear();
                eliminatePlayer(holder);
            }

            // Start new round if enough players
            if (getAliveCount() > 1) {
                startNewRound();
            }
        }
    }

    private void startNewRound() {
        roundNumber++;
        maxRoundTime = Math.max(8, 20 - roundNumber * 2);
        roundTimer = maxRoundTime;

        // Pick random holder
        List<UUID> alive = getAlivePlayers();
        currentHolder = alive.get(random.nextInt(alive.size()));

        // Clear all players and give TNT to holder
        for (Player p : getOnlineAlivePlayers()) {
            p.getInventory().clear();
            p.setGlowing(false);
        }

        Player holder = Bukkit.getPlayer(currentHolder);
        if (holder != null) {
            holder.getInventory().setItem(0, new ItemStack(Material.TNT));
            holder.setGlowing(true);
            broadcastMessage("&c" + holder.getName() + " &7がTNTを持っている！パンチで渡せ！");
            broadcastSound(Sound.ENTITY_TNT_PRIMED, 1.0f, 1.5f);
        }
    }

    private void showHolderWarning() {
        Player holder = Bukkit.getPlayer(currentHolder);
        if (holder != null) {
            lobby.minecraft_minigames.util.MessageUtil.sendTitle(holder, "&c" + roundTimer, "&7爆発まで...", 0, 25, 0);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!isPlaying(victim.getUniqueId()) || !isPlaying(attacker.getUniqueId())) return;
        if (!isAlive(victim.getUniqueId()) || !isAlive(attacker.getUniqueId())) return;

        event.setDamage(0);

        // Transfer potato
        if (currentHolder != null && currentHolder.equals(attacker.getUniqueId())) {
            currentHolder = victim.getUniqueId();

            attacker.getInventory().clear();
            attacker.setGlowing(false);

            victim.getInventory().setItem(0, new ItemStack(Material.TNT));
            victim.setGlowing(true);

            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 2.0f);
            victim.playSound(victim.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0f, 1.5f);

            broadcastMessage("&c" + victim.getName() + " &7がTNTを受け取った！");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        event.setDamage(0);
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.setGlowing(false);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("");
        lines.add("&7Round: &f" + roundNumber);
        lines.add("&7Bomb Timer: &c" + roundTimer + "s");
        lines.add("");
        Player holder = Bukkit.getPlayer(currentHolder);
        lines.add("&7Holder: &c" + (holder != null ? holder.getName() : "???"));
        lines.add("");
        lines.add("&7Alive: &f" + getAliveCount());
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.HOT_POTATO.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.HOT_POTATO.getDescription(); }
    @Override public Material getIcon() { return MinigameType.HOT_POTATO.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.HOT_POTATO; }
    @Override public int getGameDuration() { return 300; }
}
