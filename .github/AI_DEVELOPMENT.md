# AI-Powered Development for SevenToDie Plugin

This document explains how to use the AI-powered development features in this repository. The system can automatically analyze code, fix bugs, implement features, and improve performance using AI.

> **Note**: Before using these features, you need to set up the required secrets and permissions. See [Setup Instructions](.github/SETUP_AI_DEVELOPMENT.md) for details.

## How It Works

The AI development system uses GitHub Actions workflows combined with OpenAI's GPT models to:

1. Analyze code and issues automatically
2. Generate fixes for bugs and implementation for features
3. Create pull requests with the changes
4. Provide detailed analysis as comments on issues

## Using AI Development Features

### Automatic Issue Analysis

When you create a new issue, the AI assistant will:

1. Analyze the issue description
2. Review the relevant code files
3. Post a comment with its analysis
4. If possible, create a pull request with a fix

The analysis includes:
- Summary of what the code does
- Identified bugs or issues
- Recommended fixes with code snippets
- General recommendations for improvement

### Manually Triggering AI Tasks

You can manually trigger AI development tasks through the GitHub UI:

1. Go to the "Actions" tab in the repository
2. Select the "AI Development Assistant" workflow
3. Click "Run workflow"
4. Choose a task:
   - `analyze-code`: General code review and analysis
   - `fix-bugs`: Focus on finding and fixing bugs
   - `implement-feature`: Implement a feature based on your description
   - `improve-performance`: Optimize code for better performance
5. Provide a description of what you want
6. Submit the workflow

### AI Commands in Issues

You can give commands to the AI assistant by adding comments to issues:

- `@ai analyze`: Analyze the code related to this issue
- `@ai fix`: Try to fix the problem described in the issue
- `@ai implement`: Implement the feature described in the issue
- `@ai optimize`: Optimize the code described in the issue

## Setup Requirements

To use the AI-powered development features, you need to:

1. Add the `OPENAI_API_KEY` secret to your repository:
   - Go to repository Settings → Secrets → Actions
   - Add a new secret named `OPENAI_API_KEY` with your OpenAI API key

2. Ensure the GitHub Actions workflows have permission to create pull requests:
   - Go to Settings → Actions → General
   - Under "Workflow permissions", select "Read and write permissions"
   - Check "Allow GitHub Actions to create and approve pull requests"

## Limitations

The AI assistant has some limitations:

- It can only analyze a limited number of files per request (currently 15)
- Large files will be truncated to stay within token limits
- It may not understand complex architectural decisions or project-specific conventions
- The generated code may require human review and adjustments
- Not all bugs can be automatically fixed

## Development Process

For best results with the AI development workflow:

1. Create detailed issue descriptions with clear explanations of the problem or feature
2. When using manual triggers, provide comprehensive descriptions of what you want
3. Always review the pull requests created by the AI before merging
4. Provide feedback on the AI's work to help improve future iterations

## Example

Here's an example of a good issue that the AI can process effectively:

```
Title: Bug: Frame blocks don't save durability values

Description:
When a player damages a frame block, the durability value changes in-game but
doesn't persist after a server restart. I've noticed this happens specifically
with wooden and stone frame blocks.

Steps to reproduce:
1. Place a wooden frame block
2. Hit it several times with any tool
3. Observe the durability meter decreasing
4. Restart the server
5. The durability is reset to full

The issue is probably in the BlockManager.java file where it loads and saves
block data. The durability value might not be getting saved to the database.
```

This provides clear context that helps the AI generate an appropriate fix.