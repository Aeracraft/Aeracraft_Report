package com.aeracraft.report.integration.coreprotect;

import com.aeracraft.report.AeracraftReport;
import com.aeracraft.report.model.Evidence.BlockChange;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CoreProtectIntegration {

    private final AeracraftReport plugin;
    private CoreProtectAPI coreProtectAPI;
    private boolean enabled;

    public CoreProtectIntegration(AeracraftReport plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public void checkDependency() {
        if (Bukkit.getPluginManager().getPlugin("CoreProtect") != null) {
            CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (coreProtect != null) {
                coreProtectAPI = coreProtect.getAPI();
                if (coreProtectAPI.isEnabled()) {
                    enabled = true;
                    plugin.getLogger().info("CoreProtect 集成已启用");
                }
            }
        }

        if (!enabled) {
            plugin.getLogger().info("CoreProtect 未安装或未启用，相关功能将不可用");
        }
    }

    public boolean isEnabled() {
        return enabled && coreProtectAPI != null;
    }

    public List<BlockChange> lookup(String worldName, int x, int y, int z, int radius, int minutes) {
        List<BlockChange> results = new ArrayList<>();

        if (!isEnabled()) {
            return results;
        }

        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return results;
            }
            Location location = new Location(world, x, y, z);

            List<String[]> lookupResults = coreProtectAPI.performLookup(
                    minutes * 60,
                    null,
                    null,
                    null,
                    null,
                    null,
                    radius,
                    location
            );

            if (lookupResults != null) {
                for (String[] result : lookupResults) {
                    BlockChange blockChange = new BlockChange();
                    blockChange.setTimestamp(Long.parseLong(result[0]));
                    blockChange.setPlayer(result[1]);
                    blockChange.setAction(Integer.parseInt(result[2]) == 0 ? "BREAK" : "PLACE");
                    blockChange.setBlock(result[3]);
                    blockChange.setX(Integer.parseInt(result[4]));
                    blockChange.setY(Integer.parseInt(result[5]));
                    blockChange.setZ(Integer.parseInt(result[6]));
                    blockChange.setWorld(worldName);
                    results.add(blockChange);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "CoreProtect 查询失败", e);
        }

        return results;
    }

    public CompletableFuture<List<BlockChange>> lookupAsync(String worldName, int x, int y, int z, int radius, int minutes) {
        return CompletableFuture.supplyAsync(() -> lookup(worldName, x, y, z, radius, minutes));
    }

    public List<BlockChange> getBlockHistory(String worldName, int x, int y, int z, int limit) {
        List<BlockChange> results = new ArrayList<>();

        if (!isEnabled()) {
            return results;
        }

        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return results;
            }
            Location location = new Location(world, x, y, z);

            List<String[]> lookupResults = coreProtectAPI.performLookup(
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    limit,
                    location
            );

            if (lookupResults != null) {
                for (String[] result : lookupResults) {
                    BlockChange blockChange = new BlockChange();
                    blockChange.setTimestamp(Long.parseLong(result[0]));
                    blockChange.setPlayer(result[1]);
                    blockChange.setAction(Integer.parseInt(result[2]) == 0 ? "BREAK" : "PLACE");
                    blockChange.setBlock(result[3]);
                    blockChange.setX(Integer.parseInt(result[4]));
                    blockChange.setY(Integer.parseInt(result[5]));
                    blockChange.setZ(Integer.parseInt(result[6]));
                    blockChange.setWorld(worldName);
                    results.add(blockChange);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "CoreProtect 历史查询失败", e);
        }

        return results;
    }
}