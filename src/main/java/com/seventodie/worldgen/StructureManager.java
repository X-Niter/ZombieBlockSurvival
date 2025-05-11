package com.seventodie.worldgen;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.utils.SchematicUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages the creation, placement, and tracking of structures in the world
 */
public class StructureManager {
    
    private final SevenToDiePlugin plugin;
    private final SchematicUtils schematicUtils;
    private final Random random = new Random();
    private final Map<UUID, Structure> structures = new HashMap<>();
    
    /**
     * Types of structures that can be placed
     */
    public enum StructureType {
        HOUSE,
        STORE,
        FACTORY,
        HOSPITAL,
        POLICE_STATION,
        TRADER_OUTPOST,
        SPECIAL
    }
    
    /**
     * Constructor for StructureManager
     * 
     * @param plugin The SevenToDie plugin instance
     * @param schematicUtils The schematic utilities
     */
    public StructureManager(SevenToDiePlugin plugin, SchematicUtils schematicUtils) {
        this.plugin = plugin;
        this.schematicUtils = schematicUtils;
    }
    
    /**
     * Place a random structure of the given type
     * 
     * @param world The world to place in
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param type The structure type
     * @param rotation The rotation (0-3)
     * @return The created structure, or null if failed
     */
    public Structure placeRandomStructure(World world, int x, int y, int z, StructureType type, int rotation) {
        // TODO: Implement schematic loading and placement
        // This is a placeholder for now since we don't have actual schematics yet
        
        UUID structureId = UUID.randomUUID();
        Structure structure = new Structure(structureId, type, new Location(world, x, y, z), 16, 8, 16, rotation);
        
        // Register the structure
        structures.put(structureId, structure);
        
        return structure;
    }
    
    /**
     * Get a structure by its ID
     * 
     * @param id The structure ID
     * @return The structure, or null if not found
     */
    public Structure getStructure(UUID id) {
        return structures.get(id);
    }
    
    /**
     * Check if a location is inside a structure
     * 
     * @param location The location
     * @return The structure, or null if not in a structure
     */
    public Structure getStructureAt(Location location) {
        for (Structure structure : structures.values()) {
            if (structure.isInside(location)) {
                return structure;
            }
        }
        return null;
    }
    
    /**
     * Check if a block is inside a structure
     * 
     * @param block The block
     * @return The structure, or null if not in a structure
     */
    public Structure getStructureAt(Block block) {
        return getStructureAt(block.getLocation());
    }
    
    /**
     * Schedule structures to be placed around a road node
     * 
     * @param world The world
     * @param x The x coordinate
     * @param z The z coordinate
     * @param radius The radius to check
     */
    public void scheduleStructuresAroundNode(World world, int x, int z, int radius) {
        // This method is called from RoadGenerator
        // Place random structures around the road node
        int structureCount = 1 + random.nextInt(3); // 1-3 structures per node
        
        for (int i = 0; i < structureCount; i++) {
            // Choose a random offset
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetZ = random.nextInt(radius * 2) - radius;
            
            // Don't place structures too close to the road
            if (Math.abs(offsetX) < 5 && Math.abs(offsetZ) < 5) {
                continue;
            }
            
            int structureX = x + offsetX;
            int structureZ = z + offsetZ;
            
            // Get y level for structure
            int y = getHighestBlockYAt(world, structureX, structureZ);
            
            // Choose a random structure type
            StructureType type = StructureType.values()[random.nextInt(StructureType.values().length)];
            
            // Random rotation (0-3)
            int rotation = random.nextInt(4);
            
            // Place the structure
            placeRandomStructure(world, structureX, y, structureZ, type, rotation);
        }
    }
    
    /**
     * Get the highest solid block Y coordinate at the given position
     * 
     * @param world The world
     * @param x The x coordinate
     * @param z The z coordinate
     * @return The y coordinate of the highest solid block
     */
    private int getHighestBlockYAt(World world, int x, int z) {
        // Find the highest non-air block
        for (int y = world.getMaxHeight() - 1; y >= 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.getType().isAir()) {
                return y + 1;
            }
        }
        return 64; // Default if nothing is found
    }
    
    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        // Save any necessary data
        // Clear in-memory structure data
        structures.clear();
        plugin.getLogger().info("Structure manager cleaned up");
    }
    
    /**
     * Represents a structure in the world
     */
    public static class Structure {
        private final UUID id;
        private final StructureType type;
        private final Location location;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final int rotation;
        
        /**
         * Constructor for a Structure
         * 
         * @param id The unique ID
         * @param type The structure type
         * @param location The location
         * @param sizeX The X size
         * @param sizeY The Y size
         * @param sizeZ The Z size
         * @param rotation The rotation (0-3)
         */
        public Structure(UUID id, StructureType type, Location location, int sizeX, int sizeY, int sizeZ, int rotation) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.rotation = rotation;
        }
        
        /**
         * Get the structure ID
         * 
         * @return The ID
         */
        public UUID getId() {
            return id;
        }
        
        /**
         * Get the structure type
         * 
         * @return The type
         */
        public StructureType getType() {
            return type;
        }
        
        /**
         * Get the structure location
         * 
         * @return The location
         */
        public Location getLocation() {
            return location;
        }
        
        /**
         * Get the X size
         * 
         * @return The X size
         */
        public int getSizeX() {
            return sizeX;
        }
        
        /**
         * Get the Y size
         * 
         * @return The Y size
         */
        public int getSizeY() {
            return sizeY;
        }
        
        /**
         * Get the Z size
         * 
         * @return The Z size
         */
        public int getSizeZ() {
            return sizeZ;
        }
        
        /**
         * Get the rotation
         * 
         * @return The rotation (0-3)
         */
        public int getRotation() {
            return rotation;
        }
        
        /**
         * Check if a location is inside this structure
         * 
         * @param loc The location
         * @return True if inside
         */
        public boolean isInside(Location loc) {
            if (!loc.getWorld().equals(location.getWorld())) {
                return false;
            }
            
            double minX = location.getX();
            double minY = location.getY();
            double minZ = location.getZ();
            double maxX = minX + sizeX;
            double maxY = minY + sizeY;
            double maxZ = minZ + sizeZ;
            
            return loc.getX() >= minX && loc.getX() < maxX &&
                   loc.getY() >= minY && loc.getY() < maxY &&
                   loc.getZ() >= minZ && loc.getZ() < maxZ;
        }
        
        /**
         * Get the assigned quest for this structure
         * 
         * @return The quest ID, or null if none
         */
        public UUID getAssignedQuest() {
            // This is a placeholder implementation
            // In a real implementation, this would be stored in the structure data
            return null;
        }
    }
}