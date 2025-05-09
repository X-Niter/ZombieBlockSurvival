package com.seventodie.commands.quest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.commands.BaseCommand;
import com.seventodie.quests.Quest;
import com.seventodie.quests.QuestManager;
import com.seventodie.quests.QuestManager.QuestTargetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Command for managing quests.
 */
public class QuestCommand extends BaseCommand {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "create", "complete", "reset", "info");
    private static final List<String> TARGET_TYPES = Arrays.asList("kill_zombies", "collect_items", "clear_building", "dig_resources");
    
    public QuestCommand(SevenToDiePlugin plugin) {
        super(plugin, "quest", "seventodie.quest", false);
    }
    
    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return listQuests(sender, args);
            case "create":
                return createQuest(sender, args);
            case "complete":
                return completeQuest(sender, args);
            case "reset":
                return resetQuest(sender, args);
            case "info":
                return questInfo(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * List all quests
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean listQuests(CommandSender sender, String[] args) {
        List<Quest> quests;
        
        if (args.length > 1 && args[1].equalsIgnoreCase("active") && sender instanceof Player) {
            Player player = (Player) sender;
            quests = plugin.getQuestManager().getPlayerActiveQuests(player);
            sender.sendMessage(ChatColor.GOLD + "===== Your Active Quests (" + quests.size() + ") =====");
        } else {
            quests = plugin.getQuestManager().getAllQuests();
            sender.sendMessage(ChatColor.GOLD + "===== All Quests (" + quests.size() + ") =====");
        }
        
        if (quests.isEmpty()) {
            sendInfo(sender, "No quests found.");
            return true;
        }
        
        for (Quest quest : quests) {
            String status = quest.isCompleted() ? ChatColor.GREEN + "Completed" : ChatColor.YELLOW + "Active";
            sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + quest.getTitle() + " " + 
                             ChatColor.GRAY + "(" + quest.getId() + ") " + status);
        }
        
        return true;
    }
    
    /**
     * Create a new quest
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean createQuest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendError(sender, "This command can only be run by a player.");
            return true;
        }
        
        if (args.length < 4) {
            sendError(sender, "Usage: /quest create <type> <target> <title>");
            sendInfo(sender, "Types: kill_zombies, collect_items, clear_building, dig_resources");
            sendInfo(sender, "Example: /quest create kill_zombies 10 \"Clear the Neighborhood\"");
            return true;
        }
        
        Player player = (Player) sender;
        String typeStr = args[1].toUpperCase();
        int targetAmount;
        
        try {
            targetAmount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sendError(sender, "Target amount must be a number.");
            return true;
        }
        
        // Build the title from the remaining arguments
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) titleBuilder.append(" ");
            titleBuilder.append(args[i]);
        }
        String title = titleBuilder.toString();
        
        // Parse quest type
        QuestTargetType targetType;
        try {
            targetType = QuestTargetType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid quest type. Valid types: kill_zombies, collect_items, clear_building, dig_resources");
            return true;
        }
        
        // Create quest at player location
        try {
            UUID questId = UUID.randomUUID();
            String description = "A custom quest: " + title;
            
            Quest quest = new Quest(questId, title, description, targetType, targetAmount, player.getLocation(), null);
            plugin.getQuestManager().registerQuest(quest);
            
            sendSuccess(sender, "Created new quest: " + title);
            sendInfo(sender, "ID: " + questId);
        } catch (Exception e) {
            sendError(sender, "Error creating quest: " + e.getMessage());
            plugin.getLogger().severe("Error creating quest: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Complete a quest
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean completeQuest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /quest complete <id>");
            return true;
        }
        
        String idString = args[1];
        UUID id;
        
        try {
            id = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid UUID format.");
            return true;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(id);
        if (quest == null) {
            sendError(sender, "Could not find a quest with the ID: " + idString);
            return true;
        }
        
        // Mark as completed
        plugin.getQuestManager().completeQuest(id);
        
        sendSuccess(sender, "Marked quest \"" + quest.getTitle() + "\" as completed.");
        return true;
    }
    
    /**
     * Reset a quest
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean resetQuest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /quest reset <id>");
            return true;
        }
        
        String idString = args[1];
        UUID id;
        
        try {
            id = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid UUID format.");
            return true;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(id);
        if (quest == null) {
            sendError(sender, "Could not find a quest with the ID: " + idString);
            return true;
        }
        
        // Reset the quest
        plugin.getQuestManager().resetQuest(id);
        
        sendSuccess(sender, "Reset quest \"" + quest.getTitle() + "\".");
        return true;
    }
    
    /**
     * Display information about a quest
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean questInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /quest info <id>");
            return true;
        }
        
        String idString = args[1];
        UUID id;
        
        try {
            id = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid UUID format.");
            return true;
        }
        
        Quest quest = plugin.getQuestManager().getQuest(id);
        if (quest == null) {
            sendError(sender, "Could not find a quest with the ID: " + idString);
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "===== Quest Info =====");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + quest.getId());
        sender.sendMessage(ChatColor.YELLOW + "Title: " + ChatColor.WHITE + quest.getTitle());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + quest.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + quest.getTargetType().name());
        sender.sendMessage(ChatColor.YELLOW + "Target: " + ChatColor.WHITE + quest.getTargetAmount());
        sender.sendMessage(ChatColor.YELLOW + "Status: " + 
                         (quest.isCompleted() ? ChatColor.GREEN + "Completed" : ChatColor.YELLOW + "Active"));
        
        if (quest.getStructureId() != null) {
            sender.sendMessage(ChatColor.YELLOW + "Structure: " + ChatColor.WHITE + quest.getStructureId());
        }
        
        Location loc = quest.getLocation();
        sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + 
                         loc.getWorld().getName() + ", " + 
                         loc.getBlockX() + ", " + 
                         loc.getBlockY() + ", " + 
                         loc.getBlockZ());
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletions(args, SUBCOMMANDS);
        }
        
        // Handle subcommand tab completions
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("create")) {
                return filterTabCompletions(args, TARGET_TYPES);
            } else if (subCommand.equals("complete") || subCommand.equals("reset") || subCommand.equals("info")) {
                // Complete with quest IDs
                List<String> questIds = new ArrayList<>();
                for (Quest quest : plugin.getQuestManager().getAllQuests()) {
                    questIds.add(quest.getId().toString());
                }
                return filterTabCompletions(args, questIds);
            } else if (subCommand.equals("list")) {
                return filterTabCompletions(args, Arrays.asList("active", "all"));
            }
        }
        
        // For the create command, provide target amount suggestions
        if (args.length == 3 && args[0].toLowerCase().equals("create")) {
            return filterTabCompletions(args, Arrays.asList("5", "10", "15", "20"));
        }
        
        return new ArrayList<>();
    }
}