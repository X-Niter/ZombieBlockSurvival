# Repository Structure Guide

This document explains the structure of the SevenToDie repository and how the autonomous development system is organized.

## Overview

The SevenToDie project is organized as a mono-repo containing three main components:

1. **Minecraft Plugin** - The core Java plugin that implements 7 Days To Die gameplay in Minecraft
2. **Dashboard** - A web-based monitoring interface for the autonomous development system
3. **Autonomous System** - AI-powered scripts and workflows that can develop the plugin without human intervention

## Directory Structure

```
SevenToDie/
├── .github/                     # GitHub related files
│   ├── workflows/               # GitHub Actions workflow definitions
│   │   ├── ai_improvement_pr.yml    # Generates PRs from AI fixes
│   │   ├── deploy_dashboard.yml     # Deploys the monitoring dashboard
│   │   ├── issue_triage.yml         # Handles issue categorization
│   │   ├── pr_comment_response.yml  # Responds to PR comments
│   │   ├── plugin_health_check.yml  # Monitors plugin health
│   │   └── self_test_and_fix.yml    # Tests and fixes code issues
│   ├── scripts/                 # AI scripts that power the autonomous system
│   │   ├── ai_command_handler.py    # Handles AI commands in comments
│   │   ├── ai_conversation_responder.py  # Responds to issues and PRs
│   │   ├── ai_implementation.py     # Implements fixes and features
│   │   ├── ai_triage_issue.py       # Analyzes and categorizes issues
│   │   └── self_test_and_fix.py     # Runs code analysis and fixes
│   └── AI_DEVELOPMENT.md        # Documentation for the AI system
├── plugin/                      # Minecraft plugin code
│   ├── src/                     # Source code
│   │   └── main/
│   │       ├── java/            # Java source files
│   │       │   └── com/
│   │       │       └── seventodie/  # Core plugin packages
│   │       └── resources/       # Plugin resources
│   ├── target/                  # Build output (generated)
│   ├── pom.xml                  # Maven project configuration
│   └── dependency-reduced-pom.xml  # Maven shade plugin output
├── dashboard/                   # Web-based dashboard
│   ├── src/                     # Dashboard source code
│   │   ├── pages/               # Next.js pages
│   │   ├── components/          # React components
│   │   └── data/                # JSON data files for dashboard
│   │       ├── ai_interactions.json      # Records of AI responses
│   │       ├── ai_implementations.json   # Records of AI PRs
│   │       ├── issue_triage_logs.json    # Issue triage logs
│   │       ├── health_check_logs.json    # Plugin health logs
│   │       └── self_test_logs.json       # Self-test logs
│   ├── public/                  # Static files
│   ├── package.json             # Node.js dependencies
│   └── next.config.js           # Next.js configuration
├── docs/                        # Documentation
│   └── images/                  # Documentation images
├── server/                      # Local server for testing
├── README.md                    # Main README file
├── CONTRIBUTING.md              # Contribution guidelines
└── DEVELOPMENT.md               # Development setup instructions
```

## Key Components

### 1. Plugin Code (`/plugin`)

The plugin directory contains the Java code for the Minecraft server plugin. It uses Maven for building and dependency management. The core functionality includes:

- Building system with frame blocks
- Zombie AI and horde mechanics
- Trader NPCs and quests
- Custom tools and weapons
- World generation with roads and structures
- Database system for persistence

### 2. Dashboard (`/dashboard`)

The dashboard is a Next.js web application that provides monitoring and visualization for the autonomous development system. It displays:

- AI activity statistics
- Issue triage results
- Self-test logs
- Plugin health metrics
- Implementation success rates

The dashboard is automatically deployed to GitHub Pages whenever data is updated or the dashboard code changes.

### 3. Autonomous System (`.github`)

The autonomous development system consists of GitHub Actions workflows and AI scripts that work together to:

- Respond to issues and PRs in a helpful, conversational manner
- Triage new issues with appropriate labels
- Fix bugs automatically by generating PRs
- Implement requested features
- Self-test the codebase to detect issues
- Monitor the plugin's health in a test environment

## Workflow Interactions

1. **Issue Creation**
   - When a new issue is created, `issue_triage.yml` runs `ai_triage_issue.py`
   - The script analyzes the issue, adds labels, and posts an initial response
   - The interaction is logged to the dashboard

2. **AI Commands**
   - Users can comment with commands like `/ai fix` or `/ai analyze`
   - `ai_command_handler.yml` processes these commands
   - The appropriate action is taken and results logged

3. **Self-Testing**
   - `self_test_and_fix.yml` runs daily to check for code issues
   - If issues are found, they are logged and issues are created
   - The AI can then fix these issues automatically

4. **Plugin Health Monitoring**
   - `plugin_health_check.yml` runs daily to test the plugin
   - It starts a test server and monitors for errors
   - Results are logged to the dashboard
   - If issues are found, an issue is created

5. **AI-Generated Fixes**
   - When an issue has the `ai-fix` label, `ai_implementation.py` generates a fix
   - It creates a new branch with the changes
   - It submits a PR with a detailed explanation
   - The PR is logged to the dashboard

## Contributing to the Autonomous System

When making changes to the autonomous system:

1. **Updating AI Scripts**
   - Modify files in `.github/scripts/`
   - Test changes thoroughly
   - Ensure all dependencies are properly specified in workflows

2. **Updating Dashboard**
   - Modify files in `dashboard/`
   - Test locally with `npm run dev`
   - Deploy using the manual trigger in GitHub Actions

3. **Adding New Capabilities**
   - Create new script in `.github/scripts/`
   - Add corresponding workflow in `.github/workflows/`
   - Update dashboard to display new data
   - Update documentation