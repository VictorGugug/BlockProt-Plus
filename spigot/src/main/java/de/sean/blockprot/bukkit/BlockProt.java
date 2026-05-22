/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit;

import de.sean.blockprot.bukkit.audit.AuditLogger;
import de.sean.blockprot.bukkit.commands.BlockProtCommand;
import de.sean.blockprot.bukkit.config.DefaultConfig;
import de.sean.blockprot.bukkit.config.WorldsConfig;
import de.sean.blockprot.bukkit.integrations.*;
import de.sean.blockprot.bukkit.listeners.*;
import de.sean.blockprot.bukkit.metrics.IntegrationBarChart;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.TimedAccessManager;
import de.sean.blockprot.bukkit.storage.HybridDatabase;
import de.sean.blockprot.bukkit.tasks.ConfigFileWatcher;
import de.sean.blockprot.bukkit.tasks.BackupTask;
import de.sean.blockprot.bukkit.tasks.InactivityCleanupTask;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.ProfileService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.Objects;

/**
 * The main plugin instance of BlockProt.
 */
public final class BlockProt extends JavaPlugin {
    public static final int pluginId = 9999;
    public static final String defaultLanguageFile = "translations_en.yml";

    @Nullable private static BlockProt instance;
    @Nullable private static DefaultConfig defaultConfig = null;

    private final ArrayList<PluginIntegration> integrations = new ArrayList<>();

    @Nullable private static SQLiteCache playerProfileCache = null;
    @Nullable private static ProfileService playerProfileService = null;
    @Nullable private static WorldsConfig worldsConfig = null;
    @Nullable private static AuditLogger auditLogger = null;
    @Nullable private static HybridDatabase hybridDatabase = null;
    @Nullable private ConfigFileWatcher fileWatcher = null;

    private Metrics metrics;

    @NotNull
    public static BlockProt getInstance() {
        assert instance != null;
        return instance;
    }

    @NotNull
    public static DefaultConfig getDefaultConfig() throws AssertionError {
        assert defaultConfig != null : "default config should not be null.";
        return defaultConfig;
    }

    public List<PluginIntegration> getIntegrations() {
        return Collections.unmodifiableList(integrations);
    }

    @NotNull public static ProfileCache    getProfileCache()   { assert playerProfileCache   != null; return playerProfileCache; }
    @NotNull public static ProfileService  getProfileService() { assert playerProfileService != null; return playerProfileService; }
    @Nullable public static WorldsConfig   getWorldsConfig()   { return worldsConfig; }
    @Nullable public static AuditLogger    getAuditLogger()    { return auditLogger; }
    @Nullable public static HybridDatabase getHybridDatabase() { return hybridDatabase; }

    @Override
    public void onLoad() {
        instance = this;
        try {
            playerProfileCache   = new SQLiteCache(new File(Bukkit.getWorldContainer(), "blockprot_usercache.sqlite"));
            playerProfileService = new CachedProfileService(playerProfileCache);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open SQLite connection to usercache database", e);
        }
        try { registerIntegration(new TownyIntegration());          } catch (NoClassDefFoundError ignored) {}
        try { registerIntegration(new PlaceholderAPIIntegration()); } catch (NoClassDefFoundError ignored) {}
        for (PluginIntegration integration : integrations) {
            try { integration.load(); } catch (NoClassDefFoundError ignored) {}
        }
    }

