#!/usr/bin/env python3
"""
AI Implementation Script

This script automatically analyzes GitHub issues labeled with "ai-fix", uses AI to 
generate code fixes or improvements, and creates pull requests with these changes.
It represents a key component of the autonomous development system.
"""

import os
import sys
import json
import time
import base64
import logging
import random
import string
from datetime import datetime
import openai
import requests
from git import Repo

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('ai_implementation')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_WORKSPACE = os.getenv("GITHUB_WORKSPACE") or os.getcwd()
GITHUB_API_URL = "https://api.github.com"
ISSUE_NUMBER = os.getenv("ISSUE_NUMBER")  # Set when triggered by a specific issue

def get_issues_to_fix():
    """Get issues labeled with 'ai-fix' tag"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # If a specific issue number is provided, use that
        if ISSUE_NUMBER:
            issue_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{ISSUE_NUMBER}"
            response = requests.get(issue_url, headers=headers)
            issue = response.json()
            # Check if the issue has the ai-fix label
            if any(label['name'] == 'ai-fix' for label in issue.get('labels', [])):
                return [issue]
            else:
                logger.info(f"Issue #{ISSUE_NUMBER} does not have the 'ai-fix' label")
                return []
        
        # Otherwise, get all open issues with the ai-fix label
        issues_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues"
        params = {
            'state': 'open',
            'labels': 'ai-fix',
            'sort': 'created',
            'direction': 'asc'
        }
        
        response = requests.get(issues_url, headers=headers, params=params)
        issues = response.json()
        
        if not issues:
            logger.info("No issues with 'ai-fix' label found")
        
        return issues
    except Exception as e:
        logger.error(f"Failed to get issues to fix: {e}")
        return []

def get_repository_files(relevant_paths=None):
    """Get repository files to provide context to the AI"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Get repository content (root level)
        contents_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/contents"
        response = requests.get(contents_url, headers=headers)
        contents = response.json()
        
        # If no specific paths are provided, default to important directories
        if not relevant_paths:
            relevant_paths = [
                "src/main/java/com/seventodie",
                "pom.xml",
                "README.md"
            ]
        
        # Collect file contents
        files = []
        for path in relevant_paths:
            # Check if it's a directory or file
            if '.' in path.split('/')[-1]:  # It's a file
                file_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/contents/{path}"
                file_response = requests.get(file_url, headers=headers)
                if file_response.status_code == 200:
                    file_data = file_response.json()
                    if file_data.get('size', 0) <= 1000000:  # Skip files larger than ~1MB
                        content = base64.b64decode(file_data['content']).decode('utf-8')
                        files.append({
                            'path': path,
                            'content': content
                        })
            else:  # It's a directory, so list its contents
                dir_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/contents/{path}"
                dir_response = requests.get(dir_url, headers=headers)
                if dir_response.status_code == 200:
                    dir_contents = dir_response.json()
                    
                    # Process only first 10 files to avoid token limits
                    for item in dir_contents[:10]:
                        if item['type'] == 'file' and item['size'] <= 100000:  # Skip files larger than ~100KB
                            file_response = requests.get(item['url'], headers=headers)
                            if file_response.status_code == 200:
                                file_data = file_response.json()
                                try:
                                    content = base64.b64decode(file_data['content']).decode('utf-8')
                                    files.append({
                                        'path': item['path'],
                                        'content': content
                                    })
                                except:
                                    logger.warning(f"Skipping binary file: {item['path']}")
        
        return files
    except Exception as e:
        logger.error(f"Failed to get repository files: {e}")
        return []

