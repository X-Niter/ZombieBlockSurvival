#!/usr/bin/env python3
"""
AI Command Handler

This script parses comments on GitHub issues looking for AI commands like:
/ai fix, /ai implement, /ai analyze, /ai status, /ai help

It then performs the appropriate action based on the command.
"""

import os
import sys
import json
import logging
import re
from datetime import datetime
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ai_command_handler')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_EVENT_PATH = os.getenv("GITHUB_EVENT_PATH")
GITHUB_API_URL = "https://api.github.com"

# Define AI commands
AI_COMMANDS = {
    'fix': 'Request a bug fix for an issue',
    'implement': 'Request a feature implementation',
    'analyze': 'Request detailed analysis of code or issue',
    'status': 'Check implementation status',
    'help': 'Show available commands and usage'
}

def get_github_event():
    """Read GitHub event data from the event.json file."""
    try:
        with open(GITHUB_EVENT_PATH, 'r') as f:
            return json.load(f)
    except Exception as e:
        logger.error(f"Failed to read GitHub event: {e}")
        return None

def extract_command_from_comment(comment_body):
    """Extract AI command from a comment"""
    # Match /ai command [optional arguments]
    pattern = r'/ai\s+(\w+)(?:\s+(.+))?'
    match = re.search(pattern, comment_body, re.IGNORECASE)
    
    if match:
        command = match.group(1).lower()
        args = match.group(2) if match.group(2) else ''
        
        if command in AI_COMMANDS:
            return {
                'command': command,
                'args': args.strip()
            }
    
    return None

def post_comment(issue_number, comment):
    """Post a comment on the issue"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        comments_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}/comments"
        data = {"body": comment}
        
        response = requests.post(comments_url, headers=headers, json=data)
        
        if response.status_code != 201:
            logger.error(f"Failed to post comment: {response.status_code} {response.text}")
            return False
        
        logger.info(f"Posted comment to issue #{issue_number}")
        return True
    except Exception as e:
        logger.error(f"Failed to post comment: {e}")
        return False

def add_label(issue_number, label):
    """Add a label to an issue"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        labels_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}/labels"
        data = {"labels": [label]}
        
        response = requests.post(labels_url, headers=headers, json=data)
        
        if response.status_code not in [200, 201]:
            logger.error(f"Failed to add label: {response.status_code} {response.text}")
            return False
        
        logger.info(f"Added label '{label}' to issue #{issue_number}")
        return True
    except Exception as e:
        logger.error(f"Failed to add label: {e}")
        return False

def handle_fix_command(issue_number, args):
    """Handle the /ai fix command"""
    # Add the ai-fix label to trigger the implementation workflow
    if add_label(issue_number, "ai-fix"):
        response = """I'll try to fix this issue automatically.

The `ai-fix` label has been added, and our autonomous development system will analyze the issue and generate a fix.

You'll receive a notification when a pull request with a proposed fix is created. Please review the PR when it's ready.

*This is handled by the autonomous AI system.*"""
        
        return post_comment(issue_number, response)
    
    return False

def handle_implement_command(issue_number, args):
    """Handle the /ai implement command"""
    # Add the ai-implement label to trigger the implementation workflow
    if add_label(issue_number, "ai-implement"):
        response = """I'll work on implementing this feature.

The `ai-implement` label has been added, and our autonomous development system will analyze the request and start working on it.

Feature implementation may take longer than bug fixes. You'll receive a notification when a pull request with the implementation is created.

*This is handled by the autonomous AI system.*"""
        
        return post_comment(issue_number, response)
    
    return False

def handle_analyze_command(issue_number, args):
    """Handle the /ai analyze command"""
    try:
        # First, get the issue details
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        issue_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}"
        response = requests.get(issue_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get issue details: {response.status_code} {response.text}")
            return False
        
        issue = response.json()
        
        # Check for specific paths to analyze
        analyze_target = args if args else issue['title'] + " " + (issue['body'] or "")
        
        # Generate analysis with AI
        openai.api_key = OPENAI_API_KEY
        
        prompt = f"""You are an AI assistant for a Minecraft plugin development team working on SevenToDie, 
a plugin that implements 7 Days To Die gameplay in Minecraft.

Please provide a detailed analysis of the following issue/request:

Issue Title: {issue['title']}
Issue Body: {issue['body'] or "No description provided"}

Analysis Target: {analyze_target}

Your analysis should include:
1. Root cause identification (for bugs)
2. Implementation approach (for features)
3. Potential challenges and considerations
4. Estimated complexity (low, medium, high)
5. Files likely to be affected

Be thorough but concise. Focus on technical details relevant to the SevenToDie Minecraft plugin context."""

        logger.info("Sending request to OpenAI for analysis")
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=1500
        )
        
        analysis = response['choices'][0]['message']['content']
        
        # Post the analysis as a comment
        comment = f"""## AI Analysis

{analysis}

---
*This analysis was generated by the autonomous AI system. If you need clarification or have follow-up questions, please ask.*"""
        
        return post_comment(issue_number, comment)
    except Exception as e:
        logger.error(f"Failed to handle analyze command: {e}")
        error_comment = """I'm sorry, I encountered an error while trying to analyze this issue.

Please try again later or contact the repository maintainers if the problem persists.

*This message was generated by the autonomous AI system.*"""
        post_comment(issue_number, error_comment)
        return False