    @Override
    public void onEnable() {
        if (isRunningCraftBukkit()) {
            final var message = "This plugin does not support CraftBukkit. Please use Spigot or Paper.";
            getLogger().severe(message);
            getServer().getPluginManager().registerEvents(new ErrorEventListener(message), this);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, new UpdateChecker(this.getDescription()));
        MinecraftVersion.disableUpdateCheck();

        new BlockProtAPI(this);
        BlockProtLogger.init(this.getDataFolder());
        String version = this.getDescription().getVersion();

        // Session header — written to the log file only, not the console.
        BlockProtLogger.log("=== Startup: BlockProt v" + version + " ===");
        BlockProtLogger.log("Server: " + Bukkit.getVersion());
        BlockProtLogger.log("Runtime: " + VersionCompat.getDiagnosticString());
        if (VersionCompat.is26Family()) {
            BlockProtLogger.log("Version scheme: 26.x year-based detected.");
        }

        BlockProtConsole.beginStartup(this.getLogger());

        StatHandler.enable();
        this.cleanLegacyConfigKeys();
        this.saveDefaultConfig();
        saveResourceSilent("blocks.yml", false);
        saveResourceSilent("mysql/mysql.yml", false);
        this.reloadConfigAndTranslations();
        VersionValidator.validateStartup();

        boolean hasUpgradeData = BackupTask.hasPriorData(this.getDataFolder());
        if (hasUpgradeData) {
            new BackupTask(this.getDataFolder()).run();
        }
        this.mergeMissingConfigKeys();
        saveResourceSilent("worlds.yml", false);
        if (defaultConfig.isWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        }

        hybridDatabase = new HybridDatabase(this);
        hybridDatabase.start(defaultConfig);

        try {
            auditLogger = new AuditLogger(this.getDataFolder());
            BlockProtConsole.success(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_STARTED));
        } catch (Exception e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__AUDIT_LOGGER_FAILED)
                .replace("{error}", e.getMessage()));
        }

        fileWatcher = new ConfigFileWatcher(this);
        fileWatcher.start();

        int inactivityDays = this.getConfig().getInt("inactivity_cleanup_days", -1);
        if (inactivityDays > 0) {
            Bukkit.getScheduler().runTaskAsynchronously(this, new InactivityCleanupTask(inactivityDays));
        }

        metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new IntegrationBarChart());

        // Enable integrations — one console line per enabled integration.
        for (PluginIntegration integration : integrations) {
            try {
                integration.enable();
                if (integration.isEnabled()) {
                    BlockProtConsole.integrationEnabled(integration.name);
                    BlockProtLogger.log("integration", "Enabled: " + integration.name);
                }
            } catch (NoClassDefFoundError ignored) {}
        }

        BlockProtConsole.printStartupBanner(version);

        /* Register Listeners */
        final PluginManager pm = getServer().getPluginManager();
        registerEvent(pm, new BlockEventListener(this));
        registerEvent(pm, new EntityEventListener());
        registerEvent(pm, new ExplodeEventListener());
        registerEvent(pm, new HopperEventListener());
        registerEvent(pm, new InteractEventListener());
        registerEvent(pm, new InventoryEventListener());
        registerEvent(pm, new JoinEventListener());
        registerEvent(pm, new PistonEventListener());
        registerEvent(pm, new RedstoneEventListener());
        registerEvent(pm, new LockEffectListener());

        // ── Pet protection listeners (BlockProt Reloaded) ──────────────────────
        // Always registered so the config toggle is hot-reloadable (/bp reload).
        // Each event handler checks isPetProtectionEnabled() at the top and returns
        // immediately when disabled, adding zero overhead when the feature is off.
        registerEvent(pm, new PetProtectionListener());
        registerEvent(pm, new PetMenuOpenListener());
        // ─────────────────────────────────────────────────────────────────────

        if (defaultConfig.isWorldEditPasteAutolockEnabled()) {
            registerEvent(pm, new WorldEditPasteListener(this));
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__WORLDEDIT_LISTENER_ENABLED));
        }

        Objects.requireNonNull(this.getCommand("blockprot"))
            .setExecutor(new BlockProtCommand());

        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!isRunningCraftBukkit()) {
            getLogger().info("Saving statistics to disk...");
            StatHandler.disable();
            getServer().getOnlinePlayers().forEach(HumanEntity::closeInventory);
        }
        TimedAccessManager.cancelAll();
        if (fileWatcher    != null) fileWatcher.stop();
        if (auditLogger    != null) auditLogger.close();
        if (hybridDatabase != null) hybridDatabase.close();
        BlockProtLogger.close();
        super.onDisable();
    }

    public void reloadConfigAndTranslations() {
        this.mergeMissingConfigKeys();
        this.reloadConfig();
        defaultConfig = new DefaultConfig(this.getConfig(), this.getDataFolder());

        Translator.resetTranslations();
        Translator.DEFAULT_FALLBACK = defaultConfig.getTranslationFallbackString();

        final String langFolder = "lang/";
        for (String resource : Translator.DEFAULT_TRANSLATION_FILES) {
            File diskFile = new File(this.getDataFolder(), langFolder + resource);
            if (!diskFile.exists()) {
                this.saveResource(langFolder + resource, false);
            } else {
                mergeMissingLangKeys(langFolder, resource, diskFile);
            }
        }

        InputStream defaultLanguageStream = this.getResource(langFolder + defaultLanguageFile);
        if (defaultLanguageStream == null) {
            throw new RuntimeException("Failed to load the default language file. The plugin JAR may be corrupt.");
        }
        YamlConfiguration defaultLanguageConfig = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(defaultLanguageStream)));

        final String fileName = defaultConfig.getLanguageFile() == null
            ? defaultLanguageFile : defaultConfig.getLanguageFile();
        YamlConfiguration wantedConfig = saveAndLoadConfigFile(
            langFolder, fileName, BlockProt.defaultConfig.shouldReplaceTranslations());
        Translator.loadFromConfigs(defaultLanguageConfig, wantedConfig);

        if (defaultConfig.isWorldsConfigEnabled()) {
            File worldsFile = new File(this.getDataFolder(), "worlds.yml");
            YamlConfiguration worldsDisk = WorldsConfig.scanAndPopulate(worldsFile, this.getConfig(), this.getLogger());
            worldsConfig = new WorldsConfig(worldsDisk);
        } else {
            worldsConfig = null;
            BlockProtLogger.log("worlds-scan", "worlds_config_enabled=false; using global config.yml lockable lists.");
        }

        for (PluginIntegration integration : integrations) {
            integration.reload();
        }
    }

    private void registerEvent(@NotNull PluginManager pm, Listener listener) {
        pm.registerEvents(listener, this);
    }

    /**
     * Registers a plugin integration. Logs to the session file only;
     * console output is deferred until after the integration is confirmed enabled.
     */
    void registerIntegration(@NotNull PluginIntegration integration) {
        this.integrations.add(integration);
        BlockProtLogger.log("integration", "Registered: " + integration.name);
    }

    @Nullable
    public Plugin getPlugin(String pluginName) {
        return this.getServer().getPluginManager().getPlugin(pluginName);
    }

    @NotNull
    public YamlConfiguration saveAndLoadConfigFile(String folder, String name, boolean replace) {
        final String path = folder + (folder.endsWith("/") ? "" : "/") + name;
        File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            try {
                this.saveResource(path, replace);
            } catch (IllegalArgumentException e) {
                getLogger().warning(Translator.get(TranslationKey.CONSOLE__CONFIG_LANGUAGE_MISSING)
                    .replace("{file}", name));
                return new YamlConfiguration();
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void mergeMissingLangKeys(@NotNull String langFolder, @NotNull String resource, @NotNull File diskFile) {
        InputStream jarStream = this.getResource(langFolder + resource);
        if (jarStream == null) return;

        YamlConfiguration jarConfig  = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream)));
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);

        int added = 0;
        for (String key : jarConfig.getKeys(true)) {
            if (jarConfig.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("lang-merge", resource + " — added missing key: " + key);
            }
        }

        if (added > 0) {
            try {
                diskConfig.save(diskFile);
                getLogger().info(Translator.get(TranslationKey.CONSOLE__LANG_KEYS_UPDATED)
                    .replace("{file}", resource).replace("{count}", String.valueOf(added)));
            } catch (IOException e) {
                getLogger().warning(Translator.get(TranslationKey.CONSOLE__LANG_KEYS_SAVE_FAILED)
                    .replace("{file}", resource).replace("{error}", e.getMessage()));
            }
        }
    }

    /**
     * Rewrites config.yml on disk using the bundled JAR template as a base,
     * preserving all values the administrator has already configured.
     * This ensures format, comments, and sections are always clean,
     * and removes obsolete keys (mysql, console, lockable_*) if present.
     */
    private void cleanLegacyConfigKeys() {
        File diskFile = new File(this.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;

        YamlConfiguration userValues = YamlConfiguration.loadConfiguration(diskFile);

        InputStream jarStream = this.getResource("config.yml");
        if (jarStream == null) return;
        YamlConfiguration template = YamlConfiguration.loadConfiguration(
            new BufferedReader(new InputStreamReader(jarStream)));

        // Overlay user values onto the clean template (template keys only).
        for (String key : template.getKeys(true)) {
            if (template.isConfigurationSection(key)) continue;
            if (userValues.contains(key)) {
                template.set(key, userValues.get(key));
            }
        }

        try {
            template.save(diskFile);
            BlockProtLogger.log("config-clean", "config.yml rewritten with clean format; user values preserved.");
        } catch (IOException e) {
            BlockProtLogger.warn("Failed to rewrite config.yml: " + e.getMessage());
        }
    }

    /** Keys managed by separate files — never merged back into config.yml. */
    private static final Set<String> EXTERNAL_CONFIG_KEYS = Set.of(
        "lockable_tile_entities", "lockable_shulker_boxes", "lockable_blocks", "lockable_doors",
        "mysql.enabled", "mysql.host", "mysql.port", "mysql.database",
        "mysql.username", "mysql.password", "mysql.jdbc_url",
        "mysql.pool.maximum_pool_size", "mysql.pool.minimum_idle", "mysql.pool.connection_timeout_ms",
        "console.prefix_color", "console.info_color"
    );

    private void mergeMissingConfigKeys() {
        File diskFile = new File(this.getDataFolder(), "config.yml");
        if (!diskFile.exists()) return;
        InputStream jarStream = this.getResource("config.yml");
        if (jarStream == null) return;

        YamlConfiguration jarConfig  = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(jarStream)));
        YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);
        int added = 0;
        for (String key : jarConfig.getKeys(true)) {
            if (jarConfig.isConfigurationSection(key)) continue;
            if (EXTERNAL_CONFIG_KEYS.contains(key)) continue;
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, jarConfig.get(key));
                added++;
                BlockProtLogger.log("config-merge", "config.yml — added missing key: " + key);
            }
        }

        if (added == 0) return;
        try {
            diskConfig.save(diskFile);
            BlockProtConsole.info(Translator.get(TranslationKey.CONSOLE__CONFIG_KEYS_UPDATED)
                .replace("{count}", String.valueOf(added)));
            BlockProtLogger.log("config-merge", "config.yml merge complete — added " + added + " missing option(s).");
        } catch (IOException e) {
            BlockProtConsole.warn(Translator.get(TranslationKey.CONSOLE__CONFIG_KEYS_SAVE_FAILED)
                .replace("{error}", e.getMessage()));
            BlockProtLogger.log("config-merge", "Failed to save config.yml after merge: " + e.getMessage());
        }
    }

    /**
     * Saves a resource silently — does not log a warning when the file already exists.
     */
    private void saveResourceSilent(@NotNull String name, boolean replace) {
        File dest = new File(this.getDataFolder(), name);
        if (!replace && dest.exists()) return; // already there, skip quietly
        try {
            this.saveResource(name, replace);
        } catch (Exception ignored) {}
    }

    private boolean isRunningCraftBukkit() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
