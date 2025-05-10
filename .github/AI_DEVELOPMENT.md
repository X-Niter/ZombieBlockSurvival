# SevenToDie Autonomous Development System

This document explains the autonomous AI-powered development system that manages the SevenToDie Minecraft plugin project.

## How It Works

The autonomous development system consists of several interconnected components:

1. **GitHub Actions Workflows**: Automated processes that run on specific triggers (issues, PRs, schedules)
2. **AI Scripts**: Python scripts that leverage OpenAI's API to analyze code and generate fixes/improvements
3. **Dashboard**: Web interface to monitor the autonomous system's activities
4. **Issue/PR Interaction**: AI-powered responses to community engagement

![Autonomous Development System](../docs/images/ai-system-diagram.png)

## System Components

### 1. Issue Triage and Response

When a new issue is created:
- The system automatically analyzes the issue content
- It categorizes the issue (bug, feature request, question, etc.)
- It applies appropriate labels
- It responds with relevant questions or information
- It tags issues that the AI can potentially fix with `ai-fix`

### 2. Self-Test and Fix

The system regularly tests the codebase:
- Runs automated tests, checkstyle, and spotbugs
- If issues are detected, generates analysis and fix suggestions
- Creates issues with the `ai-fix` label for fixable problems
- These issues then trigger the AI Implementation workflow

### 3. AI Implementation

When an issue is labeled with `ai-fix`:
- The system analyzes the issue and relevant code files
- It generates code changes to fix the issue
- It creates a new branch with the changes
- It submits a pull request with detailed explanation
- It logs the fix attempt to the dashboard

### 4. PR Conversation

When comments are made on PRs:
- The system reads and analyzes the comment content
- It generates helpful, contextually relevant responses
- It can explain its implementation choices and reasoning
- It maintains a conversation history for appropriate context

## Commands

The AI responds to special commands in issue comments:

| Command | Description |
|---------|-------------|
| `/ai fix` | Request a bug fix |
| `/ai implement` | Request a feature implementation |
| `/ai analyze` | Request detailed analysis |
| `/ai status` | Check implementation status |
| `/ai help` | Show all available commands |

## Setup

### Required Secrets

To set up the autonomous development system, you need to add these secrets to your GitHub repository:

1. `OPENAI_API_KEY`: Your OpenAI API key for GPT-4 access
2. `GITHUB_TOKEN`: Automatically provided by GitHub Actions

### Dashboard Deployment

The dashboard is automatically deployed to GitHub Pages when:
- Dashboard data is updated (after AI activities)
- The dashboard code is modified
- A manual deployment is triggered

## System Limitations

The autonomous system has some limitations:

- It works best with well-described issues with clear context
- Complex architectural changes may require human guidance
- It cannot access or modify private repositories or external systems
- It operates within GitHub API rate limits

## Contributing to the AI System

You can help improve the autonomous system by:

1. Opening issues about AI behavior that needs adjustment
2. Suggesting new AI capabilities or commands
3. Improving the AI scripts in the `.github/scripts/` directory
4. Enhancing the dashboard in the `dashboard/` directory

## Monitoring and Metrics

The dashboard provides insights into:
- Number of issues triaged and categorized
- PR generation success rate
- Code quality improvements
- Response times and interaction quality

Access the dashboard through GitHub Pages when deployed.

## Future Improvements

The autonomous development system is continuously evolving. Planned improvements include:

- Enhanced analysis of complex code patterns
- Better understanding of Minecraft/Paper API constraints
- Integration with automated testing frameworks
- More advanced self-improvement capabilities