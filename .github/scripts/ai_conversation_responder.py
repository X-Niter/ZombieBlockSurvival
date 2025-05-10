#!/usr/bin/env python3
"""
AI Conversation Responder
This script generates intelligent responses to GitHub issues and pull requests
using OpenAI's GPT model. It analyzes the content and context of the issue/PR
and provides helpful, relevant responses.
"""

import os
import sys
import json
import time
import logging
from datetime import datetime
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ai_conversation_responder')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_EVENT_PATH = os.getenv("GITHUB_EVENT_PATH")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_API_URL = "https://api.github.com"

def get_github_event():
    """Read GitHub event data from the event.json file."""
    try:
        with open(GITHUB_EVENT_PATH, 'r') as f:
            return json.load(f)
    except Exception as e:
        logger.error(f"Failed to read GitHub event: {e}")
        return None

def get_repository_context():
    """Fetch repository description and key files to provide context."""
    headers = {
        'Authorization': f'token {GITHUB_TOKEN}',
        'Accept': 'application/vnd.github.v3+json'
    }
    
    try:
        # Get repository info
        repo_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}"
        repo_response = requests.get(repo_url, headers=headers)
        repo_data = repo_response.json()
        
        # Get README content
        readme_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/readme"
        readme_response = requests.get(readme_url, headers=headers)
        readme_data = readme_response.json()
        readme_content = ""
        if 'content' in readme_data:
            import base64
            readme_content = base64.b64decode(readme_data['content']).decode('utf-8')
        
        return {
            "description": repo_data.get('description', ''),
            "readme_summary": readme_content[:1000] + "..." if len(readme_content) > 1000 else readme_content
        }
    except Exception as e:
        logger.error(f"Failed to fetch repository context: {e}")
        return {"description": "", "readme_summary": ""}

def get_code_context(event_data):
    """Get relevant code context for PR events."""
    if 'pull_request' not in event_data:
        return ""
    
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        pr_number = event_data['pull_request']['number']
        files_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/pulls/{pr_number}/files"
        
        files_response = requests.get(files_url, headers=headers)
        files_data = files_response.json()
        
        code_context = "Files changed in this PR:\n\n"
        for file in files_data[:5]:  # Limit to first 5 files to avoid token limits
            code_context += f"- {file['filename']} ({file['additions']} additions, {file['deletions']} deletions)\n"
        
        return code_context
    except Exception as e:
        logger.error(f"Failed to fetch PR code context: {e}")
        return ""

def get_conversation_history(event_data):
    """Get existing comment history for the issue/PR."""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        if 'issue' in event_data:
            number = event_data['issue']['number']
            comments_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{number}/comments"
        elif 'pull_request' in event_data:
            number = event_data['pull_request']['number']
            comments_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{number}/comments"
        else:
            return []
        
        comments_response = requests.get(comments_url, headers=headers)
        comments_data = comments_response.json()
        
        # Filter out comments not from our bot
        bot_login = "github-actions[bot]"
        return [
            {
                "user": comment['user']['login'],
                "body": comment['body'],
                "created_at": comment['created_at']
            }
            for comment in comments_data
        ]
    except Exception as e:
        logger.error(f"Failed to fetch conversation history: {e}")
        return []

def generate_ai_response(event_data, repo_context, code_context, conversation_history):
    """Generate AI response using OpenAI."""
    try:
        openai.api_key = OPENAI_API_KEY
        
        # Determine if this is an issue or PR
        if 'issue' in event_data:
            title = event_data['issue']['title']
            body = event_data['issue']['body'] or ""
            author = event_data['issue']['user']['login']
            item_type = "issue"
        elif 'pull_request' in event_data:
            title = event_data['pull_request']['title']
            body = event_data['pull_request']['body'] or ""
            author = event_data['pull_request']['user']['login']
            item_type = "pull request"
        else:
            logger.error("Could not determine if event is issue or PR")
            return None
        
        # Prepare conversation context from history
        conversation_context = ""
        if conversation_history:
            conversation_context = "Previous comments:\n\n"
            for comment in conversation_history[-3:]:  # Limit to last 3 comments
                conversation_context += f"@{comment['user']} at {comment['created_at']}:\n{comment['body']}\n\n"
        
        prompt = f"""You are an AI assistant for a Minecraft 7 Days To Die plugin development team.
Repository description: {repo_context['description']}

A GitHub {item_type} was created by @{author}:

Title: {title}

Content:
{body}

{code_context}

{conversation_context}

Reply as if you're a professional, experienced software developer who is helpful and friendly. Focus on:
1. Understanding the technical request/issue
2. Providing concrete suggestions with code examples when relevant
3. Being supportive and encouraging of community contributions
4. Sharing knowledge about best practices for Minecraft/Paper plugin development
5. If it's a bug report, ask for necessary information like logs, Minecraft version, etc.
6. If it's a feature request, evaluate how it fits with the 7 Days To Die gameplay concept

Your response should be conversational but technically accurate."""

        logger.info("Sending request to OpenAI")
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=800
        )
        
        return response['choices'][0]['message']['content']
    except Exception as e:
        logger.error(f"Failed to generate AI response: {e}")
        return None

