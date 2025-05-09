# SevenToDie Minecraft Plugin

A comprehensive Minecraft server plugin that recreates 7 Days To Die gameplay mechanics on Paper 1.21.4.

## Features

- **Custom Building System**: Frame-based building system with upgradable blocks
- **Zombie Hordes**: Dynamic zombie spawning with increased difficulty over time
- **Trader NPCs**: Special traders that offer quests and items
- **Quest System**: Complete quests to earn rewards and progress
- **Structure Generation**: Custom structures in the world
- **Specialized Tools**: Tools designed for the building system

## Development Setup

This project is configured for easy development using GitHub workflows.

### Building with GitHub Actions

The plugin is automatically built when changes are pushed to the repository. You can view the build status and results in the "Actions" tab of the GitHub repository.

### Interactive Development Environment

You can create an on-demand development environment right in GitHub Actions:

1. Go to the "Actions" tab in your GitHub repository
2. Select the "Development Environment" workflow
3. Click "Run workflow" 
4. Configure options:
   - Set development session duration (60-240 minutes)
   - Choose whether to automatically start a Paper server
5. Click "Run workflow"
6. When the workflow reaches the Tmate step, follow the SSH connection instructions
7. Once connected, you can:
   - Build the plugin: `./build.sh`
   - Start the server (if not auto-started): `./server.sh`
   - Edit code using terminal-based editors like nano or vim
   - Use screen/tmux for multiple terminal sessions

This gives you a complete development environment without needing to install anything locally.

## Local Development Setup

If you prefer local development:

1. Install JDK 21
2. Clone this repository
3. Build with Maven: `mvn package`
4. Copy the resulting JAR from the `target` folder to your Paper server's `plugins` folder

## Project Structure

- `src/main/java/com/seventodie/`: Main plugin code
  - `blocks/`: Building system and frame blocks
  - `commands/`: Command handlers
  - `listeners/`: Event listeners
  - `quests/`: Quest system
  - `tools/`: Tool management
  - `traders/`: Trading system
  - `utils/`: Utility classes
  - `worldgen/`: World generation

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## License

[MIT License](LICENSE)