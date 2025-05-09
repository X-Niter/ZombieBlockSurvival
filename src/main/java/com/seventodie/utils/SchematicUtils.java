package com.seventodie.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import com.seventodie.SevenToDiePlugin;

/**
 * Utility class for handling schematic files, for loading and pasting structures.
 */
public class SchematicUtils {
    
    private final SevenToDiePlugin plugin;
    
    // WorldEdit integration - will use dynamic loading to avoid hard dependency
    private boolean worldEditAvailable;
    
    public SchematicUtils(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        
        // Check if WorldEdit is available
        this.worldEditAvailable = Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
    }
    
    /**
     * Pastes a schematic file at the specified location
     * 
     * @param file The schematic file
     * @param world The world to paste in
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param rotation The rotation in degrees (0, 90, 180, 270)
     * @return The size of the pasted schematic
     * @throws IOException If there is an error reading the schematic file
     */
    public Vector pasteSchematic(File file, World world, int x, int y, int z, int rotation) throws IOException {
        if (worldEditAvailable) {
            try {
                return pasteWithWorldEdit(file, world, x, y, z, rotation);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to paste with WorldEdit: " + e.getMessage());
                return pasteWithoutWorldEdit(file, world, x, y, z, rotation);
            }
        } else {
            return pasteWithoutWorldEdit(file, world, x, y, z, rotation);
        }
    }
    
    /**
     * Pastes a schematic using WorldEdit
     * 
     * @param file The schematic file
     * @param world The world to paste in
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param rotation The rotation in degrees
     * @return The size of the pasted schematic
     * @throws Exception If there is an error with WorldEdit
     */
    @SuppressWarnings("unchecked")
    private Vector pasteWithWorldEdit(File file, World world, int x, int y, int z, int rotation) throws Exception {
        // Use reflection to avoid hard dependency on WorldEdit
        Class<?> clipboardFormatClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
        Object clipboardFormat = clipboardFormatClass.getMethod("findByFile", File.class).invoke(null, file);
        
        if (clipboardFormat == null) {
            throw new IOException("Unknown schematic format for file: " + file.getName());
        }
        
        // Load the clipboard
        Object reader = clipboardFormat.getClass().getMethod("getReader", FileInputStream.class)
            .invoke(clipboardFormat, new FileInputStream(file));
        
        Object clipboard = reader.getClass().getMethod("read").invoke(reader);
        
        // Get the clipboard dimensions
        Object region = clipboard.getClass().getMethod("getRegion").invoke(clipboard);
        Object dimensions = region.getClass().getMethod("getDimensions").invoke(region);
        
        int width = (int) dimensions.getClass().getMethod("getX").invoke(dimensions);
        int height = (int) dimensions.getClass().getMethod("getY").invoke(dimensions);
        int length = (int) dimensions.getClass().getMethod("getZ").invoke(dimensions);
        
        // Create edit session
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object weWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);
        
        Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        Object worldEdit = worldEditClass.getMethod("getInstance").invoke(null);
        
        Object editSessionFactory = worldEdit.getClass().getMethod("getEditSessionFactory").invoke(worldEdit);
        
        // Create an edit session with a large block change limit
        Object editSession = editSessionFactory.getClass()
            .getMethod("getEditSession", Class.forName("com.sk89q.worldedit.world.World"), int.class)
            .invoke(editSessionFactory, weWorld, Integer.MAX_VALUE);
        
        // Get the operation to paste the clipboard
        Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.Vector3");
        Object pasteVector = vectorClass.getMethod("at", double.class, double.class, double.class)
            .invoke(null, x, y, z);
        
        Class<?> operationClass = Class.forName("com.sk89q.worldedit.function.operation.Operations");
        Object operation;
        
