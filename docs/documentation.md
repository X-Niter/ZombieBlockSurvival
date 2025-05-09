---
layout: default
title: Documentation
---

# SevenToDie AI Development Documentation

This documentation explains how to use the AI-powered development features in the SevenToDie plugin project.

## Dashboard Features

The AI Development Dashboard provides a user-friendly interface to interact with the AI development features:

### Authentication

- The dashboard requires authentication with a GitHub personal access token
- Tokens need `repo` and `workflow` scopes to trigger workflows and create issues
- Tokens are stored only in your browser session for security

### Triggering AI Tasks

You can trigger four types of AI tasks:

1. **Analyze Code**: Reviews the codebase and provides insights
   - Example: "Analyze the BlockManager class for code quality issues"

2. **Fix Bugs**: Attempts to find and fix specific bugs
   - Example: "Fix the issue where frame blocks don't save durability values"

3. **Implement Feature**: Adds new functionality based on your description
   - Example: "Implement a blood moon event that increases zombie spawns every 7 days"

4. **Improve Performance**: Optimizes code for better efficiency
   - Example: "Optimize the StructureManager to reduce lag when generating new structures"

### Creating Issues

You can create issues directly from the dashboard:

- **Bug Reports**: Describe a bug that needs fixing
- **Feature Requests**: Request a new feature to be implemented
- **Other Issues**: Any other type of task or question

The AI will automatically analyze new issues and:
1. Comment with its analysis
2. Create a pull request with fixes (when possible)

### Viewing Results

The dashboard shows:

- Recent AI tasks with their status
- Recent issues with AI response status
- Links to detailed workflow runs and pull requests

## Using AI Commands in Issues

You can also interact with the AI by adding comments to issues:

- `@ai analyze`: Analyze the code related to this issue
- `@ai fix`: Try to fix the problem described in the issue
- `@ai implement`: Implement the feature described in the issue
- `@ai optimize`: Optimize the code described in the issue

Example:
```
I've noticed that the plugin is using a lot of CPU when many zombies spawn.

@ai optimize Please analyze the zombie spawning code and optimize it for better performance.
```

## AI Workflow Details

The AI development workflow:

1. Analyzes the issue or task description
2. Examines the relevant code files
3. Generates an analysis with:
   - Summary of findings
   - Identified problems
   - Recommended solutions
4. Creates a pull request with fixes when appropriate
5. Comments on the issue with its analysis

The AI can access:
- Source code files
- Issue descriptions and comments
- Prior pull requests and commits

## Creating Effective Task Descriptions

For best results, provide clear and detailed task descriptions:

### Good Examples:

```
Fix the bug where wooden frame blocks don't save their durability after a server restart.
The issue seems to be in BlockManager.java where it loads and saves block data.
When a player damages a wooden frame, the durability decreases visually but resets on server restart.
```

```
Implement a tiered weapon system similar to 7 Days To Die.
Weapons should have quality levels (1-6) with different damage multipliers:
- Level 1: 0.8x damage
- Level 2: 0.9x damage
- Level 3: 1.0x damage
- Level 4: 1.1x damage
- Level 5: 1.2x damage
- Level 6: 1.3x damage
Weapons should show their quality level in their name and tooltip.
```

### Poor Examples:

```
Fix the blocks
```

```
Make the game better
```

## Limitations

The AI has some limitations:

- It can only analyze a limited number of files per request (currently 15)
- Large files will be truncated due to token limits
- It may not understand complex architectural decisions
- The generated code may require human review
- Not all bugs can be automatically fixed

## Troubleshooting

If you encounter issues:

- Check workflow run logs in the Actions tab
- Verify that your OpenAI API key is valid
- Make sure GitHub Actions has proper permissions
- For dashboard issues, check the browser console for errors

For more help, create an issue in the repository.