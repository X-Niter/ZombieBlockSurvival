package com.seventodie.worldgen;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.seventodie.SevenToDiePlugin;

/**
 * Maps vanilla Minecraft biomes to 7 Days to Die biome equivalents
 * and modifies block palettes accordingly.
 */
public class BiomeMapper implements Listener {
    
    private final SevenToDiePlugin plugin;
    
    // Mapping between Minecraft biomes and 7DtD equivalents
    private final Map<Biome, SevenBiome> biomeMapping = new HashMap<>();
    
    /**
     * 7 Days to Die biome equivalents
     */
    public enum SevenBiome {
        FOREST,
        GRASSLAND,
        DESERT,
        SNOW,
        WASTELAND
    }
    
    /**
     * Block palettes for each biome type
     */
    private final Map<SevenBiome, BiomePalette> biomePalettes = new EnumMap<>(SevenBiome.class);
    
    /**
     * Represents a block palette for a specific biome type
     */
    private class BiomePalette {
        Material grassBlock;
        Material surfaceBlock;
        Material subsurfaceBlock;
        Material foliage;
        Material tree;
        
        public BiomePalette(Material grassBlock, Material surfaceBlock, Material subsurfaceBlock, 
                           Material foliage, Material tree) {
            this.grassBlock = grassBlock;
            this.surfaceBlock = surfaceBlock;
            this.subsurfaceBlock = subsurfaceBlock;
            this.foliage = foliage;
            this.tree = tree;
        }
    }
    
