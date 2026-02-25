package lobby.minecraft_minigames.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;

public class MessageUtil {

    private static final String PREFIX = "&8[&6MiniGames&8] &r";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static Component colorize(String message) {
        return LEGACY.deserialize(message);
    }

    public static void send(Player player, String message) {
        player.sendMessage(colorize(PREFIX + message));
    }

    public static void sendNoPrefix(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static void broadcast(Collection<? extends Player> players, String message) {
        Component comp = colorize(PREFIX + message);
        for (Player player : players) {
            player.sendMessage(comp);
        }
    }

    public static void broadcastAll(String message) {
        Bukkit.getServer().sendMessage(colorize(PREFIX + message));
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );
        player.showTitle(Title.title(colorize(title), colorize(subtitle), times));
    }

    public static void sendTitle(Collection<? extends Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playSound(Collection<? extends Player> players, Sound sound, float volume, float pitch) {
        for (Player player : players) {
            playSound(player, sound, volume, pitch);
        }
    }

    public static Component gameTitle(String gameName) {
        return Component.text("= ", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                .append(Component.text(gameName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" =", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
    }
}
