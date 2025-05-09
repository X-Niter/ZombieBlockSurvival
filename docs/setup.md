---
layout: default
title: Setup Guide
---

# SevenToDie AI Development Setup Guide

This guide will help you set up the AI-powered development features for your SevenToDie plugin. Follow these steps to enable automated code analysis, bug fixing, and feature implementation.

## Step 1: Repository Setup

First, make sure your GitHub repository is properly configured:

1. Create a new GitHub repository (or use an existing one)
2. Push your SevenToDie plugin code to the repository
3. Enable GitHub Actions in your repository settings:
   - Go to Settings → Actions → General
   - Under "Actions permissions," select "Allow all actions and reusable workflows"
   - Make sure "Allow GitHub Actions to create and approve pull requests" is checked
   - Click "Save"

## Step 2: Configure Required Secrets

The AI development workflow requires an OpenAI API key:

1. Create an OpenAI API account if you don't have one:
   - Go to [OpenAI.com](https://openai.com) and sign up
   - Navigate to API Keys section in your account
   - Create a new API key

2. Add the API key to your GitHub repository:
   - Go to your repository → Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `OPENAI_API_KEY`
   - Value: Your OpenAI API key (starts with "sk-")
   - Click "Add secret"

## Step 3: Enable GitHub Pages

To use this dashboard, you need to enable GitHub Pages:

1. Go to your repository → Settings → Pages
2. Under "Build and deployment":
   - Source: "GitHub Actions"
   - You don't need to select anything else - the workflow is already configured

3. Wait for the GitHub Pages site to deploy
   - The URL will be displayed in the Pages settings once it's ready
   - Typically it will be `https://your-username.github.io/SevenToDie/`

## Step 4: Verify Workflow Files

Make sure your repository contains all the necessary workflow files:

1. `.github/workflows/ai-development.yml` - The main AI workflow
2. `.github/workflows/build-and-test.yml` - Build and test workflow
3. `.github/workflows/comment-command-handler.yml` - Handles AI commands in issues
4. `.github/workflows/dev-environment.yml` - Creates development environments
5. `.github/workflows/pages.yml` - Deploys the GitHub Pages dashboard

If any of these files are missing, you can manually create them from the templates in the [AI_DEVELOPMENT.md](.github/AI_DEVELOPMENT.md) documentation.

## Step 5: Using the Dashboard

Once everything is set up:

1. Go to your GitHub Pages URL
2. Authenticate with a GitHub personal access token
3. Use the dashboard to trigger AI tasks and monitor progress

Remember, all tokens are stored only in your browser session for security.

## Troubleshooting

If you encounter issues with the AI development setup:

### Workflow Errors

- Check the Actions tab in your repository to see detailed logs of any failed workflows
- Verify that your OpenAI API key is correct and has sufficient credits
- Make sure GitHub Actions has proper permissions in your repository settings

### Dashboard Not Working

- Check if GitHub Pages is properly enabled and deployed
- Verify that your personal access token has both `repo` and `workflow` scopes
- Check browser console for any JavaScript errors

### AI Not Responding to Issues

- Make sure the issue has appropriate labels (like `bug` or `enhancement`)
- Check if the AI workflow runs are visible in the Actions tab
- Verify that the AI has proper permissions to create pull requests

For more detailed troubleshooting, see the [AI_DEVELOPMENT.md](.github/AI_DEVELOPMENT.md) document.