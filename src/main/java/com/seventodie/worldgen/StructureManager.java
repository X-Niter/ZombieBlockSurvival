package com.seventodie.worldgen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.quests.Quest;
import com.seventodie.quests.QuestManager;
import com.seventodie.utils.SchematicUtils;

/**
 * Manages structure generation, placement, and tracking in the world.
 */
public class StructureManager {
    
    private final SevenToDiePlugin plugin;
    private final Random random;
    
    // Structure types
    public enum StructureType {
        RESIDENTIAL,
        COMMERCIAL,
        INDUSTRIAL,
        TRADER_OUTPOST
    }
    
    // Structure tracking
    private Map<UUID, Structure> structures = new HashMap<>();
    private Map<String, List<String>> structureSchematicFiles = new HashMap<>();
    
    // Trader outpost parameters
    private static final int TRADER_SPACING = 300;
    
    /**
     * Represents a placed structure in the world
     */
    public class Structure {
        private UUID id;
        private StructureType type;
        private Location location;
        private Vector size;
        private String schematicName;
        private int rotation; // 0, 90, 180, 270
        private Quest assignedQuest;
        
        public Structure(UUID id, StructureType type, Location location, Vector size, 
                         String schematicName, int rotation) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.size = size;
            this.schematicName = schematicName;
            this.rotation = rotation;
        }
        
        public UUID getId() {
            return id;
        }
        
        public StructureType getType() {
            return type;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public Vector getSize() {
            return size;
        }
        
        public String getSchematicName() {
            return schematicName;
        }
        
        public int getRotation() {
            return rotation;
        }
        
        public Quest getAssignedQuest() {
            return assignedQuest;
        }
        
        public void setAssignedQuest(Quest quest) {
            this.assignedQuest = quest;
        }
        
        /**
         * Check if a location is within this structure
         */
        public boolean containsLocation(Location loc) {
            if (!loc.getWorld().equals(location.getWorld())) {
                return false;
            }
            
            // Get structure bounds accounting for rotation
            double minX = location.getX();
            double minY = location.getY();
            double minZ = location.getZ();
            
            double maxX, maxZ;
            
            // Adjust bounds based on rotation
            if (rotation == 0 || rotation == 180) {
                maxX = minX + size.getX();
                maxZ = minZ + size.getZ();
            } else {
                maxX = minX + size.getZ();
                maxZ = minZ + size.getX();
            }
            
            double maxY = minY + size.getY();
            
            // Check if location is within bounds
            return loc.getX() >= minX && loc.getX() <= maxX &&
                   loc.getY() >= minY && loc.getY() <= maxY &&
                   loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }
    }
    
