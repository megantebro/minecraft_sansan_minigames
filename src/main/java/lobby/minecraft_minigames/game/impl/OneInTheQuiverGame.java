package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameResult;
import lobby.minecraft_minigames.model.PlayerData;
import lobby.minecraft_minigames.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class OneInTheQuiverGame extends Minigame {

    private static final int KILLS_TO_WIN = 10;
    private final Map<UUID, Integer> killCounts;

    public OneInTheQuiverGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.killCounts = new HashMap<>();
    }

    @Override
    protected void onStart() {
        for (UUID uuid : players.keySet()) {
            killCounts.put(uuid, 0);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                giveKit(p);
            }
        }
        broadcastTitle("&e&lOne in the Quiver!", "&7矢1本で一撃！" + KILLS_TO_WIN + "キル先取！");
        broadcastSound(Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);
    }

    @Override
    protected void onEnd() {
        // Nothing extra
    }

    @Override
    protected void onTick() {
        // Check win condition
        for (Map.Entry<UUID, Integer> entry : killCounts.entrySet()) {
            if (entry.getValue() >= KILLS_TO_WIN) {
                end(determineResults());
                return;
            }
        }
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        // In OITQ, "elimination" is a temporary death - respawn after delay
    }

    private void giveKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemBuilder(Material.WOODEN_SWORD)
                .name("&e近接武器")
                .unbreakable()
                .build());
        player.getInventory().setItem(1, new ItemBuilder(Material.BOW)
                .name("&cOne Shot Bow")
                .enchant(Enchantment.POWER, 10)
                .unbreakable()
                .build());
        player.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));
        player.setHealth(20);
        player.setGameMode(GameMode.ADVENTURE);
    }

    private void respawnPlayer(Player player) {
        List<Location> spawns = arena.getSpawnPoints();
        Location spawn = spawns.get(new Random().nextInt(spawns.size()));
        player.teleport(spawn);
        giveKit(player);
        player.setGameMode(GameMode.ADVENTURE);

        PlayerData data = players.get(player.getUniqueId());
        if (data != null) {
            data.setAlive(true);
            data.setSpectating(false);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!isPlaying(victim.getUniqueId()) || !isAlive(victim.getUniqueId())) return;

        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || !isPlaying(attacker.getUniqueId()) || !isAlive(attacker.getUniqueId())) return;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        event.setCancelled(true);

        boolean isBowKill = event.getDamager() instanceof Arrow;

        // Kill the victim
        killCounts.merge(attacker.getUniqueId(), 1, Integer::sum);
        PlayerData attackerData = players.get(attacker.getUniqueId());
        if (attackerData != null) {
            attackerData.addKill();
            attackerData.setScore(killCounts.get(attacker.getUniqueId()));
        }

        String killType = isBowKill ? "&c弓" : "&e近接";
        broadcastMessage(killType + " &7| &a" + attacker.getName() + " &7-> &c" + victim.getName() +
                " &8[&f" + killCounts.get(attacker.getUniqueId()) + "/" + KILLS_TO_WIN + "&8]");

        // Give attacker an arrow
        attacker.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);

        // Respawn victim after short delay
        victim.setGameMode(GameMode.SPECTATOR);
        PlayerData victimData = players.get(victim.getUniqueId());
        if (victimData != null) {
            victimData.setAlive(false);
            victimData.addDeath();
        }

        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (isPlaying(victim.getUniqueId()) && state == lobby.minecraft_minigames.model.GameState.ACTIVE) {
                respawnPlayer(victim);
            }
        }, 60L); // 3 second respawn
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        // Cancel fall damage and other environmental damage
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!isPlaying(shooter.getUniqueId())) return;
        // Remove arrows that hit blocks
        if (event.getHitBlock() != null) {
            arrow.remove();
        }
    }

    @Override
    protected void checkWinCondition() {
        for (Map.Entry<UUID, Integer> entry : killCounts.entrySet()) {
            if (entry.getValue() >= KILLS_TO_WIN) {
                end(determineResults());
                return;
            }
        }
    }

    @Override
    public GameResult determineResults() {
        // Sort by kill count descending
        List<UUID> placements = killCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<UUID, Integer> scores = new HashMap<>(killCounts);
        return new GameResult(getType(), placements, scores);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("&7Goal: &f" + KILLS_TO_WIN + " kills");
        lines.add("");

        // Show top players
        killCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : "???";
                    String color = entry.getKey().equals(player.getUniqueId()) ? "&a" : "&f";
                    lines.add(color + name + "&7: &e" + entry.getValue());
                });
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.ONE_IN_THE_QUIVER.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.ONE_IN_THE_QUIVER.getDescription(); }
    @Override public Material getIcon() { return MinigameType.ONE_IN_THE_QUIVER.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.ONE_IN_THE_QUIVER; }
    @Override public int getGameDuration() { return 180; }
}
