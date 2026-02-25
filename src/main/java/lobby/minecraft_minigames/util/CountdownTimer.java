package lobby.minecraft_minigames.util;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class CountdownTimer {

    private final Plugin plugin;
    private int seconds;
    private BukkitTask task;
    private IntConsumer onTick;
    private Runnable onComplete;
    private Runnable onCancel;

    public CountdownTimer(Plugin plugin, int seconds) {
        this.plugin = plugin;
        this.seconds = seconds;
    }

    public CountdownTimer onTick(IntConsumer onTick) {
        this.onTick = onTick;
        return this;
    }

    public CountdownTimer onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public CountdownTimer onCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (seconds <= 0) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                if (onTick != null) onTick.accept(seconds);
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancel() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            if (onCancel != null) onCancel.run();
        }
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    public int getSecondsRemaining() {
        return seconds;
    }
}