def identify_relevant_files(issue_content):
    """Use AI to identify which files are most relevant to the issue"""
    try:
        openai.api_key = OPENAI_API_KEY
        
        # Get file list
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        contents_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/git/trees/main?recursive=1"
        response = requests.get(contents_url, headers=headers)
        tree = response.json().get('tree', [])
        
        # Filter only files, not directories
        files = [item['path'] for item in tree if item['type'] == 'blob']
        
        # Filter only source code files for brevity
        source_files = [f for f in files if f.endswith('.java') or f.endswith('.yml') or f == 'pom.xml']
        
        # Prepare file list for AI
        file_list = "\n".join(source_files[:100])  # Limit to 100 files
        
        prompt = f"""Given the following GitHub issue and list of files in a Minecraft 7 Days To Die plugin project, 
identify which files are most likely to be relevant for fixing the issue. 
Only include files that are directly related to the issue and would need to be modified.

Issue: {issue_content}

Files in the repository:
{file_list}

Return a JSON array of file paths (maximum 5) that are most relevant. Example format:
["src/main/java/com/seventodie/ClassName.java", "src/main/resources/config.yml"]"""

        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=500
        )
        
        result = response['choices'][0]['message']['content']
        
        # Extract JSON array from response
        try:
            # Look for a JSON array in the response
            import re
            json_array_match = re.search(r'\[.+\]', result.replace('\n', ' '))
            if json_array_match:
                json_array = json_array_match.group(0)
                return json.loads(json_array)
            else:
                logger.warning("No JSON array found in AI response, using fallback")
                # Fallback: just return common directories
                return [
                    "src/main/java/com/seventodie",
                    "pom.xml",
                    "README.md"
                ]
        except json.JSONDecodeError:
            logger.warning("Failed to parse JSON from AI response, using fallback")
            return [
                "src/main/java/com/seventodie",
                "pom.xml",
                "README.md"
            ]
    except Exception as e:
        logger.error(f"Failed to identify relevant files: {e}")
        return [
            "src/main/java/com/seventodie",
            "pom.xml",
            "README.md"
        ]

def generate_fix(issue, relevant_files):
    """Generate a fix for the issue using AI"""
    try:
        openai.api_key = OPENAI_API_KEY
        
        issue_title = issue['title']
        issue_body = issue['body'] or ""
        
        # Prepare file context
        file_context = ""
        for file in relevant_files:
            # Add file content with a limit to avoid token issues
            content = file['content']
            if len(content) > 2000:
                content = content[:1000] + "\n... (content truncated) ...\n" + content[-1000:]
            
            file_context += f"\nFile: {file['path']}\n```\n{content}\n```\n"
        
        prompt = f"""You are an AI developer working on a Minecraft plugin called "SevenToDie" that implements 7 Days To Die gameplay in Minecraft.
You are tasked with fixing a GitHub issue by generating code changes.

Issue Title: {issue_title}
Issue Description: {issue_body}

Here are the relevant files from the repository:
{file_context}

Your task is to generate fixes for the issue. Follow these guidelines:
1. Understand the issue thoroughly before making changes.
2. Keep fixes minimal and focused on the specific issue.
3. Maintain the existing code style and architecture.
4. Ensure backward compatibility where possible.
5. Only modify files that need to be changed.
6. If a file needs to be created, provide the full content.

Provide your solution in the following format:
```json
{
  "changes": [
    {
      "path": "path/to/file1.java",
      "action": "modify",
      "original": "exact code segment to replace",
      "replacement": "new code segment"
    },
    {
      "path": "path/to/file2.java",
      "action": "create",
      "content": "entire content of new file"
    }
  ],
  "explanation": "A clear explanation of the changes and how they fix the issue."
}
```

IMPORTANT: For 'modify' actions, make sure 'original' contains the EXACT text to be replaced, including all whitespace and indentation.
Do not use placeholders or partial code snippets in the 'original' field."""

        logger.info("Sending request to OpenAI to generate fix")
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
            max_tokens=3000
        )
        
        result = response['choices'][0]['message']['content']
        
        # Extract JSON from response
        try:
            # Look for JSON block between triple backticks
            import re
            json_match = re.search(r'```json\s+(.*?)\s+```', result, re.DOTALL)
            if json_match:
                changes_json = json_match.group(1)
                return json.loads(changes_json)
            else:
                # Try to find any JSON object
                json_match = re.search(r'{.*}', result, re.DOTALL)
                if json_match:
                    changes_json = json_match.group(0)
                    return json.loads(changes_json)
                else:
                    logger.error("No JSON found in AI response")
                    return None
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from AI response: {e}")
            return None
    except Exception as e:
        logger.error(f"Failed to generate fix: {e}")
        return None

