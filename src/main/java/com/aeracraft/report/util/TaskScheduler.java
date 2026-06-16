package com.aeracraft.report.util;

import com.aeracraft.report.AeracraftReport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class TaskScheduler {

    private final AeracraftReport plugin;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pointsDecayTask;
    private ScheduledFuture<?> syncCheckTask;
    private boolean isFolia;
    private Object foliaScheduler;
    private Method runTaskMethod;
    private Method runTaskAsynchronouslyMethod;
    private Method runTaskLaterMethod;
    private Method runTaskTimerMethod;
    private Method getGlobalRegionSchedulerMethod;

    public TaskScheduler(AeracraftReport plugin) {
        this.plugin = plugin;
        detectFolia();
    }

    private void detectFolia() {
        try {
            Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            
            getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(null);
            
            foliaScheduler = globalScheduler;
            isFolia = true;
            
            runTaskMethod = globalRegionSchedulerClass.getMethod("run", Plugin.class, Runnable.class);
            runTaskAsynchronouslyMethod = globalRegionSchedulerClass.getMethod("runAsync", Plugin.class, Runnable.class);
            runTaskLaterMethod = globalRegionSchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class);
            runTaskTimerMethod = globalRegionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class);
            
            plugin.getLogger().info("检测到 Folia 服务器，使用 Folia 调度器");
        } catch (Exception e) {
            isFolia = false;
            plugin.getLogger().info("检测到 Paper/Bukkit 服务器，使用标准调度器");
        }
    }

    public void startScheduledTasks() {
        scheduler = Executors.newScheduledThreadPool(2);

        startPointsDecayTask();
        startSyncCheckTask();

        plugin.getLogger().info("定时任务已启动");
    }

    public void stopScheduledTasks() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();

            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            plugin.getLogger().info("定时任务已停止");
        }
    }

    private void startPointsDecayTask() {
        int decayIntervalDays = plugin.getConfigManager().getPointsDecayIntervalDays();
        long intervalMillis = TimeUnit.DAYS.toMillis(decayIntervalDays);

        pointsDecayTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                plugin.getPointsManager().applyDailyDecay().join();
                plugin.getLogger().info("玩家污点分数衰减任务执行完成");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "污点分数衰减任务执行失败", e);
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void startSyncCheckTask() {
        if (!plugin.getConfigManager().isCrossServerEnabled()) {
            return;
        }

        syncCheckTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                checkRedisConnection();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Redis 连接检查失败", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void checkRedisConnection() {
        plugin.getLogger().fine("检查 Redis 连接...");
    }

    public void runSync(Runnable task) {
        if (isFolia && foliaScheduler != null) {
            try {
                runTaskMethod.invoke(foliaScheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Folia runTask 调用失败", e);
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (isFolia && foliaScheduler != null) {
            try {
                runTaskAsynchronouslyMethod.invoke(foliaScheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Folia runAsync 调用失败", e);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runLater(Runnable task, long delay, TimeUnit unit) {
        long ticks = Math.max(1, unit.toMillis(delay) / 50);
        
        if (isFolia && foliaScheduler != null) {
            try {
                runTaskLaterMethod.invoke(foliaScheduler, plugin, task, ticks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Folia runDelayed 调用失败", e);
                Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
        }
    }

    public void runTaskTimer(Runnable task, long initialDelay, long period, TimeUnit unit) {
        long initialTicks = Math.max(1, unit.toMillis(initialDelay) / 50);
        long periodTicks = Math.max(1, unit.toMillis(period) / 50);
        
        if (isFolia && foliaScheduler != null) {
            try {
                runTaskTimerMethod.invoke(foliaScheduler, plugin, task, initialTicks, periodTicks);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Folia runAtFixedRate 调用失败", e);
                Bukkit.getScheduler().runTaskTimer(plugin, task, initialTicks, periodTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, initialTicks, periodTicks);
        }
    }

    public void runSyncForPlayer(Player player, Runnable task) {
        if (isFolia) {
            try {
                Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
                Method getRegionSchedulerMethod = player.getClass().getMethod("getScheduler");
                Object playerScheduler = getRegionSchedulerMethod.invoke(player);
                
                Method playerRunTaskMethod = regionSchedulerClass.getMethod("run", Plugin.class, Runnable.class);
                playerRunTaskMethod.invoke(playerScheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Folia player scheduler 调用失败", e);
                runSync(task);
            }
        } else {
            runSync(task);
        }
    }

    public boolean isFolia() {
        return isFolia;
    }
}