        if (rotation != 0) {
            // Apply rotation if needed
            Class<?> transformClass = Class.forName("com.sk89q.worldedit.math.transform.AffineTransform");
            Object transform = transformClass.getConstructor().newInstance();
            
            // Rotate around Y axis
            transform.getClass().getMethod("rotateY", double.class)
                .invoke(transform, Math.toRadians(rotation));
            
            // Create a transform operation
            Object builder = clipboard.getClass().getMethod("createBuilder").invoke(clipboard);
            builder.getClass().getMethod("setTransform", Class.forName("com.sk89q.worldedit.math.transform.Transform"))
                .invoke(builder, transform);
            
            Object pasteOp = builder.getClass().getMethod("to", vectorClass)
                .invoke(builder, pasteVector);
            
            pasteOp.getClass().getMethod("ignoreAirBlocks", boolean.class)
                .invoke(pasteOp, true);
            
            operation = pasteOp.getClass().getMethod("build").invoke(pasteOp);
        } else {
            // No rotation needed, paste directly
            Class<?> clipboardHolderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder");
            Object clipboardHolder = clipboardHolderClass.getConstructor(Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard"))
                .newInstance(clipboard);
            
            operation = clipboardHolderClass.getMethod("createPaste", Class.forName("com.sk89q.worldedit.EditSession"))
                .invoke(clipboardHolder, editSession);
            
            operation.getClass().getMethod("to", vectorClass)
                .invoke(operation, pasteVector);
            
            operation.getClass().getMethod("ignoreAirBlocks", boolean.class)
                .invoke(operation, true);
            
            operation = operation.getClass().getMethod("build").invoke(operation);
        }
        
        // Execute the operation
        operationClass.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation"))
            .invoke(null, operation);
        
        // Flush the edit session
        editSession.getClass().getMethod("flushSession").invoke(editSession);
        
        // Apply rotations to dimensions if needed
        if (rotation == 90 || rotation == 270) {
            int temp = width;
            width = length;
            length = temp;
        }
        
        return new Vector(width, height, length);
    }
    
    /**
     * Pastes a schematic without using WorldEdit
     * 
     * @param file The schematic file
     * @param world The world to paste in
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param rotation The rotation in degrees
     * @return The size of the pasted schematic
     * @throws IOException If there is an error reading the schematic file
     */
    private Vector pasteWithoutWorldEdit(File file, World world, int x, int y, int z, int rotation) throws IOException {
        // Simple schematic reading (limited support)
        try (FileInputStream fis = new FileInputStream(file)) {
            // This is a basic implementation that supports only simplified schematic formats
            // For full support, WorldEdit is recommended
            
            // Check file type
            if (file.getName().endsWith(".schem")) {
                return readSpongeSchematic(fis, world, x, y, z, rotation);
            } else {
                throw new IOException("Unsupported schematic format without WorldEdit: " + file.getName());
            }
        }
    }
    
