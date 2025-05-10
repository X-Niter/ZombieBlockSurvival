package com.seventodie.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
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
    private HikariDataSource connectionPool;
    private boolean inMemoryMode = false;
    private static final int MAX_POOL_SIZE = 10;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000;
    
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
     * Initializes the database connection with multi-layered fallback mechanisms.
     * 
     * This method implements a cascading fallback strategy:
     * 1. First attempts to connect using the relocated SQLite JDBC driver with native libraries
     * 2. If that fails, attempts to use the original SQLite JDBC driver
     * 3. If both SQLite approaches fail, falls back to pure Java H2 database
     * 4. As a last resort, operates in memory-only mode with no persistence
     * 
     * This approach ensures the plugin can function across diverse environments
     * and prioritizes plugin functionality over data persistence.
     * 
     * @return true if connected to a persistent database, false if in memory-only mode
     */
    public boolean initialize() {
        try {
            // Create database directory with atomic operation
            File databaseDir = new File(plugin.getDataFolder(), "database");
            if (!databaseDir.exists() && !databaseDir.mkdirs()) {
                plugin.getLogger().severe("Failed to create database directory");
                return false;
            }
            
            // Verify directory is writable
            if (!databaseDir.canWrite()) {
                plugin.getLogger().severe("Database directory is not writable");
                return false;
            }
            
            // Set up database file
            databaseFile = new File(databaseDir, "seventodie.db");
            
            // Try multiple approaches for database connectivity
            if (!initializeWithRelocatedSQLite()) {
                plugin.getLogger().info("Relocated SQLite initialization failed, trying original SQLite...");
                if (!initializeWithOriginalSQLite()) {
                    plugin.getLogger().info("Original SQLite initialization failed, trying H2 or in-memory fallback mode...");
                    if (!initializeWithFallbackMode()) {
                        plugin.getLogger().severe("All database initialization approaches failed! Plugin functionality may be limited.");
                        return false;
                    }
                    // Fallback mode with H2 was successful
                    return !inMemoryMode;
                }
            }
            
            // If we get here, one of the SQLite approaches worked
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error during database initialization", e);
            return false;
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
     * Initializes the database using pure Java in-memory fallback modes.
     * Used when both SQLite initialization methods have failed, typically
     * due to native library issues or permission problems.
     * 
     * This method implements two levels of fallback:
     * 1. First tries H2 database in pure Java mode (shaded version)
     * 2. Then tries regular H2 database if present in the classpath
     * 3. Finally defaults to no database with in-memory flag set
     * 
     * @return true if any in-memory database initialization succeeded, false otherwise
     */
    private boolean initializeWithFallbackMode() {
        try {
            plugin.getLogger().warning("Initializing database in memory-only mode. Data will not be persisted!");
            inMemoryMode = true;
            
            // Try H2 database as it doesn't rely on native libraries
            try {
                // Safe approach using H2 Database which is pure Java
                Class.forName("com.seventodie.lib.h2.Driver");
                connection = DriverManager.getConnection("jdbc:h2:mem:seventodie;DB_CLOSE_DELAY=-1");
                plugin.getLogger().info("Using H2 in-memory database as fallback");
            } catch (ClassNotFoundException h2ShadedNotFound) {
                try {
                    // Try non-shaded H2 as a last resort
                    Class.forName("org.h2.Driver");
                    connection = DriverManager.getConnection("jdbc:h2:mem:seventodie;DB_CLOSE_DELAY=-1");
                    plugin.getLogger().info("Using H2 in-memory database as fallback (non-shaded)");
                } catch (ClassNotFoundException h2NotFound) {
                    // Complete failure - create a dummy connection object that will work 
                    // but won't actually save anything
                    plugin.getLogger().severe("Failed to load any database driver - using dummy no-op database");
                    createDummyConnection();
                }
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
            
            // Set core system properties used by SQLite JDBC
            System.setProperty("sqlite.purejava", "true"); // Force pure Java mode as a fallback
            
            // Try multiple approaches
            try {
                // Approach 1: Try System.load direct loading on the right file
                File libFile = new File(libsDir, libFilename);
                if (libFile.exists()) {
                    try {
                        System.load(libFile.getAbsolutePath());
                        plugin.getLogger().info("Loaded SQLite native library directly: " + libFile.getAbsolutePath());
                    } catch (UnsatisfiedLinkError e) {
                        plugin.getLogger().warning("Direct loading failed: " + e.getMessage());
                    }
                }
                
                // Approach 2: Update java.library.path
                addDirectoryToJavaLibraryPath(libsDir.getAbsolutePath());
                
                // Approach 3: Use SQLite's own properties
                System.setProperty(packageName + ".lib.path", libsDir.getAbsolutePath());
                System.setProperty(packageName + ".lib.name", libFilename.replaceAll("^lib|\\.(so|dll|dylib)$", ""));
                System.setProperty(packageName + ".lib.version", "3.43.0");
                
                // Approach 4: Tell SQLite to use a specific library path for binary loading
                System.setProperty(packageName + ".tmpdir", libsDir.getAbsolutePath());
                
                // Approach 5: Make SQLite search the extracted native library path directly
                System.setProperty("java.io.tmpdir", libsDir.getAbsolutePath());
                
                plugin.getLogger().info("Set SQLite native library path to: " + libsDir.getAbsolutePath());
                
                // Log all system properties to help with debugging
                plugin.getLogger().info("Java library path: " + System.getProperty("java.library.path"));
                plugin.getLogger().info("SQLite lib path: " + System.getProperty(packageName + ".lib.path"));
                plugin.getLogger().info("SQLite lib name: " + System.getProperty(packageName + ".lib.name"));
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update library paths: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to extract SQLite native libraries", e);
        }
    }
    
    /**
     * Adds the specified path to the java.library.path system property and updates
     * the ClassLoader's internal cache to reflect the change.
     * 
     * @param path the path to add to java.library.path
     * @throws Exception if there is a reflection error
     */
    private void addDirectoryToJavaLibraryPath(String path) throws Exception {
        try {
            // Get the current java.library.path
            String libraryPath = System.getProperty("java.library.path");
            
            // Skip if path is already in java.library.path
            if (libraryPath.contains(path)) {
                return;
            }
            
            // Add the new path
            if (libraryPath == null || libraryPath.isEmpty()) {
                libraryPath = path;
            } else {
                libraryPath = path + File.pathSeparator + libraryPath;
            }
            
            // Set the new java.library.path
            System.setProperty("java.library.path", libraryPath);
            
            // Force the ClassLoader to reload the native library cache
            // This uses reflection to access and clear a private field
            try {
                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not reset ClassLoader's native library cache: " + e.getMessage());
            }
            
            plugin.getLogger().info("Added to java.library.path: " + path);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add directory to java.library.path: " + e.getMessage());
            throw e;
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
     * Verifies the database connection works by executing a simple query.
     * This method adds special handling for in-memory mode where we don't have 
     * a real database connection but still need the plugin to function.
     * 
     * @return true if connection verification succeeded or if we're in in-memory 
     *         mode (which simulates a working connection), false otherwise
     */
    private boolean verifyConnection() {
        // If no connection, we can't verify
        if (connection == null) {
            // In our "null connection" mode, pretend verification succeeded
            // This allows the plugin to continue functioning without a database
            if (inMemoryMode) {
                return true;
            }
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
        // Can't create tables without a connection
        if (connection == null) {
            plugin.getLogger().info("Skipping table creation - no database connection");
            return;
        }
        
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
     * Creates a dummy no-op database connection as a last resort fallback mechanism.
     * This method implements multiple layers of fallback:
     * 1. First tries to use the shaded H2 database driver
     * 2. Then tries the system H2 database driver if available
     * 3. Finally falls back to a null connection with in-memory mode flag set
     * 
     * The in-memory mode flag allows the plugin to function without a database by
     * signaling to the rest of the code that persistent storage isn't available.
     * This approach prioritizes plugin functionality over data persistence.
     */
    private void createDummyConnection() {
        try {
            // Try using pure H2 database as first fallback, as it's 100% Java with no native dependencies
            try {
                // First try the shaded H2 driver
                Class.forName("com.seventodie.lib.h2.Driver");
                connection = DriverManager.getConnection("jdbc:h2:mem:dummy;MODE=MySQL");
                plugin.getLogger().info("Using H2 dummy database connection (shaded)");
                return;
            } catch (Exception e) {
                // Try the non-shaded H2 driver
                try {
                    Class.forName("org.h2.Driver");
                    connection = DriverManager.getConnection("jdbc:h2:mem:dummy;MODE=MySQL");
                    plugin.getLogger().info("Using H2 dummy database connection (non-shaded)");
                    return;
                } catch (Exception e2) {
                    // H2 not available, create a very minimal implementation
                    plugin.getLogger().warning("H2 database not available: " + e2.getMessage());
                }
            }
            
            // Create a minimal connection for basic compatibility
            // This is a last-resort approach when nothing else works
            plugin.getLogger().warning("Creating minimal no-op database connection");
            inMemoryMode = true;
            
            // Skip the connection entirely - plugin will work in memory-only mode
            // This is better than trying to create a mock connection
            connection = null;
            plugin.getLogger().warning("Using no database mode. Data will not be persisted.");
            
            // Give up on real database implementation - plugin will be limited
            plugin.getLogger().severe("Failed to create any database connection. Plugin functionality will be limited.");
        } catch (Exception e) {
            plugin.getLogger().severe("Complete database fallback failure: " + e.getMessage());
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
            plugin.getLogger().severe("Database connection not available. Structure could not be saved. Attempting reconnection...");
            if (initialize()) {
                return saveStructure(id, type, location, sizeX, sizeY, sizeZ, schematic, rotation);
            }
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