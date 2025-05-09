SevenToDie Schematics Directory
===============================

This directory contains the schematic files for structures used in the SevenToDie plugin.

Directory Structure:
-------------------
- residential/ - Houses, apartments, and other residential buildings
- commercial/ - Stores, offices, and other commercial buildings
- industrial/ - Factories, warehouses, and other industrial buildings
- trader/ - Trader outposts and related buildings

Supported Formats:
-----------------
- .schem (Sponge Schematic Format) - Preferred
- .schematic (MCEdit Schematic Format) - Legacy support

How to Add Your Own Schematics:
------------------------------
1. Create your structure in a creative world
2. Save it using WorldEdit's //copy and //schematic save commands
   Example: //schematic save residential/my_house
3. Place the schematic file in the appropriate subdirectory
4. Restart the server or use /seventodie reload

Rotation Guidelines:
------------------
- Design your structures with the front entrance facing SOUTH (positive Z)
- The plugin will handle rotation automatically based on road orientation
- Include a clear entrance (door, gate, etc.) for proper placement along roads

Structure Requirements:
---------------------
- Residential structures should include basic living areas and potential loot containers
- Commercial structures should include shop areas and storage containers
- Industrial structures should include machinery, factory elements, and resource areas
- Trader outposts must include a clear area for the trader NPC to spawn

Size Guidelines:
--------------
- Small: 10x10x10 blocks or smaller
- Medium: 10x10x10 to 25x25x25 blocks
- Large: 25x25x25 to 50x50x50 blocks
- Extra Large: Buildings larger than 50x50x50 may cause performance issues

Note: The plugin will automatically calculate the dimensions of your schematic, but
designing structures with these guidelines in mind will ensure better performance.

For more information, visit the wiki or contact the plugin author.