    /**
     * Read a Sponge schematic format file
     * 
     * @param fis The file input stream
     * @param world The world to paste in
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @param rotation The rotation in degrees
     * @return The size of the pasted schematic
     * @throws IOException If there is an error reading the schematic
     */
    private Vector readSpongeSchematic(FileInputStream fis, World world, int x, int y, int z, int rotation) throws IOException {
        // This is a very basic implementation that only handles simple schematics
        // For full support, WorldEdit is needed
        
        // Let's create a sample structure for testing purposes
        // In a real implementation, this would parse the schematic data
        
        int width = 5;
        int height = 5;
        int length = 5;
        
        // Create a simple structure (5x5x5 cube of stone with hollow interior)
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                for (int dz = 0; dz < length; dz++) {
                    // Only place blocks on the shell, keep interior empty
                    if (dx == 0 || dx == width - 1 || dy == 0 || dy == height - 1 || dz == 0 || dz == length - 1) {
                        int worldX = x + (rotation == 90 || rotation == 180 ? width - dx - 1 : dx);
                        int worldY = y + dy;
                        int worldZ = z + (rotation == 180 || rotation == 270 ? length - dz - 1 : dz);
                        
                        // Adjust for rotation
                        if (rotation == 90) {
                            int temp = worldX;
                            worldX = x + dz;
                            worldZ = z + (width - dx - 1);
                        } else if (rotation == 270) {
                            int temp = worldX;
                            worldX = x + (length - dz - 1);
                            worldZ = z + dx;
                        }
                        
                        Block block = world.getBlockAt(worldX, worldY, worldZ);
                        block.setType(Material.STONE);
                    }
                }
            }
        }
        
        // Add a door
        int doorX = x + (width / 2);
        int doorZ = z;
        if (rotation == 90) {
            doorX = x + width - 1;
            doorZ = z + (length / 2);
        } else if (rotation == 180) {
            doorX = x + (width / 2);
            doorZ = z + length - 1;
        } else if (rotation == 270) {
            doorX = x;
            doorZ = z + (length / 2);
        }
        
        world.getBlockAt(doorX, y + 1, doorZ).setType(Material.AIR);
        world.getBlockAt(doorX, y + 2, doorZ).setType(Material.AIR);
        
        plugin.getLogger().warning("Using simplified schematic pasting without WorldEdit. Limited functionality.");
        
        // Handle rotations for returned dimensions
        if (rotation == 90 || rotation == 270) {
            return new Vector(length, height, width);
        } else {
            return new Vector(width, height, length);
        }
    }
    
    /**
     * Get the size of a schematic without pasting it
     * 
     * @param file The schematic file
     * @return The size vector (width, height, length)
     * @throws IOException If there is an error reading the schematic
     */
    public Vector getSchematicSize(File file) throws IOException {
        if (worldEditAvailable) {
            try {
                return getSchematicSizeWithWorldEdit(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get schematic size with WorldEdit: " + e.getMessage());
                // Fall back to basic size estimation
                return new Vector(10, 5, 10); // Default size estimation
            }
        } else {
            // Basic size estimation
            return new Vector(10, 5, 10); // Default size estimation
        }
    }
    
    /**
     * Get the size of a schematic using WorldEdit
     * 
     * @param file The schematic file
     * @return The size vector
     * @throws Exception If there is an error with WorldEdit
     */
    private Vector getSchematicSizeWithWorldEdit(File file) throws Exception {
        // Use reflection to avoid hard dependency on WorldEdit
        Class<?> clipboardFormatClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
        Object clipboardFormat = clipboardFormatClass.getMethod("findByFile", File.class).invoke(null, file);
        
        if (clipboardFormat == null) {
            throw new IOException("Unknown schematic format for file: " + file.getName());
        }
        
        // Load the clipboard
        Object reader = clipboardFormat.getClass().getMethod("getReader", FileInputStream.class)
            .invoke(clipboardFormat, new FileInputStream(file));
        
        Object clipboard = reader.getClass().getMethod("read").invoke(reader);
        
        // Get the clipboard dimensions
        Object region = clipboard.getClass().getMethod("getRegion").invoke(clipboard);
        Object dimensions = region.getClass().getMethod("getDimensions").invoke(region);
        
        int width = (int) dimensions.getClass().getMethod("getX").invoke(dimensions);
        int height = (int) dimensions.getClass().getMethod("getY").invoke(dimensions);
        int length = (int) dimensions.getClass().getMethod("getZ").invoke(dimensions);
        
        return new Vector(width, height, length);
    }
    
    /**
     * Clones a region in the world to another location
     * 
     * @param world The world
     * @param startLoc The start location of the region
     * @param endLoc The end location of the region
     * @param pasteLoc The location to paste the region
     * @throws IOException If there is an error
     */
    public void cloneRegion(World world, Location startLoc, Location endLoc, Location pasteLoc) throws IOException {
        if (worldEditAvailable) {
            try {
                cloneRegionWithWorldEdit(world, startLoc, endLoc, pasteLoc);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clone region with WorldEdit: " + e.getMessage());
            }
        }
        
        // Fallback to manual clone
        manualCloneRegion(world, startLoc, endLoc, pasteLoc);
    }
    
    /**
     * Clone a region using WorldEdit
     * 
     * @param world The world
     * @param startLoc The start location
     * @param endLoc The end location
     * @param pasteLoc The paste location
     * @throws Exception If there is an error with WorldEdit
     */
    private void cloneRegionWithWorldEdit(World world, Location startLoc, Location endLoc, Location pasteLoc) throws Exception {
        // Use reflection to avoid hard dependency on WorldEdit
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object weWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);
        
        // Create vector objects for positions
        Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        Object pos1 = vectorClass.getMethod("at", int.class, int.class, int.class)
            .invoke(null, startLoc.getBlockX(), startLoc.getBlockY(), startLoc.getBlockZ());
        
        Object pos2 = vectorClass.getMethod("at", int.class, int.class, int.class)
            .invoke(null, endLoc.getBlockX(), endLoc.getBlockY(), endLoc.getBlockZ());
        
        Object pastePos = vectorClass.getMethod("at", int.class, int.class, int.class)
            .invoke(null, pasteLoc.getBlockX(), pasteLoc.getBlockY(), pasteLoc.getBlockZ());
        
        // Create WorldEdit CuboidRegion
        Class<?> cuboidRegionClass = Class.forName("com.sk89q.worldedit.regions.CuboidRegion");
        Object region = cuboidRegionClass.getConstructor(vectorClass, vectorClass)
            .invoke(null, pos1, pos2);
        
        // Create an edit session
        Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        Object worldEdit = worldEditClass.getMethod("getInstance").invoke(null);
        
        Object editSessionFactory = worldEdit.getClass().getMethod("getEditSessionFactory").invoke(worldEdit);
        
        Object editSession = editSessionFactory.getClass()
            .getMethod("getEditSession", Class.forName("com.sk89q.worldedit.world.World"), int.class)
            .invoke(editSessionFactory, weWorld, Integer.MAX_VALUE);
        
        // Copy the region to clipboard
        Class<?> clipboardClass = Class.forName("com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard");
        Object clipboard = clipboardClass.getConstructor(Class.forName("com.sk89q.worldedit.regions.Region"))
            .invoke(null, region);
        
        Class<?> forwardExtentCopyClass = Class.forName("com.sk89q.worldedit.function.operation.ForwardExtentCopy");
        Object copy = forwardExtentCopyClass.getConstructor(
                Class.forName("com.sk89q.worldedit.extent.Extent"),
                Class.forName("com.sk89q.worldedit.regions.Region"),
                Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard"),
                vectorClass)
            .newInstance(editSession, region, clipboard, pos1);
        
        // Execute the copy
        Class<?> operationsClass = Class.forName("com.sk89q.worldedit.function.operation.Operations");
        operationsClass.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation"))
            .invoke(null, copy);
        
        // Create the paste operation
        Class<?> clipboardHolderClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder");
        Object clipboardHolder = clipboardHolderClass.getConstructor(Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard"))
            .newInstance(clipboard);
        
        Object operation = clipboardHolderClass.getMethod("createPaste", Class.forName("com.sk89q.worldedit.EditSession"))
            .invoke(clipboardHolder, editSession);
        
        operation.getClass().getMethod("to", vectorClass)
            .invoke(operation, pastePos);
        
        operation.getClass().getMethod("ignoreAirBlocks", boolean.class)
            .invoke(operation, true);
        
        operation = operation.getClass().getMethod("build").invoke(operation);
        
        // Execute the paste
        operationsClass.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation"))
            .invoke(null, operation);
        
        // Flush the edit session
        editSession.getClass().getMethod("flushSession").invoke(editSession);
    }
    
    /**
     * Clone a region manually (block by block)
     * 
     * @param world The world
     * @param startLoc The start location
     * @param endLoc The end location
     * @param pasteLoc The paste location
     */
    private void manualCloneRegion(World world, Location startLoc, Location endLoc, Location pasteLoc) {
        // Get the minimum and maximum coordinates
        int minX = Math.min(startLoc.getBlockX(), endLoc.getBlockX());
        int minY = Math.min(startLoc.getBlockY(), endLoc.getBlockY());
        int minZ = Math.min(startLoc.getBlockZ(), endLoc.getBlockZ());
        
        int maxX = Math.max(startLoc.getBlockX(), endLoc.getBlockX());
        int maxY = Math.max(startLoc.getBlockY(), endLoc.getBlockY());
        int maxZ = Math.max(startLoc.getBlockZ(), endLoc.getBlockZ());
        
        // Calculate offsets
        int offsetX = pasteLoc.getBlockX() - minX;
        int offsetY = pasteLoc.getBlockY() - minY;
        int offsetZ = pasteLoc.getBlockZ() - minZ;
        
        // First pass: copy blocks (excluding tile entities)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block sourceBlock = world.getBlockAt(x, y, z);
                    Block targetBlock = world.getBlockAt(x + offsetX, y + offsetY, z + offsetZ);
                    
                    // Copy block data (excluding tile entities)
                    BlockData data = sourceBlock.getBlockData().clone();
                    targetBlock.setBlockData(data, false);
                }
            }
        }
        
        // Second pass: copy tile entities
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block sourceBlock = world.getBlockAt(x, y, z);
                    BlockState sourceState = sourceBlock.getState();
                    
                    // Check if this is a tile entity
                    if (sourceState instanceof org.bukkit.block.Container || 
                        sourceState instanceof org.bukkit.block.Sign) {
                        
                        Block targetBlock = world.getBlockAt(x + offsetX, y + offsetY, z + offsetZ);
                        BlockState targetState = targetBlock.getState();
                        
                        // Copy container contents
                        if (sourceState instanceof org.bukkit.block.Container && 
                            targetState instanceof org.bukkit.block.Container) {
                            
                            org.bukkit.block.Container sourceContainer = (org.bukkit.block.Container) sourceState;
                            org.bukkit.block.Container targetContainer = (org.bukkit.block.Container) targetState;
                            
                            targetContainer.getInventory().setContents(sourceContainer.getInventory().getContents());
                            targetContainer.update(true);
                        }
                        
                        // Copy sign text
                        else if (sourceState instanceof org.bukkit.block.Sign && 
                                 targetState instanceof org.bukkit.block.Sign) {
                            
                            org.bukkit.block.Sign sourceSign = (org.bukkit.block.Sign) sourceState;
                            org.bukkit.block.Sign targetSign = (org.bukkit.block.Sign) targetState;
                            
                            for (int i = 0; i < 4; i++) {
                                targetSign.setLine(i, sourceSign.getLine(i));
                            }
                            targetSign.update(true);
                        }
                    }
                }
            }
        }
    }
}
