package com.seventodie.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
    private boolean inMemoryMode = false;
    
    // Database file
    private File databaseFile;
    
    // Database tables
    private static final String TABLE_STRUCTURES = "structures";
    private static final String TABLE_QUESTS = "quests";
    private static final String TABLE_TRADERS = "traders";
    private static final String TABLE_PLAYER_QUESTS = "player_quests";
    private static final String TABLE_FRAME_BLOCKS = "frame_blocks";
    
    // Native library names
    private static final String[] NATIVE_LIBRARIES = {
        "sqlite-native-win-x64.dll",
        "sqlite-native-win-x86.dll",
        "libsqlite-native-mac.dylib",
        "libsqlite-native-linux-x64.so",
        "libsqlite-native-linux-x86.so",
        "libsqlite-native-linux-arm.so",
        "libsqlite-native-linux-aarch64.so"
    };
    
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
        
        // Try multiple approaches for SQLite connectivity
        if (!initializeWithRelocatedSQLite() && !initializeWithOriginalSQLite()) {
            initializeWithFallbackMode();
        }
    }
    
    /**
     * Attempts to initialize with relocated SQLite library
     * 
     * @return true if successful, false otherwise
     */
    private boolean initializeWithRelocatedSQLite() {
        try {
            // Try with the relocated package first (after Maven Shade plugin relocation)
            Class.forName("com.seventodie.lib.sqlite.JDBC");
            plugin.getLogger().info("Using relocated SQLite JDBC driver (com.seventodie.lib.sqlite.JDBC)");
            
            // Extract and configure native libraries
            extractNativeLibraries("com.seventodie.lib.sqlite");
            
            // Try connecting to the database file
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            // Verify connection works with a simple query
            if (verifyConnection()) {
                // Create tables if they don't exist
                createTables();
                plugin.getLogger().info("Database initialized successfully with relocated SQLite");
                return true;
            } else {
                plugin.getLogger().severe("Database connection verification failed with relocated SQLite.");
                return false;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Relocated SQLite driver not found: " + e.getMessage());
            return false;
        } catch (UnsatisfiedLinkError e) {
            plugin.getLogger().severe("Failed to load relocated SQLite native library: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database with relocated SQLite", e);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during relocated SQLite initialization", e);
            return false;
        }
    }
    
    /**
     * Attempts to initialize with original SQLite library
     * 
     * @return true if successful, false otherwise
     */
    private boolean initializeWithOriginalSQLite() {
        try {
            // Try with the original package
            Class.forName("org.sqlite.JDBC");
            plugin.getLogger().info("Using original SQLite JDBC driver (org.sqlite.JDBC)");
            
            // Extract and configure native libraries
            extractNativeLibraries("org.sqlite");
            
            // Try connecting to the database file
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            // Verify connection works with a simple query
            if (verifyConnection()) {
                // Create tables if they don't exist
                createTables();
                plugin.getLogger().info("Database initialized successfully with original SQLite");
                return true;
            } else {
                plugin.getLogger().severe("Database connection verification failed with original SQLite.");
                return false;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Original SQLite driver not found: " + e.getMessage());
            return false;
        } catch (UnsatisfiedLinkError e) {
            plugin.getLogger().severe("Failed to load original SQLite native library: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database with original SQLite", e);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during original SQLite initialization", e);
            return false;
        }
    }
    
    /**
     * Initialize with in-memory fallback mode (no persistence)
     */
    private boolean initializeWithFallbackMode() {
        try {
            plugin.getLogger().warning("Initializing database in memory-only mode. Data will not be persisted!");
            inMemoryMode = true;
            
            // Try to initialize with in-memory SQLite database
            try {
                // Try the relocated package first
                Class.forName("com.seventodie.lib.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            } catch (ClassNotFoundException e) {
                // Try the original package
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            }
            
            // Verify connection works with a simple query
            if (verifyConnection()) {
                // Create tables if they don't exist
                createTables();
                plugin.getLogger().info("In-memory database initialized successfully");
                return true;
            } else {
                plugin.getLogger().severe("In-memory database connection verification failed");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize in-memory database", e);
            return false;
        }
    }
    
    /**
     * Extract SQLite native libraries to plugin data folder
     */
    private void extractNativeLibraries(String packageName) {
        try {
            // Get OS name and architecture
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            
            // Determine library filename based on OS
            String libFilename = null;
            if (osName.contains("win")) {
                libFilename = osArch.contains("64") ? "sqlite-native-win-x64.dll" : "sqlite-native-win-x86.dll";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                libFilename = "libsqlite-native-mac.dylib";
            } else if (osName.contains("linux") || osName.contains("unix")) {
                if (osArch.contains("arm") || osArch.contains("aarch")) {
                    libFilename = osArch.contains("64") ? "libsqlite-native-linux-aarch64.so" : "libsqlite-native-linux-arm.so";
                } else {
                    libFilename = osArch.contains("64") ? "libsqlite-native-linux-x64.so" : "libsqlite-native-linux-x86.so";
                }
            }
            
            if (libFilename == null) {
                plugin.getLogger().warning("Unsupported OS/architecture for SQLite native library: " + osName + " " + osArch);
                return;
            }
            
            // Create libs directory
            File libsDir = new File(plugin.getDataFolder(), "libs");
            if (!libsDir.exists()) {
                libsDir.mkdirs();
            }
            
            // Attempt to extract all libraries
            extractResourcesFromJar("native", libsDir);
            
            // Set system properties for SQLite JDBC
            System.setProperty(packageName + ".lib.path", libsDir.getAbsolutePath());
            System.setProperty(packageName + ".lib.name", libFilename);
            System.setProperty(packageName + ".lib.version", "3.43.0");
            
            plugin.getLogger().info("Set SQLite native library path to: " + libsDir.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract SQLite native libraries", e);
        }
    }
    
    /**
     * Extract resources from the plugin JAR
     */
    private void extractResourcesFromJar(String folderName, File destDir) {
        try {
            URL jarUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl == null) {
                plugin.getLogger().warning("Failed to get plugin JAR location");
                return;
            }
            
            // Try to extract resources from the current JAR file
            try (JarFile jar = new JarFile(new File(jarUrl.toURI()))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    // Check if it's a native library
                    if (name.startsWith(folderName + "/") && !entry.isDirectory()) {
                        String fileName = name.substring(name.lastIndexOf('/') + 1);
                        File outFile = new File(destDir, fileName);
                        
                        // Extract the file
                        if (!outFile.exists() || outFile.length() != entry.getSize()) {
                            try (InputStream is = jar.getInputStream(entry);
                                 OutputStream os = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                            }
                            outFile.setExecutable(true);
                            plugin.getLogger().info("Extracted native library: " + fileName);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to extract from JAR", e);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract resources from JAR", e);
        }
    }
    
    /**
     * Verify connection works with a simple query
     * 
     * @return true if connection verification succeeded
     */
    private boolean verifyConnection() {
        if (connection == null) {
            return false;
        }
        
        try {
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Connection verification failed", e);
            return false;
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
            return connection != null && !connection.isClosed() && verifyConnection();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Check if the database is running in memory-only mode 
     * 
     * @return true if in memory-only mode, false if using persistent storage
     */
    public boolean isInMemoryMode() {
        return inMemoryMode;
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
    
    // Additional database methods would go here...
}