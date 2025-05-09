SevenToDie Sounds Directory
===========================

This directory contains sound files for the SevenToDie plugin, primarily for trader voice lines
and ambient sounds to enhance the 7 Days to Die experience in Minecraft.

Sound Format Requirements:
-------------------------
- All sound files must be in OGG format (.ogg)
- Recommended sample rate: 44.1 kHz
- Recommended bit depth: 16-bit
- Keep file sizes small (under 1MB per file) for better performance

Directory Structure:
------------------
- traders/ - Voice lines for trader NPCs
  - greetings/ - Greetings when players approach traders
  - farewells/ - Farewells when players leave traders
  - day_ending/ - Lines when traders are closing for the night
  - quest_completed/ - Lines when players complete quests
  
- ambient/ - Environmental and ambient sounds
  - wasteland/ - Sounds for wasteland biomes
  - forest/ - Sounds for forest biomes
  - desert/ - Sounds for desert biomes
  - snow/ - Sounds for snow biomes

- blocks/ - Block-related sounds
  - frame/ - Sounds for frame block placement and upgrades
  - tools/ - Sounds for tool usage

Naming Convention:
----------------
Use the following naming pattern for sound files:
[category]_[type]_[variant].ogg

Example:
trader_greeting_01.ogg
trader_greeting_02.ogg
ambient_wasteland_wind_01.ogg
block_frame_upgrade_01.ogg

Adding Custom Sounds:
-------------------
1. Create or obtain the sound file in OGG format
2. Place it in the appropriate directory
3. Update sounds.json if needed (for resource pack integration)
4. Restart the server or use /seventodie reload

Resource Pack Integration:
------------------------
The plugin automatically registers these sounds with Minecraft's sound system, but
for best results, include these sounds in a resource pack and define them in sounds.json.

Example sounds.json entry:
{
  "seventodie.trader.greeting": {
    "sounds": [
      "seventodie:traders/greetings/trader_greeting_01",
      "seventodie:traders/greetings/trader_greeting_02"
    ]
  }
}

Legal Considerations:
-------------------
Ensure you have the legal right to use any sounds you add to the plugin. Only use:
- Sounds you've created yourself
- Sounds with appropriate licenses (public domain, Creative Commons, etc.)
- Sounds you've purchased the rights to use

Do not use copyrighted sounds from 7 Days to Die or other games without permission.

For more information, visit the wiki or contact the plugin author.
