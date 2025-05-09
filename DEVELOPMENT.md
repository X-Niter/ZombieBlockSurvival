# Development Guide for SevenToDie Plugin

This guide provides detailed instructions for developing the SevenToDie Minecraft plugin using GitHub workflows, without needing to maintain a continuous Replit session.

## GitHub Workflow Development

GitHub Actions provides powerful CI/CD capabilities that we can leverage for development. This project includes two main workflows:

1. **Build and Test**: Automatically builds and tests the plugin when changes are pushed
2. **Development Environment**: Creates an interactive development environment with SSH access

### Using the Development Environment

The Development Environment workflow creates a complete environment with JDK 21, Maven, and a Paper server for testing. Here's how to use it:

#### Starting a Development Session

1. Go to your GitHub repository
2. Click on the "Actions" tab
3. Select "Development Environment" from the workflows list
4. Click "Run workflow" dropdown in the right sidebar
5. Configure your session:
   - **Session Duration**: How long the environment will remain active (60-240 minutes)
   - **Enable Server**: Whether to automatically start a Paper server in the background
6. Click "Run workflow"

#### Connecting to Your Session

When the workflow reaches the Tmate step:

1. You'll see SSH connection details in the workflow logs
2. Copy the SSH command (something like `ssh ...tmate.io`)
3. Open a terminal on your local machine and paste the command
4. You're now connected to the development environment!

#### Working in the Environment

Once connected, you have:

- Full access to the repository code
- JDK 21 and Maven installed
- Helper scripts:
  - `./build.sh`: Builds the plugin and copies it to the server plugins folder
  - `./server.sh`: Starts the Paper server (if it's not running already)

Common workflow:

1. Edit code using terminal-based editors like nano, vim, or emacs
   ```
   nano src/main/java/com/seventodie/SevenToDiePlugin.java
   ```

2. Build the plugin to apply your changes
   ```
   ./build.sh
   ```

3. Test your changes on the server
   ```
   # If server is running in the background
   screen -r minecraft
   
   # To detach from the screen session (return to main terminal)
   # Press Ctrl+A then D
   ```

4. Commit and push your changes
   ```
   git add .
   git commit -m "Description of changes"
   git push
   ```

#### Development Tips

- **Multiple Terminal Sessions**: Use `screen` or `tmux` to create multiple terminal sessions
  ```
  # Create a new screen session
  screen -S coding
  
  # List active sessions
  screen -ls
  
  # Reconnect to a session
  screen -r coding
  ```

- **Extending Session Time**: If you need more time, you can start a new session before the current one expires and continue working

- **Debugging**: Check server logs in `server/logs/latest.log`
  ```
  tail -f server/logs/latest.log
  ```

## CI/CD Pipeline

The Build and Test workflow provides continuous integration:

1. **Build**: Compiles the plugin with Maven
2. **Test on Paper**: Starts a Paper server with the plugin and verifies it loads correctly
3. **Code Quality**: Runs SpotBugs and Checkstyle to ensure code quality

This pipeline runs automatically on push to main/master branches, and can be manually triggered at any time.

## Local Development Setup

If you prefer local development:

1. Install JDK 21 from [Adoptium](https://adoptium.net/)
2. Clone the repository: `git clone https://github.com/yourusername/SevenToDie.git`
3. Open in your favorite IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)
4. Build with Maven: `mvn package`
5. For local testing, setup a Paper server:
   - Download Paper 1.21.4: [Paper Download](https://papermc.io/downloads)
   - Create a server directory with the JAR file
   - Create an `eula.txt` file with `eula=true`
   - Start the server: `java -Xms1G -Xmx1G -jar paper.jar nogui`
   - Copy your plugin JAR to the server's `plugins` folder

## Project Structure

```
com.seventodie
├── SevenToDiePlugin.java       # Main plugin class
├── blocks                      # Building system
│   ├── BlockManager.java
│   └── FrameBlock.java
├── commands                    # Command handlers
│   ├── BaseCommand.java
│   ├── CommandManager.java
│   └── various commands...
├── listeners                   # Event listeners
│   ├── BlockInteractionListener.java
│   ├── PlayerListener.java
│   └── WorldGenListener.java
├── quests                      # Quest system
│   ├── Quest.java
│   └── QuestManager.java
├── tools                       # Tool system
│   └── ToolManager.java
├── traders                     # Trading system
│   ├── TraderManager.java
│   └── TraderNPC.java
├── utils                       # Utilities
│   ├── ConfigManager.java
│   ├── DatabaseManager.java
│   └── SchematicUtils.java
└── worldgen                    # World generation
    ├── BiomeMapper.java
    ├── RoadGenerator.java
    └── StructureManager.java
```

## Implementing Features

When implementing new features:

1. Define clear boundaries between systems
2. Use managers for each subsystem (e.g., BlockManager, QuestManager)
3. Implement event listeners for player interactions
4. Use configuration files for customizable values
5. Add commands for admin and player interactions
6. Document your code with JavaDoc comments

## Troubleshooting

- **Build Fails**: Check the build logs for specific errors
- **Server Crashes**: Check `server/logs/latest.log` for exception details
- **Plugin Doesn't Load**: Verify the plugin.yml file is correct and the main class matches
- **GitHub Actions Issues**: Check if you've exceeded your GitHub Actions minutes quota