package com.seventodie.utils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import com.seventodie.SevenToDiePlugin;

/**
 * Manages database operations for the plugin.
 */
public class DatabaseManager {
    
    private final SevenToDiePlugin plugin;
    private Connection connection;
    
    // Database file
    private File databaseFile;
    
    // Database tables
    private static final String TABLE_STRUCTURES = "structures";
    private static final String TABLE_QUESTS = "quests";
    private static final String TABLE_TRADERS = "traders";
    private static final String TABLE_PLAYER_QUESTS = "player_quests";
    private static final String TABLE_FRAME_BLOCKS = "frame_blocks";
    
    public DatabaseManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the database
     */
    public void initialize() {
        // Create database directory if it doesn't exist
        File databaseDir = new File(plugin.getDataFolder(), "database");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }
        
        // Set up database file
        databaseFile = new File(databaseDir, "seventodie.db");
        
        // Initialize database connection
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            // Create tables if they don't exist
            createTables();
            
            plugin.getLogger().info("Database initialized successfully");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found!");
            plugin.getLogger().warning("The plugin will continue without database support.");
        } catch (UnsatisfiedLinkError e) {
            plugin.getLogger().severe("Failed to load SQLite native library: " + e.getMessage());
            plugin.getLogger().warning("The plugin will continue in memory-only mode without persistence.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            plugin.getLogger().warning("The plugin will continue without database support.");
        }
    }
    
    /**
     * Create database tables if they don't exist
     * 
     * @throws SQLException If there is a database error
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Structures table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE_STRUCTURES + " (" +
                "id TEXT PRIMARY KEY, " +
                "type TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "size_x DOUBLE NOT NULL, " +
                "size_y DOUBLE NOT NULL, " +
                "size_z DOUBLE NOT NULL, " +
                "schematic TEXT NOT NULL, " +
                "rotation INTEGER NOT NULL" +
                ")"
            );
            
            // Quests table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE_QUESTS + " (" +
                "id TEXT PRIMARY KEY, " +
                "title TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "target_type TEXT NOT NULL, " +
                "target_amount INTEGER NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "structure_id TEXT, " +
                "completed BOOLEAN NOT NULL DEFAULT 0" +
                ")"
            );
            
            // Traders table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE_TRADERS + " (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "structure_id TEXT, " +
                "FOREIGN KEY (structure_id) REFERENCES " + TABLE_STRUCTURES + "(id)" +
                ")"
            );
            
            // Player quests table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYER_QUESTS + " (" +
                "player_id TEXT NOT NULL, " +
                "quest_id TEXT NOT NULL, " +
                "progress INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (player_id, quest_id), " +
                "FOREIGN KEY (quest_id) REFERENCES " + TABLE_QUESTS + "(id)" +
                ")"
            );
            
            // Frame blocks table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS " + TABLE_FRAME_BLOCKS + " (" +
                "world TEXT NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "tier INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (world, x, y, z)" +
                ")"
            );
        }
    }
    
    /**
     * Shut down the database connection
     */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    /**
     * Check if the database connection is available
     * 
     * @return true if the connection is available, false otherwise
     */
    public boolean isConnectionAvailable() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Save a structure to the database
     * 
     * @param id The structure ID
     * @param type The structure type
     * @param location The structure location
     * @param sizeX The structure X size
     * @param sizeY The structure Y size
     * @param sizeZ The structure Z size
     * @param schematic The schematic name
     * @param rotation The rotation
     * @return True if the operation was successful
     */
    public boolean saveStructure(UUID id, String type, Location location, 
                               double sizeX, double sizeY, double sizeZ, 
                               String schematic, int rotation) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Structure could not be saved.");
            return false;
        }
                               
        String sql = "INSERT OR REPLACE INTO " + TABLE_STRUCTURES + 
                     " (id, type, world, x, y, z, size_x, size_y, size_z, schematic, rotation) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.setString(2, type);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setDouble(7, sizeX);
            stmt.setDouble(8, sizeY);
            stmt.setDouble(9, sizeZ);
            stmt.setString(10, schematic);
            stmt.setInt(11, rotation);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving structure", e);
            return false;
        }
    }
    
    /**
     * Save a quest to the database
     * 
     * @param id The quest ID
     * @param title The quest title
     * @param description The quest description
     * @param targetType The quest target type
     * @param targetAmount The target amount
     * @param location The quest location
     * @param structureId The structure ID, or null
     * @param completed Whether the quest is completed
     * @return True if the operation was successful
     */
    public boolean saveQuest(UUID id, String title, String description, 
                           String targetType, int targetAmount, Location location, 
                           UUID structureId, boolean completed) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Quest could not be saved.");
            return false;
        }
                           
        String sql = "INSERT OR REPLACE INTO " + TABLE_QUESTS + 
                     " (id, title, description, target_type, target_amount, " +
                     "world, x, y, z, structure_id, completed) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setString(4, targetType);
            stmt.setInt(5, targetAmount);
            stmt.setString(6, location.getWorld().getName());
            stmt.setDouble(7, location.getX());
            stmt.setDouble(8, location.getY());
            stmt.setDouble(9, location.getZ());
            stmt.setString(10, structureId != null ? structureId.toString() : null);
            stmt.setBoolean(11, completed);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving quest", e);
            return false;
        }
    }
    
    /**
     * Save a trader to the database
     * 
     * @param id The trader ID
     * @param name The trader name
     * @param location The trader location
     * @param structureId The structure ID, or null
     * @return True if the operation was successful
     */
    public boolean saveTrader(UUID id, String name, Location location, UUID structureId) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Trader could not be saved.");
            return false;
        }
        
        String sql = "INSERT OR REPLACE INTO " + TABLE_TRADERS + 
                     " (id, name, world, x, y, z, structure_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.setString(2, name);
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setString(7, structureId != null ? structureId.toString() : null);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving trader", e);
            return false;
        }
    }
    
    /**
     * Save player quest progress
     * 
     * @param playerId The player ID
     * @param questId The quest ID
     * @param progress The progress value
     * @return True if the operation was successful
     */
    public boolean savePlayerQuestProgress(UUID playerId, UUID questId, int progress) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Quest progress could not be saved.");
            return false;
        }
        
        String sql = "INSERT OR REPLACE INTO " + TABLE_PLAYER_QUESTS + 
                     " (player_id, quest_id, progress) " +
                     "VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, questId.toString());
            stmt.setInt(3, progress);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player quest progress", e);
            return false;
        }
    }
    
    /**
     * Save a frame block
     * 
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @param tier The block tier
     * @return True if the operation was successful
     */
    public boolean saveFrameBlock(String world, int x, int y, int z, int tier) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Frame block could not be saved.");
            return false;
        }
        
        String sql = "INSERT OR REPLACE INTO " + TABLE_FRAME_BLOCKS + 
                     " (world, x, y, z, tier) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.setInt(5, tier);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving frame block", e);
            return false;
        }
    }
    
    /**
     * Delete a frame block
     * 
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return True if the operation was successful
     */
    public boolean deleteFrameBlock(String world, int x, int y, int z) {
        if (!isConnectionAvailable()) {
            plugin.getLogger().warning("Database connection not available. Frame block could not be deleted.");
            return false;
        }
        
        String sql = "DELETE FROM " + TABLE_FRAME_BLOCKS + 
                     " WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting frame block", e);
            return false;
        }
    }
    
    /**
     * Get the tier of a frame block
     * 
     * @param world The world name
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return The block tier, or -1 if not found
     */
    public int getFrameBlockTier(String world, int x, int y, int z) {
        String sql = "SELECT tier FROM " + TABLE_FRAME_BLOCKS + 
                     " WHERE world = ? AND x = ? AND y = ? AND z = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tier");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting frame block tier", e);
        }
        
        return -1;
    }
    
    /**
     * Check if a quest is completed
     * 
     * @param questId The quest ID
     * @return True if the quest is completed
     */
    public boolean isQuestCompleted(UUID questId) {
        String sql = "SELECT completed FROM " + TABLE_QUESTS + " WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, questId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("completed");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking quest completion", e);
        }
        
        return false;
    }
    
    /**
     * Get player quest progress
     * 
     * @param playerId The player ID
     * @param questId The quest ID
     * @return The progress value, or 0 if not found
     */
    public int getPlayerQuestProgress(UUID playerId, UUID questId) {
        String sql = "SELECT progress FROM " + TABLE_PLAYER_QUESTS + 
                     " WHERE player_id = ? AND quest_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, questId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("progress");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player quest progress", e);
        }
        
        return 0;
    }
    
    /**
     * Get a structure by ID
     * 
     * @param structureId The structure ID
     * @return An array of [type, world, x, y, z, sizeX, sizeY, sizeZ, schematic, rotation] or null if not found
     */
    public Object[] getStructure(UUID structureId) {
        String sql = "SELECT type, world, x, y, z, size_x, size_y, size_z, schematic, rotation " +
                     "FROM " + TABLE_STRUCTURES + " WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, structureId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[] {
                        rs.getString("type"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getDouble("size_x"),
                        rs.getDouble("size_y"),
                        rs.getDouble("size_z"),
                        rs.getString("schematic"),
                        rs.getInt("rotation")
                    };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting structure", e);
        }
        
        return null;
    }
    
    /**
     * Get a quest by ID
     * 
     * @param questId The quest ID
     * @return An array of [title, description, targetType, targetAmount, world, x, y, z, structureId, completed] or null if not found
     */
    public Object[] getQuest(UUID questId) {
        String sql = "SELECT title, description, target_type, target_amount, world, x, y, z, structure_id, completed " +
                     "FROM " + TABLE_QUESTS + " WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, questId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[] {
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("target_type"),
                        rs.getInt("target_amount"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getString("structure_id"),
                        rs.getBoolean("completed")
                    };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting quest", e);
        }
        
        return null;
    }
    
    /**
     * Get the database connection
     * 
     * @return The database connection
     */
    public Connection getConnection() {
        return connection;
    }
}
