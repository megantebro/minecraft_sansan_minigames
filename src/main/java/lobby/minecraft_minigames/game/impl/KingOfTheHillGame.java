package lobby.minecraft_minigames.game.impl;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.Minigame;
import lobby.minecraft_minigames.game.MinigameType;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;
import lobby.minecraft_minigames.model.GameResult;
import lobby.minecraft_minigames.model.PlayerData;
import lobby.minecraft_minigames.util.Cuboid;
import lobby.minecraft_minigames.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class KingOfTheHillGame extends Minigame {

    private static final int SCORE_TO_WIN = 60;
    private final Map<UUID, Integer> scores;
    private Cuboid hillRegion;

    public KingOfTheHillGame(Minecraft_minigames plugin, GameManager gameManager, Arena arena) {
        super(plugin, gameManager, arena);
        this.scores = new HashMap<>();

        // Hill region: center of arena, smaller area
        int cx = (arena.getBounds().getMinX() + arena.getBounds().getMaxX()) / 2;
        int cz = (arena.getBounds().getMinZ() + arena.getBounds().getMaxZ()) / 2;
        int cy = arena.getCustomInt("hill-y", arena.getBounds().getMaxY());
        int radius = arena.getCustomInt("hill-radius", 3);
        Location c1 = new Location(arena.getWorld(), cx - radius, cy - 1, cz - radius);
        Location c2 = new Location(arena.getWorld(), cx + radius, cy + 3, cz + radius);
        this.hillRegion = new Cuboid(c1, c2);
    }

    @Override
    protected void onStart() {
        for (UUID uuid : players.keySet()) {
            scores.put(uuid, 0);
        }
        for (Player p : getOnlineAlivePlayers()) {
            p.getInventory().setItem(0, new ItemBuilder(Material.STICK)
                    .name("&6ノックバック棒")
                    .enchant(Enchantment.KNOCKBACK, 1)
                    .build());
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false));
        }
        broadcastTitle("&6&lKing of the Hill!", "&7中央を制圧してポイント獲得！");
        broadcastSound(Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.0f);
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
            if (hillRegion.contains(p.getLocation())) {
                UUID uuid = p.getUniqueId();
                scores.merge(uuid, 1, Integer::sum);
                PlayerData data = players.get(uuid);
                if (data != null) data.setScore(scores.get(uuid));

                // Visual feedback
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 2.0f);

                if (scores.get(uuid) >= SCORE_TO_WIN) {
                    end(determineResults());
                    return;
                }
            }
        }

        // Particle effects on hill
        Location center = new Location(arena.getWorld(),
                (hillRegion.getMinX() + hillRegion.getMaxX()) / 2.0,
                hillRegion.getMaxY(),
                (hillRegion.getMinZ() + hillRegion.getMaxZ()) / 2.0);
        arena.getWorld().spawnParticle(Particle.DUST, center, 5, 2, 0.5, 2,
                new Particle.DustOptions(Color.YELLOW, 1.5f));
    }

    @Override
    protected void onPlayerEliminate(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!isPlaying(victim.getUniqueId()) || !isPlaying(attacker.getUniqueId())) return;
        // Allow knockback, cancel damage
        event.setDamage(0);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPlaying(player.getUniqueId())) return;
        // Cancel fall damage etc
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isPlaying(player.getUniqueId()) || !isAlive(player.getUniqueId())) return;
        // Respawn if fallen below arena
        double deathY = arena.getCustomDouble("death-y", arena.getBounds().getMinY() - 5);
        if (player.getLocation().getY() < deathY) {
            List<Location> spawns = arena.getSpawnPoints();
            player.teleport(spawns.get(new Random().nextInt(spawns.size())));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    @Override
    protected void checkWinCondition() {
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            if (entry.getValue() >= SCORE_TO_WIN) {
                end(determineResults());
                return;
            }
        }
    }

    @Override
    public GameResult determineResults() {
        List<UUID> placements = scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        Map<UUID, Integer> scoreMap = new HashMap<>(scores);
        return new GameResult(getType(), placements, scoreMap);
    }

    @Override
    public List<String> getScoreboardLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7Time: &f" + formatTime(timeRemaining));
        lines.add("&7Goal: &f" + SCORE_TO_WIN + " pts");
        lines.add("");
        scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : "???";
                    String color = entry.getKey().equals(player.getUniqueId()) ? "&a" : "&f";
                    lines.add(color + name + "&7: &6" + entry.getValue() + "pt");
                });
        return lines;
    }

    @Override public String getDisplayName() { return MinigameType.KING_OF_THE_HILL.getDisplayName(); }
    @Override public String getDescription() { return MinigameType.KING_OF_THE_HILL.getDescription(); }
    @Override public Material getIcon() { return MinigameType.KING_OF_THE_HILL.getIcon(); }
    @Override public MinigameType getType() { return MinigameType.KING_OF_THE_HILL; }
    @Override public int getGameDuration() { return 180; }
}