def apply_changes_and_create_pr(issue, changes_data):
    """Apply the changes to a new branch and create a pull request"""
    try:
        # Create a unique branch name
        branch_suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=8))
        branch_name = f"ai-fix-issue-{issue['number']}-{branch_suffix}"
        
        # Initialize repo
        repo = Repo(GITHUB_WORKSPACE)
        
        # Create and checkout new branch
        repo.git.checkout('main')
        repo.git.checkout('-b', branch_name)
        
        # Apply changes
        changes = changes_data.get('changes', [])
        for change in changes:
            path = change['path']
            action = change['action']
            
            full_path = os.path.join(GITHUB_WORKSPACE, path)
            os.makedirs(os.path.dirname(full_path), exist_ok=True)
            
            if action == 'create':
                # Create new file
                with open(full_path, 'w') as f:
                    f.write(change['content'])
                repo.git.add(path)
            elif action == 'modify':
                if os.path.exists(full_path):
                    # Read existing file
                    with open(full_path, 'r') as f:
                        content = f.read()
                    
                    # Replace content
                    new_content = content.replace(change['original'], change['replacement'])
                    
                    # Write back
                    with open(full_path, 'w') as f:
                        f.write(new_content)
                    
                    repo.git.add(path)
                else:
                    logger.warning(f"File not found: {path}")
        
        # Commit changes
        commit_message = f"Fix #{issue['number']}: {issue['title']}\n\nAI-generated fix"
        repo.git.commit('-m', commit_message)
        
        # Push branch
        repo.git.push('--set-upstream', 'origin', branch_name)
        
        # Create pull request
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        pr_data = {
            'title': f"Fix issue #{issue['number']}: {issue['title']}",
            'body': f"""This PR addresses issue #{issue['number']}.

## AI-Generated Fix Explanation
{changes_data.get('explanation', 'No explanation provided')}

## Changes Made
{len(changes)} files modified

---
*This PR was automatically generated by the autonomous development system. Please review the changes carefully.*
""",
            'head': branch_name,
            'base': 'main',
            'maintainer_can_modify': True
        }
        
        pr_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/pulls"
        response = requests.post(pr_url, headers=headers, json=pr_data)
        
        if response.status_code in (201, 200):
            pr_info = response.json()
            logger.info(f"Created PR #{pr_info['number']}: {pr_info['html_url']}")
            
            # Add comment to the issue
            issue_comment = {
                'body': f"I've created a pull request with an AI-generated fix: {pr_info['html_url']}\n\nPlease review the changes."
            }
            
            comment_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{issue['number']}/comments"
            requests.post(comment_url, headers=headers, json=issue_comment)
            
            # Add ai-implementation label to the PR
            labels_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues/{pr_info['number']}/labels"
            labels_data = {'labels': ['ai-implementation']}
            requests.post(labels_url, headers=headers, json=labels_data)
            
            return pr_info
        else:
            logger.error(f"Failed to create PR: {response.status_code} {response.text}")
            return None
    except Exception as e:
        logger.error(f"Failed to apply changes and create PR: {e}")
        return None

def log_to_dashboard(issue, changes_data, pr_info):
    """Log fix attempt to the dashboard"""
    try:
        dashboard_dir = os.path.join(os.getcwd(), "dashboard", "src", "data")
        os.makedirs(dashboard_dir, exist_ok=True)
        
        log_file = os.path.join(dashboard_dir, "ai_implementations.json")
        
        # Create log entry
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "issue_number": issue['number'],
            "issue_title": issue['title'],
            "successful": pr_info is not None,
            "changes_count": len(changes_data.get('changes', [])) if changes_data else 0,
            "explanation": changes_data.get('explanation', '') if changes_data else '',
            "pr_number": pr_info['number'] if pr_info else None,
            "pr_url": pr_info['html_url'] if pr_info else None
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
            
        logger.info(f"Logged implementation to dashboard data: {log_file}")
    except Exception as e:
        logger.error(f"Failed to log to dashboard: {e}")

def main():
    """Main function to process issues and generate fixes"""
    try:
        logger.info("Starting AI implementation script")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Get issues to fix
        issues = get_issues_to_fix()
        
        if not issues:
            logger.info("No issues to fix")
            return
        
        for issue in issues:
            logger.info(f"Processing issue #{issue['number']}: {issue['title']}")
            
            # Identify relevant files for this issue
            issue_content = f"{issue['title']}\n\n{issue['body']}"
            relevant_paths = identify_relevant_files(issue_content)
            logger.info(f"Identified relevant paths: {relevant_paths}")
            
            # Get file contents for relevant files
            relevant_files = get_repository_files(relevant_paths)
            logger.info(f"Fetched {len(relevant_files)} files for context")
            
            # Generate fix
            changes_data = generate_fix(issue, relevant_files)
            if not changes_data:
                logger.error(f"Failed to generate fix for issue #{issue['number']}")
                continue
            
            logger.info(f"Generated fix with {len(changes_data.get('changes', []))} changes")
            
            # Apply changes and create PR
            pr_info = apply_changes_and_create_pr(issue, changes_data)
            
            # Log to dashboard
            log_to_dashboard(issue, changes_data, pr_info)
            
            # Add some delay between processing issues
            time.sleep(5)
            
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()