    public StructureManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        
        // Load structure schematics
        loadStructureSchematics();
    }
    
    /**
     * Load available structure schematics from the schematics folder
     */
    private void loadStructureSchematics() {
        // Initialize structure type lists
        for (StructureType type : StructureType.values()) {
            structureSchematicFiles.put(type.name(), new ArrayList<>());
        }
        
        // Load schematic files from plugin directory
        File schematicsDir = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsDir.exists()) {
            schematicsDir.mkdirs();
            
            // Create sample directory structure
            new File(schematicsDir, "residential").mkdirs();
            new File(schematicsDir, "commercial").mkdirs();
            new File(schematicsDir, "industrial").mkdirs();
            new File(schematicsDir, "trader").mkdirs();
            
            plugin.getLogger().info("Created schematics directory structure");
            return;
        }
        
        // Load residential schematics
        loadSchematicsFromDir(new File(schematicsDir, "residential"), StructureType.RESIDENTIAL);
        
        // Load commercial schematics
        loadSchematicsFromDir(new File(schematicsDir, "commercial"), StructureType.COMMERCIAL);
        
        // Load industrial schematics
        loadSchematicsFromDir(new File(schematicsDir, "industrial"), StructureType.INDUSTRIAL);
        
        // Load trader outpost schematics
        loadSchematicsFromDir(new File(schematicsDir, "trader"), StructureType.TRADER_OUTPOST);
        
        // Log loaded schematic counts
        for (StructureType type : StructureType.values()) {
            int count = structureSchematicFiles.get(type.name()).size();
            plugin.getLogger().info("Loaded " + count + " " + type.name() + " schematics");
        }
    }
    
    /**
     * Load schematics from a directory
     * 
     * @param dir The directory to load from
     * @param type The structure type
     */
    private void loadSchematicsFromDir(File dir, StructureType type) {
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".schem") || name.endsWith(".schematic"));
        if (files != null) {
            for (File file : files) {
                structureSchematicFiles.get(type.name()).add(file.getAbsolutePath());
            }
        }
    }
    
    /**
     * Schedule structure placement around a road node
     * 
     * @param world The world
     * @param x X coordinate of the node
     * @param y Y coordinate of the node
     * @param z Z coordinate of the node
     */
    public void scheduleStructuresAroundNode(World world, int x, int y, int z) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            generateStructuresAroundNode(world, x, y, z);
        }, 20L); // 1 second delay for chunk loading
    }
    
    /**
     * Generate structures around a road node
     * 
     * @param world The world
     * @param x X coordinate of the node
     * @param y Y coordinate of the node
     * @param z Z coordinate of the node
     */
    private void generateStructuresAroundNode(World world, int x, int y, int z) {
        // Determine structure types based on noise and position
        double noise = (Math.sin(x * 0.01) + Math.cos(z * 0.01)) * 0.5 + 0.5;
        
        StructureType primaryType;
        if (noise < 0.33) {
            primaryType = StructureType.RESIDENTIAL;
        } else if (noise < 0.66) {
            primaryType = StructureType.COMMERCIAL;
        } else {
            primaryType = StructureType.INDUSTRIAL;
        }
        
        // Check if there should be a trader outpost nearby
        boolean placeTrader = shouldPlaceTraderOutpost(world, x, z);
        
        // Place structures in a pattern around the intersection
        placeStructureInDirection(world, x, y, z, 1, 0, primaryType); // East
        placeStructureInDirection(world, x, y, z, 0, 1, primaryType); // South
        placeStructureInDirection(world, x, y, z, -1, 0, primaryType); // West
        placeStructureInDirection(world, x, y, z, 0, -1, primaryType); // North
        
        // Place trader if needed
        if (placeTrader) {
            placeTraderOutpost(world, x, y, z);
        }
    }
    
    /**
     * Place a structure in a specified direction from a point
     * 
     * @param world The world
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param z Starting Z coordinate
     * @param dirX X direction (1, 0, or -1)
     * @param dirZ Z direction (1, 0, or -1)
     * @param type The structure type to place
     */
    private void placeStructureInDirection(World world, int x, int y, int z, int dirX, int dirZ, StructureType type) {
        // Calculate placement position (20-40 blocks away from intersection)
        int distance = 20 + random.nextInt(21);
        int placeX = x + dirX * distance;
        int placeZ = z + dirZ * distance;
        
        // Find ground level
        int placeY = findSuitableGroundLevel(world, placeX, placeZ);
        
        // Determine rotation based on direction
        int rotation = 0;
        if (dirX == 1) rotation = 90;      // East
        else if (dirX == -1) rotation = 270;// West
        else if (dirZ == 1) rotation = 180; // South
        else if (dirZ == -1) rotation = 0;  // North
        
        // Place the structure
        placeRandomStructure(world, placeX, placeY, placeZ, type, rotation);
    }
    
    /**
     * Place a trader outpost near a position
     * 
     * @param world The world
     * @param x X coordinate center
     * @param y Y coordinate center
     * @param z Z coordinate center
     */
    private void placeTraderOutpost(World world, int x, int y, int z) {
        // Offset 50-70 blocks in random direction
        int distance = 50 + random.nextInt(21);
        double angle = random.nextDouble() * Math.PI * 2;
        
        int placeX = x + (int)(Math.cos(angle) * distance);
        int placeZ = z + (int)(Math.sin(angle) * distance);
        
        // Find ground level
        int placeY = findSuitableGroundLevel(world, placeX, placeZ);
        
        // Random rotation (0, 90, 180, 270)
        int rotation = random.nextInt(4) * 90;
        
        // Place the trader outpost
        Structure trader = placeRandomStructure(world, placeX, placeY, placeZ, StructureType.TRADER_OUTPOST, rotation);
        
        // Register with trader manager if successful
        if (trader != null) {
            plugin.getTraderManager().registerTraderOutpost(trader);
        }
    }
    
    /**
     * Place a random structure of the specified type
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param type The structure type
     * @param rotation Rotation in degrees (0, 90, 180, or 270)
     * @return The placed structure, or null if placement failed
     */
    public Structure placeRandomStructure(World world, int x, int y, int z, StructureType type, int rotation) {
        List<String> availableSchematics = structureSchematicFiles.get(type.name());
        
        if (availableSchematics.isEmpty()) {
            plugin.getLogger().warning("No schematics available for type: " + type.name());
            return null;
        }
        
        // Pick a random schematic
        String schematicFile = availableSchematics.get(random.nextInt(availableSchematics.size()));
        
        try {
            // Load and paste the schematic
            SchematicUtils schematicUtils = new SchematicUtils(plugin);
            File file = new File(schematicFile);
            
            // Get just the filename without path or extension
            String schematicName = file.getName();
            if (schematicName.contains(".")) {
                schematicName = schematicName.substring(0, schematicName.lastIndexOf('.'));
            }
            
            // Paste the schematic
            Vector size = schematicUtils.pasteSchematic(file, world, x, y, z, rotation);
            
            if (size != null) {
                // Create and register the structure
                UUID structureId = UUID.randomUUID();
                Structure structure = new Structure(
                    structureId, 
                    type, 
                    new Location(world, x, y, z), 
                    size, 
                    schematicName, 
                    rotation
                );
                
                // Register the structure
                structures.put(structureId, structure);
                
                // If not a trader outpost, assign a quest to the structure
                if (type != StructureType.TRADER_OUTPOST) {
                    QuestManager questManager = plugin.getQuestManager();
                    Quest quest = questManager.createRandomQuest(structure);
                    structure.setAssignedQuest(quest);
                }
                
                // Save structure to database
                saveStructure(structure);
                
                return structure;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error pasting schematic: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find a suitable ground level for structure placement
     * 
     * @param world The world
     * @param x X coordinate
     * @param z Z coordinate
     * @return Y coordinate for placement
     */
    private int findSuitableGroundLevel(World world, int x, int z) {
        // Start from top and move down
        for (int y = world.getMaxHeight() - 1; y >= 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.getType().isAir() && !block.getType().toString().contains("LEAVES")) {
                return y + 1; // Place on top of the first solid block
            }
        }
        
        return 64; // Default height if nothing found
    }
    
    /**
     * Check if a trader outpost should be placed at a location
     * 
     * @param world The world
     * @param x X coordinate
     * @param z Z coordinate
     * @return True if a trader outpost should be placed
     */
    private boolean shouldPlaceTraderOutpost(World world, int x, int z) {
        // Check if we're in a grid position that should have a trader
        int gridX = Math.floorDiv(x, TRADER_SPACING);
        int gridZ = Math.floorDiv(z, TRADER_SPACING);
        
        // Use hash of grid coordinates for deterministic placement
        long hash = (gridX * 73856093L) ^ (gridZ * 19349663L);
        Random gridRandom = new Random(hash);
        
        // 10% chance for a position to have a trader
        if (gridRandom.nextDouble() < 0.1) {
            // Check there are no traders nearby
            for (Structure structure : structures.values()) {
                if (structure.getType() == StructureType.TRADER_OUTPOST && 
                    structure.getLocation().getWorld().equals(world)) {
                    
                    double distance = structure.getLocation().distance(new Location(world, x, 0, z));
                    if (distance < TRADER_SPACING) {
                        return false; // Too close to another trader
                    }
                }
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Save structure data to the plugin's database
     * 
     * @param structure The structure to save
     */
    private void saveStructure(Structure structure) {
        // Save to database
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("id", structure.getId().toString());
            config.set("type", structure.getType().name());
            config.set("world", structure.getLocation().getWorld().getName());
            config.set("x", structure.getLocation().getX());
            config.set("y", structure.getLocation().getY());
            config.set("z", structure.getLocation().getZ());
            config.set("sizeX", structure.getSize().getX());
            config.set("sizeY", structure.getSize().getY());
            config.set("sizeZ", structure.getSize().getZ());
            config.set("schematic", structure.getSchematicName());
            config.set("rotation", structure.getRotation());
            
            // Save to structures folder
            File structuresDir = new File(plugin.getDataFolder(), "structures");
            if (!structuresDir.exists()) {
                structuresDir.mkdirs();
            }
            
            File structureFile = new File(structuresDir, structure.getId() + ".yml");
            config.save(structureFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save structure: " + e.getMessage());
        }
    }
    
    /**
     * Load all structures from the plugin's database
     */
    public void loadStructures() {
        File structuresDir = new File(plugin.getDataFolder(), "structures");
        if (!structuresDir.exists()) {
            return;
        }
        
        File[] files = structuresDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                UUID id = UUID.fromString(config.getString("id"));
                StructureType type = StructureType.valueOf(config.getString("type"));
                World world = Bukkit.getWorld(config.getString("world"));
                
                if (world == null) {
                    plugin.getLogger().warning("Could not load structure, world not found: " + config.getString("world"));
                    continue;
                }
                
                double x = config.getDouble("x");
                double y = config.getDouble("y");
                double z = config.getDouble("z");
                
                double sizeX = config.getDouble("sizeX");
                double sizeY = config.getDouble("sizeY");
                double sizeZ = config.getDouble("sizeZ");
                
                String schematic = config.getString("schematic");
                int rotation = config.getInt("rotation");
                
                Location location = new Location(world, x, y, z);
                Vector size = new Vector(sizeX, sizeY, sizeZ);
                
                Structure structure = new Structure(id, type, location, size, schematic, rotation);
                structures.put(id, structure);
                
                // If this is a trader outpost, register it with the trader manager
                if (type == StructureType.TRADER_OUTPOST) {
                    plugin.getTraderManager().registerTraderOutpost(structure);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading structure from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + structures.size() + " structures");
    }
    
    /**
     * Reset a structure to its original state (regenerate)
     * 
     * @param structureId The UUID of the structure to reset
     * @return True if the structure was reset successfully
     */
    public boolean resetStructure(UUID structureId) {
        Structure structure = structures.get(structureId);
        if (structure == null) {
            return false;
        }
        
        // Find the schematic file
        String schematicName = structure.getSchematicName();
        File schematicFile = findSchematicByName(schematicName, structure.getType());
        
        if (schematicFile == null || !schematicFile.exists()) {
            plugin.getLogger().warning("Could not find schematic file for structure: " + schematicName);
            return false;
        }
        
        try {
            // Repaste the schematic
            SchematicUtils schematicUtils = new SchematicUtils(plugin);
            Location loc = structure.getLocation();
            
            schematicUtils.pasteSchematic(
                schematicFile, 
                loc.getWorld(), 
                loc.getBlockX(), 
                loc.getBlockY(), 
                loc.getBlockZ(), 
                structure.getRotation()
            );
            
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Error resetting structure: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a schematic file by name and type
     * 
     * @param name The schematic name
     * @param type The structure type
     * @return The schematic file, or null if not found
     */
    private File findSchematicByName(String name, StructureType type) {
        List<String> schematicPaths = structureSchematicFiles.get(type.name());
        
        for (String path : schematicPaths) {
            File file = new File(path);
            String fileName = file.getName();
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            
            if (fileName.equals(name)) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * Get a structure by UUID
     * 
     * @param id The structure UUID
     * @return The structure, or null if not found
     */
    public Structure getStructure(UUID id) {
        return structures.get(id);
    }
    
    /**
     * Find which structure contains a location
     * 
     * @param location The location to check
     * @return The structure containing the location, or null if none
     */
    public Structure getStructureAt(Location location) {
        for (Structure structure : structures.values()) {
            if (structure.containsLocation(location)) {
                return structure;
            }
        }
        return null;
    }
    
    /**
     * Get all structures of a specific type
     * 
     * @param type The structure type
     * @return A list of structures of that type
     */
    public List<Structure> getStructuresByType(StructureType type) {
        List<Structure> result = new ArrayList<>();
        
        for (Structure structure : structures.values()) {
            if (structure.getType() == type) {
                result.add(structure);
            }
        }
        
        return result;
    }
}
