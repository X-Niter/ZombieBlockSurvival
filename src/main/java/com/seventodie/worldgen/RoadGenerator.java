package com.seventodie.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import com.seventodie.SevenToDiePlugin;

/**
 * Handles road generation in the world, creating networks of roads
 * that connect different zones and structures.
 */
public class RoadGenerator {
    
    private final SevenToDiePlugin plugin;
    private final Random random;
    
    // Road generation parameters
    private static final int MAIN_ROAD_SPACING = 512; // Distance between main roads
    private static final int SECONDARY_ROAD_SPACING = 128; // Distance between secondary roads
    private static final int ROAD_WIDTH_MAIN = 5; // Width of main roads
    private static final int ROAD_WIDTH_SECONDARY = 3; // Width of secondary roads
    
    // Materials
    private static final Material ROAD_MATERIAL = Material.BLACK_CONCRETE;
    private static final Material ROAD_CURB_MATERIAL = Material.GRAY_CONCRETE;
    
    // Road nodes for connectivity
    private List<RoadNode> roadNodes = new ArrayList<>();
    
    /**
     * Represents a node in the road network
     */
    private class RoadNode {
        int x, z;
        List<RoadNode> connections = new ArrayList<>();
        boolean isMainNode;
        
        public RoadNode(int x, int z, boolean isMainNode) {
            this.x = x;
            this.z = z;
            this.isMainNode = isMainNode;
        }
    }
    
    public RoadGenerator(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    /**
     * Generates roads for a chunk that's being generated
     * 
     * @param world The world
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public void generateRoadsInChunk(World world, int chunkX, int chunkZ) {
        // Convert to block coordinates
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;
        
        // Check if this chunk should contain main roads
        boolean hasMainRoadX = isNearMainRoad(blockX, MAIN_ROAD_SPACING);
        boolean hasMainRoadZ = isNearMainRoad(blockZ, MAIN_ROAD_SPACING);
        
        // Check if this chunk should contain secondary roads
        boolean hasSecondaryRoadX = isNearMainRoad(blockX, SECONDARY_ROAD_SPACING);
        boolean hasSecondaryRoadZ = isNearMainRoad(blockZ, SECONDARY_ROAD_SPACING);
        
        // Generate the roads if needed
        if (hasMainRoadX || hasMainRoadZ || hasSecondaryRoadX || hasSecondaryRoadZ) {
            // Find the highest point in the chunk for road height
            int roadHeight = findAverageGroundHeight(world, blockX, blockZ);
            
            // Create a road node if this is an intersection
            if ((hasMainRoadX || hasSecondaryRoadX) && (hasMainRoadZ || hasSecondaryRoadZ)) {
                // This is a road intersection - add to road nodes
                boolean isMainNode = hasMainRoadX && hasMainRoadZ;
                createRoadNode(world, blockX + 8, roadHeight, blockZ + 8, isMainNode);
            }
            
            // Generate the actual roads
            if (hasMainRoadX) {
                generateRoadSegment(world, blockX, blockZ, true, true, roadHeight);
            } else if (hasSecondaryRoadX) {
                generateRoadSegment(world, blockX, blockZ, true, false, roadHeight);
            }
            
            if (hasMainRoadZ) {
                generateRoadSegment(world, blockX, blockZ, false, true, roadHeight);
            } else if (hasSecondaryRoadZ) {
                generateRoadSegment(world, blockX, blockZ, false, false, roadHeight);
            }
        }
    }
    
    /**
     * Create a road node at the specified coordinates
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param isMainNode Whether this is a main road node
     */
    private void createRoadNode(World world, int x, int y, int z, boolean isMainNode) {
        RoadNode node = new RoadNode(x, z, isMainNode);
        roadNodes.add(node);
        
        // Connect to nearby nodes
        connectToNearbyNodes(node);
        
        // If it's a main node, inform the structure manager to place structures around it
        if (isMainNode) {
            plugin.getStructureManager().scheduleStructuresAroundNode(world, x, y, z);
        }
    }
    
    /**
     * Connect a road node to nearby existing nodes
     * 
     * @param node The node to connect
     */
    private void connectToNearbyNodes(RoadNode node) {
        // Find nodes that are aligned on the same road
        for (RoadNode existingNode : roadNodes) {
            if (existingNode == node) continue;
            
            // Check if nodes are on the same X or Z line (road alignment)
            if (existingNode.x == node.x || existingNode.z == node.z) {
                // Check distance - connect if they're close enough
                double distance = Math.sqrt(Math.pow(existingNode.x - node.x, 2) + 
                                           Math.pow(existingNode.z - node.z, 2));
                
                if (distance < MAIN_ROAD_SPACING * 1.5) {
                    node.connections.add(existingNode);
                    existingNode.connections.add(node);
                }
            }
        }
    }
    
    /**
     * Generates a segment of road in a chunk
     * 
     * @param world The world
     * @param blockX The starting X coordinate
     * @param blockZ The starting Z coordinate
     * @param isXAxis True if the road runs along the X axis
     * @param isMainRoad True if this is a main road
     * @param roadHeight The Y level for the road
     */
    private void generateRoadSegment(World world, int blockX, int blockZ, boolean isXAxis, boolean isMainRoad, int roadHeight) {
        int roadWidth = isMainRoad ? ROAD_WIDTH_MAIN : ROAD_WIDTH_SECONDARY;
        
        // Calculate road start and end coordinates
        int roadStart, roadEnd;
        if (isXAxis) {
            // Road running along X axis
            roadStart = blockZ + 8 - roadWidth / 2;
            roadEnd = roadStart + roadWidth;
            
            // Generate road along the entire X length of the chunk
            for (int x = blockX; x < blockX + 16; x++) {
                for (int z = roadStart; z < roadEnd; z++) {
                    placeRoadBlock(world, x, roadHeight, z, z == roadStart || z == roadEnd - 1);
                }
            }
        } else {
            // Road running along Z axis
            roadStart = blockX + 8 - roadWidth / 2;
            roadEnd = roadStart + roadWidth;
            
            // Generate road along the entire Z length of the chunk
            for (int z = blockZ; z < blockZ + 16; z++) {
                for (int x = roadStart; x < roadEnd; x++) {
                    placeRoadBlock(world, x, roadHeight, z, x == roadStart || x == roadEnd - 1);
                }
            }
        }
    }
    
    /**
     * Places a road block at the specified location
     * 
     * @param world The world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param isCurb Whether this block is a curb
     */
    private void placeRoadBlock(World world, int x, int y, int z, boolean isCurb) {
        // Get the block and set it to road material
        Block block = world.getBlockAt(x, y, z);
        block.setType(isCurb ? ROAD_CURB_MATERIAL : ROAD_MATERIAL);
        
        // Clear a few blocks above for clearance
        for (int i = 1; i <= 3; i++) {
            Block airBlock = world.getBlockAt(x, y + i, z);
            if (!airBlock.getType().equals(Material.AIR)) {
                airBlock.setType(Material.AIR);
            }
        }
        
        // Add support blocks below if needed
        Block supportBlock = world.getBlockAt(x, y - 1, z);
        if (supportBlock.getType().equals(Material.AIR) || 
            supportBlock.getType().equals(Material.WATER) ||
            supportBlock.getType().isItem()) {
            supportBlock.setType(Material.STONE);
        }
    }
    
    /**
     * Checks if a coordinate is near a main road
     * 
     * @param coord The coordinate to check
     * @param spacing The road spacing
     * @return True if the coordinate is near a main road
     */
    private boolean isNearMainRoad(int coord, int spacing) {
        // Calculate distance to nearest road axis
        int distToRoad = Math.abs(coord % spacing - (spacing / 2));
        return distToRoad < 8; // Check if within 8 blocks of the road center
    }
    
    /**
     * Find the average ground height in a chunk for road placement
     * 
     * @param world The world
     * @param blockX The chunk's X coordinate
     * @param blockZ The chunk's Z coordinate
     * @return The average height for road placement
     */
    private int findAverageGroundHeight(World world, int blockX, int blockZ) {
        int totalHeight = 0;
        int sampleCount = 0;
        
        // Sample several points in the chunk
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                int height = getHighestBlockYAt(world, blockX + x, blockZ + z);
                totalHeight += height;
                sampleCount++;
            }
        }
        