def post_github_comment(event_data, comment):
    """Post comment to GitHub issue or PR."""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        if 'issue' in event_data:
            number = event_data['issue']['number']
            comments_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{number}/comments"
        elif 'pull_request' in event_data:
            number = event_data['pull_request']['number']
            comments_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{number}/comments"
        else:
            logger.error("Could not determine if event is issue or PR")
            return False
        
        # Add a signature to the comment
        signature = "\n\n---\n*I'm an AI assistant. If I've misunderstood something, please let me know.*"
        full_comment = comment + signature
        
        response = requests.post(
            comments_url,
            headers=headers,
            json={"body": full_comment}
        )
        
        if response.status_code == 201:
            logger.info(f"Successfully posted comment to {'issue' if 'issue' in event_data else 'PR'} #{number}")
            return True
        else:
            logger.error(f"Failed to post comment: {response.status_code} {response.text}")
            return False
    except Exception as e:
        logger.error(f"Failed to post GitHub comment: {e}")
        return False

def log_to_dashboard(event_data, response):
    """Log interaction to dashboard data for future review."""
    try:
        dashboard_dir = os.path.join(os.getcwd(), "dashboard", "src", "data")
        os.makedirs(dashboard_dir, exist_ok=True)
        
        log_file = os.path.join(dashboard_dir, "ai_interactions.json")
        
        # Determine event type and ID
        if 'issue' in event_data:
            event_type = "issue"
            event_id = event_data['issue']['number']
            title = event_data['issue']['title']
        elif 'pull_request' in event_data:
            event_type = "pull_request"
            event_id = event_data['pull_request']['number']
            title = event_data['pull_request']['title']
        else:
            event_type = "unknown"
            event_id = 0
            title = "Unknown"
        
        # Create log entry
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "event_type": event_type,
            "event_id": event_id,
            "title": title,
            "response": response
        }
        
        # Read existing log if it exists
        try:
            with open(log_file, 'r') as f:
                log_data = json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            log_data = []
        
        # Add new entry and write back
        log_data.append(log_entry)
        with open(log_file, 'w') as f:
            json.dump(log_data, f, indent=2)
            
        logger.info(f"Logged interaction to dashboard data: {log_file}")
    except Exception as e:
        logger.error(f"Failed to log to dashboard: {e}")

def main():
    """Main function to process GitHub events and generate responses."""
    try:
        logger.info("Starting AI conversation responder")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_EVENT_PATH, GITHUB_REPOSITORY]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Read GitHub event data
        event_data = get_github_event()
        if not event_data:
            logger.error("Failed to get GitHub event data")
            sys.exit(1)
        
        logger.info(f"Processing GitHub event: {GITHUB_EVENT_PATH}")
        
        # Get repository context
        repo_context = get_repository_context()
        
        # Get code context for PRs
        code_context = get_code_context(event_data)
        
        # Get conversation history
        conversation_history = get_conversation_history(event_data)
        
        # Generate AI response
        ai_response = generate_ai_response(event_data, repo_context, code_context, conversation_history)
        if not ai_response:
            logger.error("Failed to generate AI response")
            sys.exit(1)
        
        logger.info("Generated AI response successfully")
        
        # Post comment to GitHub
        if post_github_comment(event_data, ai_response):
            logger.info("Successfully posted comment to GitHub")
        else:
            logger.error("Failed to post comment to GitHub")
        
        # Log to dashboard
        log_to_dashboard(event_data, ai_response)
        
        logger.info("AI conversation responder completed successfully")
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
