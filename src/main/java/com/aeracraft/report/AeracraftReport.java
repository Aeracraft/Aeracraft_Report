package com.aeracraft.report;

import com.google.gson.Gson;

import com.aeracraft.report.command.LanguageCommand;
import com.aeracraft.report.command.ReportAdminCommand;
import com.aeracraft.report.command.ReportCommand;
import com.aeracraft.report.command.ReportsCommand;
import com.aeracraft.report.config.ConfigManager;
import com.aeracraft.report.core.EvidenceCollector;
import com.aeracraft.report.core.PointsManager;
import com.aeracraft.report.core.ReportService;
import com.aeracraft.report.database.AuditRepository;
import com.aeracraft.report.database.ConnectionPool;
import com.aeracraft.report.database.PlayerPreferencesRepository;
import com.aeracraft.report.database.ReportRepository;
import com.aeracraft.report.gui.ReportDetailGUI;
import com.aeracraft.report.gui.ReportListGUI;
import com.aeracraft.report.integration.coreprotect.CoreProtectIntegration;
import com.aeracraft.report.integration.economy.VaultHook;
import com.aeracraft.report.integration.placeholder.PAPIExpansion;
import com.aeracraft.report.integration.punishment.FallbackProvider;
import com.aeracraft.report.integration.punishment.LiteBansProvider;
import com.aeracraft.report.integration.punishment.PunishmentProvider;
import com.aeracraft.report.language.LanguageManager;
import com.aeracraft.report.listener.InventoryListener;
import com.aeracraft.report.listener.PlayerListener;
import com.aeracraft.report.listener.PunishmentListener;
import com.aeracraft.report.listener.RedisMessageListener;
import com.aeracraft.report.rest.RestServer;
import com.aeracraft.report.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AeracraftReport extends JavaPlugin {

    private static AeracraftReport instance;
    private Logger logger;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ConnectionPool connectionPool;
    private ReportRepository reportRepository;
    private AuditRepository auditRepository;
    private PlayerPreferencesRepository playerPreferencesRepository;
    private ReportService reportService;
    private EvidenceCollector evidenceCollector;
    private PointsManager pointsManager;
    private PunishmentProvider punishmentProvider;
    private CoreProtectIntegration coreProtectIntegration;
    private VaultHook vaultHook;
    private RestServer restServer;
    private TaskScheduler taskScheduler;
    private Gson gson;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();
        this.gson = new Gson();

        saveDefaultConfig();

        initializeComponents();

        if (!initializePunishmentProvider()) {
            logger.log(Level.SEVERE, "未能初始化任何处罚提供程序！");
        }

        coreProtectIntegration = new CoreProtectIntegration(this);
        coreProtectIntegration.checkDependency();

        vaultHook = new VaultHook(this);
        vaultHook.setup();

        registerCommands();
        registerListeners();

        taskScheduler.startScheduledTasks();

        if (getConfig().getBoolean("rest-api.enabled", false)) {
            restServer = new RestServer(this);
            restServer.start();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        logger.info("AeracraftReport 插件已成功启用！");
    }

    @Override
    public void onDisable() {
        if (restServer != null) {
            restServer.stop();
        }

        if (taskScheduler != null) {
            taskScheduler.stopScheduledTasks();
        }

        if (connectionPool != null) {
            connectionPool.close();
        }

        logger.info("AeracraftReport 插件已禁用！");
    }

    private void initializeComponents() {
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        connectionPool = new ConnectionPool(this);
        connectionPool.initialize();

        reportRepository = new ReportRepository(connectionPool);
        auditRepository = new AuditRepository(connectionPool);
        playerPreferencesRepository = new PlayerPreferencesRepository(connectionPool);

        reportRepository.initializeTables();
        auditRepository.initializeTables();
        playerPreferencesRepository.initializeTables();

        reportService = new ReportService(this);
        evidenceCollector = new EvidenceCollector(this);
        pointsManager = new PointsManager(this);
        taskScheduler = new TaskScheduler(this);
    }

    private boolean initializePunishmentProvider() {
        if (Bukkit.getPluginManager().getPlugin("LiteBans") != null) {
            punishmentProvider = new LiteBansProvider(this);
            logger.info("已选择 LiteBans 作为处罚提供程序");
            return true;
        } else {
            punishmentProvider = new FallbackProvider(this);
            logger.log(Level.WARNING, "未检测到 LiteBans，使用内存存储（仅限测试）");
            return true;
        }
    }

    private void registerCommands() {
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));
        getCommand("reportadmin").setExecutor(new ReportAdminCommand(this));
        getCommand("language").setExecutor(new LanguageCommand(this));
    }

    private void registerListeners() {
        if (isFolia()) {
            try {
                Class<?> eventManagerClass = Class.forName("io.papermc.paper.plugin.event.PluginEventManager");
                Method registerEventMethod = eventManagerClass.getMethod("registerEvent", 
                        Class.class, org.bukkit.event.EventPriority.class,
                        org.bukkit.plugin.Plugin.class, org.bukkit.event.Listener.class);
                
                Object eventManager = Bukkit.getPluginManager();
                
                registerEventMethod.invoke(eventManager, org.bukkit.event.player.PlayerJoinEvent.class, 
                        org.bukkit.event.EventPriority.NORMAL, this, new PlayerListener(this));
                registerEventMethod.invoke(eventManager, org.bukkit.event.player.PlayerJoinEvent.class, 
                        org.bukkit.event.EventPriority.NORMAL, this, new PunishmentListener(this));
                registerEventMethod.invoke(eventManager, org.bukkit.event.player.PlayerJoinEvent.class, 
                        org.bukkit.event.EventPriority.NORMAL, this, new RedisMessageListener(this));
                registerEventMethod.invoke(eventManager, org.bukkit.event.inventory.InventoryClickEvent.class, 
                        org.bukkit.event.EventPriority.NORMAL, this, new InventoryListener(this));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Folia 事件注册失败，回退到标准方式", e);
                registerListenersStandard();
            }
        } else {
            registerListenersStandard();
        }
    }

    private void registerListenersStandard() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PunishmentListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RedisMessageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        languageManager.reload();
    }

    public static AeracraftReport getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public ReportRepository getReportRepository() {
        return reportRepository;
    }

    public AuditRepository getAuditRepository() {
        return auditRepository;
    }

    public PlayerPreferencesRepository getPlayerPreferencesRepository() {
        return playerPreferencesRepository;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public EvidenceCollector getEvidenceCollector() {
        return evidenceCollector;
    }

    public PointsManager getPointsManager() {
        return pointsManager;
    }

    public PunishmentProvider getPunishmentProvider() {
        return punishmentProvider;
    }

    public CoreProtectIntegration getCoreProtectIntegration() {
        return coreProtectIntegration;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public RestServer getRestServer() {
        return restServer;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public Gson getGson() {
        return gson;
    }
}