        // Return average height
        return sampleCount > 0 ? totalHeight / sampleCount : 64;
    }
    
    /**
     * Get the highest non-air block at a position
     * 
     * @param world The world
     * @param x X coordinate
     * @param z Z coordinate
     * @return The Y coordinate of the highest block
     */
    private int getHighestBlockYAt(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y >= 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            if (!type.equals(Material.AIR) && 
                !type.equals(Material.WATER) &&
                !type.equals(Material.LAVA) &&
                !type.equals(Material.OAK_LEAVES) &&
                !type.equals(Material.BIRCH_LEAVES) &&
                !type.equals(Material.SPRUCE_LEAVES) &&
                !type.equals(Material.DARK_OAK_LEAVES) &&
                !type.equals(Material.ACACIA_LEAVES) &&
                !type.equals(Material.JUNGLE_LEAVES) &&
                !type.equals(Material.AZALEA_LEAVES) &&
                !type.equals(Material.FLOWERING_AZALEA_LEAVES) &&
                !type.name().endsWith("LEAVES")) {
                return y + 1;
            }
        }
        return 64; // Default if nothing is found
    }
    
    /**
     * Gets a list of all road nodes for other systems to use
     * 
     * @return The list of road nodes
     */
    public List<Location> getRoadNodeLocations(World world) {
        List<Location> locations = new ArrayList<>();
        
        for (RoadNode node : roadNodes) {
            // Get the highest block at this location for accurate Y
            int y = getHighestBlockYAt(world, node.x, node.z);
            locations.add(new Location(world, node.x, y, node.z));
        }
        
        return locations;
    }
    
    /**
     * Gets the nearest road node to a position
     * 
     * @param x X coordinate
     * @param z Z coordinate
     * @return The nearest road node, or null if none exist
     */
    public RoadNode getNearestRoadNode(int x, int z) {
        RoadNode nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (RoadNode node : roadNodes) {
            double distance = Math.sqrt(Math.pow(node.x - x, 2) + Math.pow(node.z - z, 2));
            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }
        
        return nearest;
    }
    
    /**
     * Generate roads for a world using a custom chunk generator
     * 
     * @param generator The chunk generator
     * @param worldInfo The world info
     * @param random The random generator
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @param biomeGrid The biome grid
     * @param defaultChunk The default chunk data
     */
    public void populateChunk(ChunkGenerator generator, WorldInfo worldInfo, Random random, 
                           int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        // This would be called from a chunk generator override
        // Use the methods above to generate roads in the chunk data
        // WorldInfo doesn't have getWorld() in Paper 1.21, so we need a World parameter to be passed in
        // This is a placeholder that would be implemented in the actual chunk generator
        // generateRoadsInChunk(world, chunkX, chunkZ);
    }
}