def handle_status_command(issue_number, args):
    """Handle the /ai status command"""
    try:
        # Check if there are any PRs related to this issue
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Search for PRs mentioning this issue
        search_url = f"{GITHUB_API_URL}/search/issues"
        query = f"repo:{GITHUB_REPOSITORY} is:pr is:open mentions:{issue_number}"
        response = requests.get(search_url, headers=headers, params={"q": query})
        
        if response.status_code != 200:
            logger.error(f"Failed to search PRs: {response.status_code} {response.text}")
            return False
        
        prs = response.json()['items']
        
        # Check issue labels
        issue_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}"
        response = requests.get(issue_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get issue details: {response.status_code} {response.text}")
            return False
        
        issue = response.json()
        labels = [label['name'] for label in issue.get('labels', [])]
        
        # Generate status report
        if "ai-fix" in labels or "ai-implement" in labels:
            if prs:
                pr_list = "\n".join([f"- [#{pr['number']}]({pr['html_url']}): {pr['title']}" for pr in prs])
                status = f"""## Status Report

This issue is currently being worked on by the autonomous development system.

**Active Pull Requests:**
{pr_list}

Please review the PRs above to see the proposed changes.

*This status report was generated by the autonomous AI system.*"""
            else:
                status = """## Status Report

This issue is in the queue for automated handling. The AI system will analyze it and generate a fix or implementation soon.

No pull requests have been created yet. You'll be notified when a PR is available for review.

*This status report was generated by the autonomous AI system.*"""
        else:
            status = """## Status Report

This issue is not currently flagged for automated handling.

To request automated handling, please use one of the following commands:
- `/ai fix` - Request an automated bug fix
- `/ai implement` - Request an automated feature implementation

*This status report was generated by the autonomous AI system.*"""
        
        return post_comment(issue_number, status)
    except Exception as e:
        logger.error(f"Failed to handle status command: {e}")
        error_comment = """I'm sorry, I encountered an error while trying to get the status of this issue.

Please try again later or contact the repository maintainers if the problem persists.

*This message was generated by the autonomous AI system.*"""
        post_comment(issue_number, error_comment)
        return False

def handle_help_command(issue_number, args):
    """Handle the /ai help command"""
    help_text = """## AI Command Help

You can interact with the autonomous development system using the following commands:

| Command | Description | Usage |
|---------|-------------|-------|
| `/ai fix` | Request an automated bug fix | `/ai fix` |
| `/ai implement` | Request an automated feature implementation | `/ai implement` |
| `/ai analyze` | Request a detailed analysis | `/ai analyze [optional target]` |
| `/ai status` | Check implementation status | `/ai status` |
| `/ai help` | Show this help message | `/ai help` |

**Examples:**
- `/ai fix` - Flag an issue for automatic fixing
- `/ai implement` - Request feature implementation
- `/ai analyze BaseCommand.java` - Analyze a specific file
- `/ai status` - Check if any PRs have been created for this issue

The autonomous system will respond to your command and take appropriate action.

*This help message was generated by the autonomous AI system.*"""
    
    return post_comment(issue_number, help_text)

def handle_command(command_data, issue_number):
    """Handle an AI command"""
    command = command_data['command']
    args = command_data['args']
    
    logger.info(f"Handling command: {command} with args: {args}")
    
    if command == 'fix':
        return handle_fix_command(issue_number, args)
    elif command == 'implement':
        return handle_implement_command(issue_number, args)
    elif command == 'analyze':
        return handle_analyze_command(issue_number, args)
    elif command == 'status':
        return handle_status_command(issue_number, args)
    elif command == 'help':
        return handle_help_command(issue_number, args)
    else:
        # Should not reach here as extract_command_from_comment validates commands
        logger.error(f"Unknown command: {command}")
        return False

def main():
    """Main function to process GitHub events and handle AI commands"""
    try:
        logger.info("Starting AI command handler")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY, GITHUB_EVENT_PATH]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Get event data
        event_data = get_github_event()
        if not event_data:
            logger.error("Failed to get GitHub event data")
            sys.exit(1)
        
        # Only process issue comments
        if event_data.get('action') != 'created' or 'comment' not in event_data or 'issue' not in event_data:
            logger.info("Not an issue comment creation event, skipping")
            sys.exit(0)
        
        # Extract issue number and comment body
        issue_number = event_data['issue']['number']
        comment_body = event_data['comment']['body']
        
        # Look for AI commands
        command_data = extract_command_from_comment(comment_body)
        
        if command_data:
            logger.info(f"Found AI command: {command_data['command']}")
            if handle_command(command_data, issue_number):
                logger.info(f"Successfully handled command: {command_data['command']}")
            else:
                logger.error(f"Failed to handle command: {command_data['command']}")
        else:
            logger.info("No AI command found in comment")
        
        logger.info("AI command handler completed successfully")
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()