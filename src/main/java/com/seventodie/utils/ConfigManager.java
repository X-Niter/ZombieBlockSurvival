package com.seventodie.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.seventodie.SevenToDiePlugin;

/**
 * Manages plugin configuration files and settings.
 */
public class ConfigManager {
    
    private final SevenToDiePlugin plugin;
    
    // Config cache
    private FileConfiguration mainConfig;
    private final Map<String, FileConfiguration> configCache = new HashMap<>();
    
    // Default configuration values
    private static final Map<String, Object> DEFAULT_CONFIG = new HashMap<>();
    
    static {
        // Road generation settings
        DEFAULT_CONFIG.put("world-gen.roads.main-road-spacing", 512);
        DEFAULT_CONFIG.put("world-gen.roads.secondary-road-spacing", 128);
        DEFAULT_CONFIG.put("world-gen.roads.main-road-width", 5);
        DEFAULT_CONFIG.put("world-gen.roads.secondary-road-width", 3);
        
        // Structure generation settings
        DEFAULT_CONFIG.put("world-gen.structures.trader-spacing", 300);
        DEFAULT_CONFIG.put("world-gen.structures.residential-chance", 0.5);
        DEFAULT_CONFIG.put("world-gen.structures.commercial-chance", 0.3);
        DEFAULT_CONFIG.put("world-gen.structures.industrial-chance", 0.2);
        
        // Trader settings
        DEFAULT_CONFIG.put("traders.day-only-entry", true);
        DEFAULT_CONFIG.put("traders.play-voice-lines", true);
        DEFAULT_CONFIG.put("traders.ejection-distance", 15);
        
        // Quest settings
        DEFAULT_CONFIG.put("quests.marker-enabled", true);
        DEFAULT_CONFIG.put("quests.max-active-per-player", 5);
        DEFAULT_CONFIG.put("quests.reset-structures-on-completion", true);
        
        // Frame block settings
        DEFAULT_CONFIG.put("blocks.frames.enabled", true);
        DEFAULT_CONFIG.put("blocks.frames.upgrade-sound", "BLOCK_ANVIL_USE");
        
        // Tool settings
        DEFAULT_CONFIG.put("tools.hammers.enabled", true);
        DEFAULT_CONFIG.put("tools.hammers.stone-max-tier", 1);
        DEFAULT_CONFIG.put("tools.hammers.iron-max-tier", 3);
        DEFAULT_CONFIG.put("tools.hammers.steel-max-tier", 4);
        
        // Performance settings
        DEFAULT_CONFIG.put("performance.async-schematic-loading", true);
        DEFAULT_CONFIG.put("performance.block-update-throttle", 1000);
    }
    
    public ConfigManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        
        // Load main config
        loadMainConfig();
        
        // Create directories
        createDirectories();
    }
    
    /**
     * Load the main plugin configuration
     */
    private void loadMainConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Load config
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        
        // Add any missing default values
        boolean needsSave = false;
        
        for (Map.Entry<String, Object> entry : DEFAULT_CONFIG.entrySet()) {
            if (!mainConfig.contains(entry.getKey())) {
                mainConfig.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }
        
        // Save if changes were made
        if (needsSave) {
            plugin.saveConfig();
        }
    }
    
    /**
     * Create required directories
     */
    private void createDirectories() {
        File dataFolder = plugin.getDataFolder();
        
        // Create main data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Create subdirectories
        new File(dataFolder, "schematics").mkdirs();
        new File(dataFolder, "schematics/residential").mkdirs();
        new File(dataFolder, "schematics/commercial").mkdirs();
        new File(dataFolder, "schematics/industrial").mkdirs();
        new File(dataFolder, "schematics/trader").mkdirs();
        
        new File(dataFolder, "quests").mkdirs();
        new File(dataFolder, "structures").mkdirs();
        new File(dataFolder, "traders").mkdirs();
        new File(dataFolder, "sounds").mkdirs();
    }
    
    /**
     * Get the main configuration
     * 
     * @return The main configuration
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Reload the main configuration
     */
    public void reloadMainConfig() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
    }
    
    /**
     * Save the main configuration
     */
    public void saveMainConfig() {
        plugin.saveConfig();
    }
    
    /**
     * Load a custom configuration file
     * 
     * @param fileName The file name (without .yml extension)
     * @return The configuration file
     */
    public FileConfiguration getConfig(String fileName) {
        if (configCache.containsKey(fileName)) {
            return configCache.get(fileName);
        }
        
        File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!configFile.exists()) {
            // Try to save default from resources
            plugin.saveResource(fileName + ".yml", false);
        }
        
        // If file still doesn't exist, create an empty one
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create config file: " + fileName + ".yml");
                return new YamlConfiguration();
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        configCache.put(fileName, config);
        
        return config;
    }
    
    /**
     * Save a custom configuration file
     * 
     * @param fileName The file name (without .yml extension)
     * @param config The configuration to save
     */
    public void saveConfig(String fileName, FileConfiguration config) {
        try {
            File configFile = new File(plugin.getDataFolder(), fileName + ".yml");
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config file: " + fileName + ".yml");
            plugin.getLogger().severe(e.getMessage());
        }
    }
    
    /**
     * Get a configuration section, creating it if it doesn't exist
     * 
     * @param config The configuration
     * @param path The section path
     * @return The configuration section
     */
    public ConfigurationSection getOrCreateSection(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            section = config.createSection(path);
        }
        return section;
    }
    
    /**
     * Gets a string from the config, with a default value
     * 
     * @param path The config path
     * @param defaultValue The default value
     * @return The config value
     */
    public String getString(String path, String defaultValue) {
        return mainConfig.getString(path, defaultValue);
    }
    
    /**
     * Gets an integer from the config, with a default value
     * 
     * @param path The config path
     * @param defaultValue The default value
     * @return The config value
     */
    public int getInt(String path, int defaultValue) {
        return mainConfig.getInt(path, defaultValue);
    }
    
    /**
     * Gets a boolean from the config, with a default value
     * 
     * @param path The config path
     * @param defaultValue The default value
     * @return The config value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return mainConfig.getBoolean(path, defaultValue);
    }
    
    /**
     * Gets a double from the config, with a default value
     * 
     * @param path The config path
     * @param defaultValue The default value
     * @return The config value
     */
    public double getDouble(String path, double defaultValue) {
        return mainConfig.getDouble(path, defaultValue);
    }
    
    /**
     * Gets a list of strings from the config
     * 
     * @param path The config path
     * @return The list of strings, or empty list if not found
     */
    public List<String> getStringList(String path) {
        return mainConfig.getStringList(path);
    }
    
    /**
     * Sets a value in the main config
     * 
     * @param path The config path
     * @param value The value to set
     */
    public void set(String path, Object value) {
        mainConfig.set(path, value);
    }
    
    /**
     * Checks if a path exists in the main config
     * 
     * @param path The config path
     * @return True if the path exists
     */
    public boolean contains(String path) {
        return mainConfig.contains(path);
    }
}
