package com.seventodie;

import com.seventodie.blocks.BlockManager;
import com.seventodie.tools.ToolManager;
import com.seventodie.traders.TraderManager;
import com.seventodie.worldgen.RoadGenerator;
import com.seventodie.worldgen.StructureManager;
import com.seventodie.worldgen.BiomeMapper;
import com.seventodie.quests.QuestManager;
import com.seventodie.utils.ConfigManager;
import com.seventodie.utils.DatabaseManager;
import com.seventodie.listeners.BlockInteractionListener;
import com.seventodie.listeners.WorldGenListener;
import com.seventodie.listeners.PlayerListener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Main plugin class for the 7 Days to Die Minecraft implementation.
 * This plugin recreates the core gameplay mechanics of 7 Days to Die in Minecraft.
 */
public class SevenToDiePlugin extends JavaPlugin {
    
    private static SevenToDiePlugin instance;
    
    // Managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private BlockManager blockManager;
    private ToolManager toolManager;
    private RoadGenerator roadGenerator;
    private StructureManager structureManager;
    private BiomeMapper biomeMapper;
    private TraderManager traderManager;
    private QuestManager questManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Initialize managers
        blockManager = new BlockManager(this);
        toolManager = new ToolManager(this);
        roadGenerator = new RoadGenerator(this);
        structureManager = new StructureManager(this);
        biomeMapper = new BiomeMapper(this);
        traderManager = new TraderManager(this);
        questManager = new QuestManager(this);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldGenListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Check for required dependencies
        if (!checkDependencies()) {
            getLogger().severe("Required dependencies are missing! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup day/night cycle handler for traders
        setupDayNightCycle();
        
        getLogger().info("7 Days to Die Plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        getLogger().info("7 Days to Die Plugin has been disabled!");
    }
    
    /**
     * Checks if all required dependencies are present.
     */
    private boolean checkDependencies() {
        boolean allPresent = true;
        
        // Check for WorldEdit
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().warning("WorldEdit not found! Structure generation may not work properly.");
            allPresent = false;
        }
        
        // Check for Citizens
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            getLogger().warning("Citizens not found! Trader NPCs will not function properly.");
            allPresent = false;
        }
        
        // Check for ProtocolLib
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib not found! Custom block handling will be less optimized.");
            allPresent = false;
        }
        
        return allPresent;
    }
    
    /**
     * Sets up the day/night cycle scheduler for trader operations.
     */
    private void setupDayNightCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                traderManager.handleDayNightCycle();
            }
        }.runTaskTimer(this, 0L, 100L); // Check every 5 seconds (100 ticks)
    }
    
    /**
     * Returns the plugin instance.
     */
    public static SevenToDiePlugin getInstance() {
        return instance;
    }
    
    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public ToolManager getToolManager() {
        return toolManager;
    }
    
    public RoadGenerator getRoadGenerator() {
        return roadGenerator;
    }
    
    public StructureManager getStructureManager() {
        return structureManager;
    }
    
    public BiomeMapper getBiomeMapper() {
        return biomeMapper;
    }
    
    public TraderManager getTraderManager() {
        return traderManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
}
