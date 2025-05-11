package com.seventodie.listeners;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import com.seventodie.SevenToDiePlugin;

/**
 * Handles world generation and chunk loading events to add custom elements
 * like roads, structures, and biome modifications.
 */
public class WorldGenListener implements Listener {
    
    private final SevenToDiePlugin plugin;
    
    public WorldGenListener(SevenToDiePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle world initialization to register custom generators
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldInit(WorldInitEvent event) {
        // Register any custom generators or modifications
        plugin.getLogger().info("World " + event.getWorld().getName() + " is initializing");
        
        // This is where we would register custom chunk generators, but since we're modifying
        // after vanilla generation, we'll do that in chunk load events instead
    }
    
    /**
     * Handle world loading to load saved structures and data
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        
        plugin.getLogger().info("World " + world.getName() + " loaded");
        
        // Load structures for this world
        // This is handled by the structure manager during plugin startup
    }
    
    /**
     * Handle chunk loading to add roads and structures
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Only process newly generated chunks
        if (!event.isNewChunk()) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        
        // Skip non-overworld dimensions
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        
        // Generate roads and structures
        // Since road generation might be intensive, schedule it for the next tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Generate roads in this chunk
            plugin.getRoadGenerator().generateRoadsInChunk(
                world, 
                chunk.getX(), 
                chunk.getZ()
            );
            
            // Structures are generated via the road nodes in the road generator
        });
        
        // Apply biome modifications for this chunk
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getBiomeMapper().applyBiomeToChunk(
                world,
                chunk.getX(),
                chunk.getZ()
            );
        }, 2L); // Slight delay to ensure road generation completes first
    }
}
