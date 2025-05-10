#!/usr/bin/env python3
"""
AI Issue Triage Script

This script automatically analyzes new GitHub issues, categorizes them,
suggests labels, and provides an initial response.
"""

import os
import sys
import json
import logging
from datetime import datetime
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ai_triage_issue')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
ISSUE_NUMBER = os.getenv("ISSUE_NUMBER")
GITHUB_API_URL = "https://api.github.com"

def get_issue_details():
    """Get details of the issue being triaged"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        issue_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{ISSUE_NUMBER}"
        response = requests.get(issue_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get issue details: {response.status_code} {response.text}")
            return None
        
        return response.json()
    except Exception as e:
        logger.error(f"Failed to get issue details: {e}")
        return None

def get_repository_info():
    """Get basic information about the repository"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        repo_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}"
        response = requests.get(repo_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get repository info: {response.status_code} {response.text}")
            return None
        
        return response.json()
    except Exception as e:
        logger.error(f"Failed to get repository info: {e}")
        return None

def analyze_issue(issue, repo_info):
    """Analyze the issue using AI and determine category and appropriate labels"""
    try:
        openai.api_key = OPENAI_API_KEY
        
        issue_title = issue['title']
        issue_body = issue['body'] or ""
        user = issue['user']['login']
        
        prompt = f"""You are an AI assistant for a Minecraft plugin development team. The plugin is called "SevenToDie" and implements 7 Days To Die gameplay in Minecraft.

Analyze this GitHub issue and categorize it:

Repository: {repo_info['name']}
Repository Description: {repo_info['description']}
Issue Title: {issue_title}
Issue Body:
{issue_body}

Based on this information, categorize the issue and suggest GitHub labels.

Return your analysis in this JSON format:
```json
{{
  "category": "One of: bug, feature_request, question, enhancement, or documentation",
  "labels": ["array", "of", "suggested", "label", "names"],
  "response": "Your suggested initial response to the issue",
  "requires_attention": true/false (whether this needs human attention immediately),
  "complexity": "low/medium/high",
  "ai_fixable": true/false (whether this might be suitable for AI to fix)
}}
```

For labels, choose from common GitHub labels such as:
- bug, feature, enhancement, documentation, question
- difficulty: easy, difficulty: medium, difficulty: hard
- good first issue
- help wanted
- priority: low, priority: medium, priority: high
- scope: gameplay, scope: building, scope: zombies, scope: performance
- type: bug, type: feature, type: refactor
- AI-fix (if it seems like something the AI could potentially fix)

For the response, be helpful, friendly and welcoming. Acknowledge the issue and:
1. For bugs: Ask for any missing information like Minecraft version, detailed steps to reproduce, etc.
2. For features: Thank them for the suggestion and give initial thoughts.
3. For questions: Provide a helpful answer if possible or ask for clarification.
4. For enhancements: Discuss the merits and potential implementation challenges.

DO NOT promise specific timelines or guaranteed implementations.
"""
        
        logger.info("Sending request to OpenAI")
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=1000
        )
        
        result = response['choices'][0]['message']['content']
        
        # Extract JSON from response
        try:
            # Look for JSON block between triple backticks
            import re
            json_match = re.search(r'```json\s+(.*?)\s+```', result, re.DOTALL)
            if json_match:
                analysis_json = json_match.group(1)
                return json.loads(analysis_json)
            else:
                # Try to find any JSON object
                json_match = re.search(r'{.*}', result, re.DOTALL)
                if json_match:
                    analysis_json = json_match.group(0)
                    return json.loads(analysis_json)
                else:
                    logger.error("No JSON found in AI response")
                    return None
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from AI response: {e}")
            return None
    except Exception as e:
        logger.error(f"Failed to analyze issue: {e}")
        return None

def apply_labels(issue_number, labels):
    """Apply suggested labels to the issue"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        labels_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}/labels"
        data = {"labels": labels}
        
        response = requests.post(labels_url, headers=headers, json=data)
        
        if response.status_code not in [200, 201]:
            logger.error(f"Failed to apply labels: {response.status_code} {response.text}")
            return False
        
        logger.info(f"Applied labels to issue #{issue_number}: {', '.join(labels)}")
        return True
    except Exception as e:
        logger.error(f"Failed to apply labels: {e}")
        return False

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

def notify_maintainers(issue_number, analysis):
    """Notify maintainers if the issue requires immediate attention"""
    if analysis.get("requires_attention", False):
        try:
            # This could send an email, Slack message, or create another GitHub issue
            # For now, we'll just add a special label
            headers = {
                'Authorization': f'token {GITHUB_TOKEN}',
                'Accept': 'application/vnd.github.v3+json'
            }
            
            labels_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue_number}/labels"
            data = {"labels": ["attention-needed"]}
            
            response = requests.post(labels_url, headers=headers, json=data)
            
            if response.status_code not in [200, 201]:
                logger.error(f"Failed to add attention label: {response.status_code} {response.text}")
                return False
            
            logger.info(f"Added attention-needed label to issue #{issue_number}")
            return True
        except Exception as e:
            logger.error(f"Failed to notify maintainers: {e}")
            return False
    
    return True

def log_to_dashboard(issue, analysis):
    """Log triage results to dashboard data"""
    try:
        dashboard_dir = os.path.join(os.getcwd(), "dashboard", "src", "data")
        os.makedirs(dashboard_dir, exist_ok=True)
        
        log_file = os.path.join(dashboard_dir, "issue_triage_logs.json")
        
        # Create log entry
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "issue_number": issue["number"],
            "issue_title": issue["title"],
            "category": analysis.get("category", "unknown"),
            "labels": analysis.get("labels", []),
            "complexity": analysis.get("complexity", "unknown"),
            "ai_fixable": analysis.get("ai_fixable", False),
            "requires_attention": analysis.get("requires_attention", False)
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
            
        logger.info(f"Logged triage results to dashboard data: {log_file}")
    except Exception as e:
        logger.error(f"Failed to log to dashboard: {e}")

def main():
    """Main function to triage a GitHub issue"""
    try:
        logger.info("Starting AI issue triage")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY, ISSUE_NUMBER]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Get issue details
        issue = get_issue_details()
        if not issue:
            logger.error("Failed to get issue details")
            sys.exit(1)
        
        # Get repository info
        repo_info = get_repository_info()
        if not repo_info:
            logger.error("Failed to get repository info")
            sys.exit(1)
        
        # Analyze issue
        analysis = analyze_issue(issue, repo_info)
        if not analysis:
            logger.error("Failed to analyze issue")
            sys.exit(1)
        
        logger.info(f"Issue analysis: {json.dumps(analysis, indent=2)}")
        
        # Apply labels
        if "labels" in analysis and analysis["labels"]:
            if apply_labels(issue["number"], analysis["labels"]):
                logger.info("Labels applied successfully")
        
        # Format the comment with the bot signature
        comment = analysis.get("response", "Thank you for your issue! Our team will look into it.")
        comment += "\n\n---\n*I'm an AI assistant helping triage issues. If I've misunderstood something, please let me know.*"
        
        # Post comment
        if post_comment(issue["number"], comment):
            logger.info("Comment posted successfully")
        
        # Notify maintainers if needed
        if notify_maintainers(issue["number"], analysis):
            logger.info("Maintainers notified (if required)")
        
        # Log to dashboard
        log_to_dashboard(issue, analysis)
        
        logger.info("AI issue triage completed successfully")
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()