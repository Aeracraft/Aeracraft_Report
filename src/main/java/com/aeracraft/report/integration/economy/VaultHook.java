package com.aeracraft.report.integration.economy;

import com.aeracraft.report.AeracraftReport;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class VaultHook {

    private final AeracraftReport plugin;
    private Economy economy;
    private boolean enabled;

    public VaultHook(AeracraftReport plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault 未安装，经济奖励功能将不可用");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("无法获取 Vault Economy 服务");
            return;
        }

        economy = rsp.getProvider();
        enabled = economy != null;

        if (enabled) {
            plugin.getLogger().info("Vault Economy 集成已启用");
        } else {
            plugin.getLogger().warning("Vault Economy 提供者不可用");
        }
    }

    public boolean isEnabled() {
        return enabled && economy != null;
    }

    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Vault deposit 失败", e);
            return false;
        }
    }

    public boolean deposit(String playerName, double amount) {
        if (!isEnabled()) {
            return false;
        }

        try {
            EconomyResponse response = economy.depositPlayer(
                    plugin.getServer().getPlayer(playerName),
                    amount
            );
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Vault deposit 失败", e);
            return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Vault withdraw 失败", e);
            return false;
        }
    }

    public double getBalance(Player player) {
        if (!isEnabled()) {
            return 0.0;
        }

        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Vault getBalance 失败", e);
            return 0.0;
        }
    }

    public String getCurrencyName() {
        if (!isEnabled()) {
            return "";
        }

        try {
            return economy.currencyNamePlural();
        } catch (Exception e) {
            return "coins";
        }
    }

    public String format(double amount) {
        if (!isEnabled()) {
            return String.valueOf(amount);
        }

        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.valueOf(amount);
        }
    }
}
