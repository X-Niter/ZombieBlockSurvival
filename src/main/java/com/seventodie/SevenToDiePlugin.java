package com.seventodie;

import com.seventodie.blocks.BlockManager;
import com.seventodie.commands.CommandManager;
import com.seventodie.listeners.BlockInteractionListener;
import com.seventodie.listeners.PlayerListener;
import com.seventodie.listeners.WorldGenListener;
import com.seventodie.quests.QuestManager;
import com.seventodie.tools.ToolManager;
import com.seventodie.traders.TraderManager;
import com.seventodie.utils.ConfigManager;
import com.seventodie.utils.DatabaseManager;
import com.seventodie.utils.SchematicUtils;
import com.seventodie.worldgen.BiomeMapper;
import com.seventodie.worldgen.RoadGenerator;
import com.seventodie.worldgen.StructureManager;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for the SevenToDie plugin.
 */
public class SevenToDiePlugin extends JavaPlugin {
  
  private ConfigManager configManager;
  private DatabaseManager databaseManager;
  private BlockManager blockManager;
  private ToolManager toolManager;
  private StructureManager structureManager;
  private RoadGenerator roadGenerator;
  private BiomeMapper biomeMapper;
  private TraderManager traderManager;
  private QuestManager questManager;
  private SchematicUtils schematicUtils;
  private CommandManager commandManager;
  
  @Override
  public void onEnable() {
    // Save default config
    saveDefaultConfig();
    
    // Initialize managers
    initializeManagers();
    
    // Register event listeners
    registerEventListeners();
    
    // Register commands
    commandManager = new CommandManager(this);
    
    // Log startup
    getLogger().info("SevenToDie plugin has been enabled!");
    getLogger().info("Running on Minecraft " + getServer().getBukkitVersion());
    
    // Check for optional dependencies
    checkDependencies();
  }
  
  @Override
  public void onDisable() {
    // Save data
    if (databaseManager != null) {
      databaseManager.shutdown();
    }
    
    // Cleanup
    if (traderManager != null) {
      traderManager.cleanup();
    }
    
    getLogger().info("SevenToDie plugin has been disabled!");
  }
  
  /**
   * Initialize all managers.
   */
  private void initializeManagers() {
    // Config manager first
    configManager = new ConfigManager(this);
    configManager.reloadMainConfig();
    
    // Database
    databaseManager = new DatabaseManager(this);
    if (databaseManager.initialize()) {
      getLogger().info("Database initialized successfully");
    } else {
      // Database initialization failed but we can continue with memory-only mode
      getLogger().warning("Database initialization failed.");
      getLogger().warning("Plugin will run with limited functionality.");
      getLogger().warning("Data will not be persisted between server restarts.");
    }
    
    // Schematics
    schematicUtils = new SchematicUtils(this);
    
    // World generation
    biomeMapper = new BiomeMapper(this);
    structureManager = new StructureManager(this, schematicUtils);
    roadGenerator = new RoadGenerator(this);
    
    // Game mechanics
    blockManager = new BlockManager(this);
    toolManager = new ToolManager(this);
    
    // Game systems
    traderManager = new TraderManager(this);
    questManager = new QuestManager(this);
  }
  
  /**
   * Register event listeners.
   */
  private void registerEventListeners() {
    getServer().getPluginManager().registerEvents(new BlockInteractionListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    getServer().getPluginManager().registerEvents(new WorldGenListener(this), this);
  }
  
  /**
   * Check for optional dependencies.
   */
  private void checkDependencies() {
    boolean worldEditPresent = getServer().getPluginManager().getPlugin("WorldEdit") != null;
    boolean citizensPresent = getServer().getPluginManager().getPlugin("Citizens") != null;
    boolean protocolLibPresent = getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    
    if (!worldEditPresent) {
      getLogger().warning("WorldEdit is not installed. "
          + "Advanced structure manipulation will be limited.");
    }
    
    if (!citizensPresent) {
      getLogger().warning("Citizens is not installed. Traders will use vanilla entities instead.");
    }
    
    if (!protocolLibPresent) {
      getLogger().warning("ProtocolLib is not installed. "
          + "Some custom rendering features will be disabled.");
    }
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
  
  public StructureManager getStructureManager() {
    return structureManager;
  }
  
  public RoadGenerator getRoadGenerator() {
    return roadGenerator;
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
  
  public SchematicUtils getSchematicUtils() {
    return schematicUtils;
  }
}