    public BiomeMapper(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        
        // Initialize biome mappings
        initBiomeMappings();
        
        // Initialize block palettes
        initBiomePalettes();
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize mappings between Minecraft biomes and 7DtD biomes
     */
    private void initBiomeMappings() {
        // Forest biomes
        biomeMapping.put(Biome.FOREST, SevenBiome.FOREST);
        biomeMapping.put(Biome.BIRCH_FOREST, SevenBiome.FOREST);
        biomeMapping.put(Biome.DARK_FOREST, SevenBiome.FOREST);
        biomeMapping.put(Biome.OLD_GROWTH_BIRCH_FOREST, SevenBiome.FOREST);
        biomeMapping.put(Biome.JUNGLE, SevenBiome.FOREST);
        
        // Grassland biomes
        biomeMapping.put(Biome.PLAINS, SevenBiome.GRASSLAND);
        biomeMapping.put(Biome.SUNFLOWER_PLAINS, SevenBiome.GRASSLAND);
        biomeMapping.put(Biome.SAVANNA, SevenBiome.GRASSLAND);
        biomeMapping.put(Biome.SAVANNA_PLATEAU, SevenBiome.GRASSLAND);
        
        // Desert biomes
        biomeMapping.put(Biome.DESERT, SevenBiome.DESERT);
        biomeMapping.put(Biome.WINDSWEPT_HILLS, SevenBiome.DESERT);
        biomeMapping.put(Biome.BADLANDS, SevenBiome.DESERT);
        biomeMapping.put(Biome.WOODED_BADLANDS, SevenBiome.DESERT);
        biomeMapping.put(Biome.ERODED_BADLANDS, SevenBiome.DESERT);
        
        // Snow biomes
        biomeMapping.put(Biome.SNOWY_PLAINS, SevenBiome.SNOW);
        biomeMapping.put(Biome.SNOWY_SLOPES, SevenBiome.SNOW);
        biomeMapping.put(Biome.SNOWY_TAIGA, SevenBiome.SNOW);
        biomeMapping.put(Biome.GROVE, SevenBiome.SNOW);
        biomeMapping.put(Biome.ICE_SPIKES, SevenBiome.SNOW);
        
        // Wasteland biomes
        biomeMapping.put(Biome.BADLANDS, SevenBiome.WASTELAND);
        biomeMapping.put(Biome.WOODED_BADLANDS, SevenBiome.WASTELAND);
        biomeMapping.put(Biome.ERODED_BADLANDS, SevenBiome.WASTELAND);
        biomeMapping.put(Biome.NETHER_WASTES, SevenBiome.WASTELAND);
        biomeMapping.put(Biome.SOUL_SAND_VALLEY, SevenBiome.WASTELAND);
        
        plugin.getLogger().info("Initialized biome mappings");
    }
    
    /**
     * Initialize block palettes for each 7DtD biome type
     */
    private void initBiomePalettes() {
        // Forest palette
        biomePalettes.put(SevenBiome.FOREST, new BiomePalette(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.STONE,
            Material.OAK_LEAVES,
            Material.OAK_LOG
        ));
        
        // Grassland palette
        biomePalettes.put(SevenBiome.GRASSLAND, new BiomePalette(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.STONE,
            Material.FERN,
            Material.BIRCH_LOG
        ));
        
        // Desert palette
        biomePalettes.put(SevenBiome.DESERT, new BiomePalette(
            Material.SAND,
            Material.SANDSTONE,
            Material.SMOOTH_SANDSTONE,
            Material.DEAD_BUSH,
            Material.STRIPPED_ACACIA_LOG
        ));
        
        // Snow palette
        biomePalettes.put(SevenBiome.SNOW, new BiomePalette(
            Material.SNOW_BLOCK,
            Material.SNOW,
            Material.PACKED_ICE,
            Material.SPRUCE_LEAVES,
            Material.SPRUCE_LOG
        ));
        
        // Wasteland palette
        biomePalettes.put(SevenBiome.WASTELAND, new BiomePalette(
            Material.COARSE_DIRT,
            Material.DIRT,
            Material.TERRACOTTA,
            Material.DEAD_BUSH,
            Material.STRIPPED_OAK_LOG
        ));
        
        plugin.getLogger().info("Initialized biome palettes");
    }
    
    /**
     * Get the 7DtD biome equivalent for a Minecraft biome
     * 
     * @param biome The Minecraft biome
     * @return The 7DtD biome equivalent
     */
    public SevenBiome getSevenBiome(Biome biome) {
        return biomeMapping.getOrDefault(biome, SevenBiome.GRASSLAND);
    }
    
    /**
     * Get the block palette for a 7DtD biome
     * 
     * @param sevenBiome The 7DtD biome
     * @return The block palette
     */
    public BiomePalette getBiomePalette(SevenBiome sevenBiome) {
        return biomePalettes.get(sevenBiome);
    }
    
    /**
     * Apply biome-specific material to a block
     * 
     * @param block The block to modify
     * @param blockType The type of block (grass, surface, etc.)
     */
    public void applyBiomeMaterial(Block block) {
        Biome biome = block.getBiome();
        SevenBiome sevenBiome = getSevenBiome(biome);
        BiomePalette palette = biomePalettes.get(sevenBiome);
        
        if (block.getType() == Material.GRASS_BLOCK) {
            block.setType(palette.grassBlock);
        } else if (block.getType() == Material.DIRT) {
            block.setType(palette.surfaceBlock);
        } else if (block.getType() == Material.STONE && block.getY() > 40) {
            block.setType(palette.subsurfaceBlock);
        }
    }
    
    /**
     * Handle chunk load to apply biome-specific blocks
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            // This is a newly generated chunk, schedule biome application
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                applyBiomeToChunk(event.getChunk().getWorld(), 
                                 event.getChunk().getX(), 
                                 event.getChunk().getZ());
            }, 10L); // Slight delay to ensure chunk is fully generated
        }
    }
    
    /**
     * Apply biome-specific blocks to a chunk
     * 
     * @param world The world
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     */
    public void applyBiomeToChunk(World world, int chunkX, int chunkZ) {
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Biome biome = world.getBiome(startX + x, 0, startZ + z);
                SevenBiome sevenBiome = getSevenBiome(biome);
                BiomePalette palette = biomePalettes.get(sevenBiome);
                
                // Find the highest block at this position
                int maxY = world.getHighestBlockYAt(startX + x, startZ + z);
                
                // Apply to top blocks
                for (int y = maxY; y > maxY - 5; y--) {
                    Block block = world.getBlockAt(startX + x, y, startZ + z);
                    
                    if (block.getType() == Material.GRASS_BLOCK) {
                        block.setType(palette.grassBlock);
                    } else if (block.getType() == Material.DIRT) {
                        block.setType(palette.surfaceBlock);
                    } else if (block.getType() == Material.STONE) {
                        block.setType(palette.subsurfaceBlock);
                    }
                }
            }
        }
    }
    
    /**
     * Get grass color for a specific 7DtD biome
     * 
     * @param sevenBiome The 7DtD biome
     * @return The grass color as an RGB integer
     */
    public int getGrassColorForBiome(SevenBiome sevenBiome) {
        switch (sevenBiome) {
            case FOREST:
                return 0x5EB424; // Vibrant green
            case GRASSLAND:
                return 0x80B497; // Lighter green
            case DESERT:
                return 0xC2B280; // Tan/brown
            case SNOW:
                return 0xFFFFFF; // White
            case WASTELAND:
                return 0x8B4513; // Brown
            default:
                return 0x7FBF50; // Default minecraft green
        }
    }
    
    /**
     * Get foliage color for a specific 7DtD biome
     * 
     * @param sevenBiome The 7DtD biome
     * @return The foliage color as an RGB integer
     */
    public int getFoliageColorForBiome(SevenBiome sevenBiome) {
        switch (sevenBiome) {
            case FOREST:
                return 0x59AE30; // Vibrant green
            case GRASSLAND:
                return 0x79C05A; // Standard green
            case DESERT:
                return 0xB1A442; // Yellowish
            case SNOW:
                return 0xA0FFFF; // Light blue tint
            case WASTELAND:
                return 0x6B4423; // Dark brown
            default:
                return 0x6A7039; // Default minecraft foliage
        }
    }
}
