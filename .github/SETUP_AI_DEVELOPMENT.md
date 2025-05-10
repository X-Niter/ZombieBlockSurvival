# Setting Up AI Development for SevenToDie Plugin

This document provides detailed instructions on how to set up the AI-powered development features for this plugin.

## Required Secrets

To enable the AI development capabilities, you need to set up the following repository secrets:

### 1. OPENAI_API_KEY

This is required for the AI to analyze code and generate fixes.

1. Get an API key from OpenAI:
   - Go to [OpenAI API Keys](https://platform.openai.com/api-keys)
   - Create a new API key
   - Copy the key (it starts with "sk-")

2. Add it to your GitHub repository:
   - Go to your repository on GitHub
   - Click on "Settings" → "Secrets and variables" → "Actions"
   - Click "New repository secret"
   - Name: `OPENAI_API_KEY`
   - Value: Paste your OpenAI API key
   - Click "Add secret"

### 2. Repository Permissions

The GitHub Actions workflows need permission to create pull requests:

1. Go to your repository on GitHub
2. Click on "Settings" → "Actions" → "General"
3. Scroll down to "Workflow permissions"
4. Select "Read and write permissions"
5. Check "Allow GitHub Actions to create and approve pull requests"
6. Click "Save"

## Verifying Setup

Once you've set up the required secrets and permissions, you can verify that everything is working:

1. Go to the "Actions" tab in your repository
2. Select the "AI Development Assistant" workflow
3. Click "Run workflow"
4. Choose "analyze-code" as the task
5. Add a description like "Please analyze the codebase for bugs and issues"
6. Click "Run workflow"

The workflow should run and:
1. Analyze your code
2. Generate an analysis report
3. Potentially create a pull request with fixes (if issues are found)

## Using AI Commands in Issues

After setting up the AI development system, you can use commands in issue comments:

- `@ai analyze` - Analyze the code related to the issue
- `@ai fix` - Try to fix the problem described in the issue
- `@ai implement` - Implement the feature described in the issue
- `@ai optimize` - Optimize the code described in the issue

For example, create a new issue describing a bug, then add a comment with `@ai fix` to have the AI attempt to fix it automatically.

## Troubleshooting

If the AI development features aren't working:

1. **Check Action Logs**: Go to the Actions tab and check the workflow logs for error messages

2. **API Key Issues**: Make sure your OpenAI API key is valid and has sufficient credits

3. **Permission Issues**: Verify that the workflow permissions are set correctly

4. **Rate Limits**: Be aware of GitHub API rate limits and OpenAI API rate limits

5. **Token Limits**: The AI can only analyze a limited number of files at once due to token limits