#!/usr/bin/env python3
"""
AI Processor for GitHub Actions

This script processes GitHub issues and workflow dispatch events, analyzing code and issues
to generate automated fixes and responses using AI.
"""

import argparse
import os
import json
import sys
import subprocess
import re
import requests
from pathlib import Path
import openai

# Constants
GITHUB_API_URL = "https://api.github.com"
OPENAI_MODEL = "gpt-4"  # Using GPT-4 as it's more capable for code

def setup_argparse():
    """Set up argument parsing for the script."""
    parser = argparse.ArgumentParser(description='AI processor for GitHub Actions')
    parser.add_argument('--task', required=True, 
                        choices=['analyze-code', 'fix-bugs', 'implement-feature', 
                                'improve-performance', 'analyze-issue'],
                        help='Task for the AI to perform')
    parser.add_argument('--repo', required=True, help='Repository in owner/repo format')
    parser.add_argument('--token', required=True, help='GitHub token')
    parser.add_argument('--issue', help='Issue number if relevant')
    parser.add_argument('--description', help='Description of the task')
    return parser.parse_args()

def setup_github_api(token):
    """Configure the GitHub API."""
    headers = {
        'Authorization': f'token {token}',
        'Accept': 'application/vnd.github.v3+json'
    }
    return headers

def get_issue_details(repo, issue_number, headers):
    """Fetch details about a GitHub issue."""
    url = f"{GITHUB_API_URL}/repos/{repo}/issues/{issue_number}"
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        return response.json()
    else:
        print(f"Error fetching issue: {response.status_code}")
        print(response.text)
        return None

def get_repository_files(repo, headers, path=""):
    """Recursively get repository files."""
    url = f"{GITHUB_API_URL}/repos/{repo}/contents/{path}"
    response = requests.get(url, headers=headers)
    
    if response.status_code != 200:
        print(f"Error fetching repository files: {response.status_code}")
        return []
    
    contents = response.json()
    files = []
    
    if isinstance(contents, list):
        for item in contents:
            if item['type'] == 'file' and item['name'].endswith(('.java', '.yml', '.xml')):
                files.append(item['path'])
            elif item['type'] == 'dir' and not item['name'].startswith(('.', 'target')):
                files.extend(get_repository_files(repo, headers, item['path']))
    
    return files

def get_file_content(repo, file_path, headers):
    """Get content of a specific file."""
    url = f"{GITHUB_API_URL}/repos/{repo}/contents/{file_path}"
    response = requests.get(url, headers=headers)
    
    if response.status_code != 200:
        print(f"Error fetching file {file_path}: {response.status_code}")
        return None
    
    content = response.json()
    if 'content' in content:
        import base64
        return base64.b64decode(content['content']).decode('utf-8')
    
    return None

def analyze_code_with_ai(code_files, issue_description=None):
    """Use AI to analyze code and generate insights."""
    client = openai.OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
    
    # Build the prompt
    prompt = "You are an expert Java developer specialized in Minecraft Bukkit/Paper plugins.\n\n"
    
    if issue_description:
        prompt += f"ISSUE DESCRIPTION:\n{issue_description}\n\n"
    
    prompt += "CODE FILES:\n"
    for file_name, content in code_files.items():
        # Truncate very large files to avoid token limits
        if len(content) > 10000:
            content = content[:10000] + "\n... (truncated)\n"
        prompt += f"File: {file_name}\n```java\n{content}\n```\n\n"
    
    prompt += """
TASK: Analyze the code and provide the following:
1. A summary of what the code does
2. Identify any bugs or issues
3. Recommend specific fixes with code snippets
4. For each fix, provide the file path, the code to replace, and the new code

Your response should be structured as follows:
```json
{
  "analysis": "Overall analysis of the codebase",
  "issues": [
    {
      "file": "path/to/file.java",
      "description": "Description of the issue",
      "original_code": "code with issue",
      "fixed_code": "fixed code"
    }
  ],
  "recommendations": "General recommendations for improvement",
  "response_for_issue": "A helpful response to provide on the GitHub issue"
}
```
"""
    
    try:
        response = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": "You are an expert Java developer specialized in Minecraft plugin development."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.1,  # Lower temperature for more deterministic responses
            max_tokens=4000
        )
        
        # Extract and parse the JSON from the response
        result = response.choices[0].message.content
        
        # Find JSON block in response
        json_match = re.search(r'```json\n(.*?)\n```', result, re.DOTALL)
        if json_match:
            json_str = json_match.group(1)
        else:
            # Try to find any JSON block or just take the whole response
            json_str = result
        
        try:
            parsed_result = json.loads(json_str)
            return parsed_result
        except json.JSONDecodeError:
            print(f"Error parsing JSON from AI response: {json_str}")
            # Create a basic structure with the raw response
            return {
                "analysis": "Could not parse AI response properly",
                "issues": [],
                "recommendations": "See raw response",
                "response_for_issue": result
            }
            
    except Exception as e:
        print(f"Error calling OpenAI API: {e}")
        return {
            "analysis": f"Error during AI analysis: {str(e)}",
            "issues": [],
            "recommendations": "Could not complete analysis due to API error",
            "response_for_issue": "The AI assistant encountered an error processing this request."
        }

def create_patch_from_analysis(analysis):
    """Create a git patch file from the AI analysis."""
    patch_lines = []
    
    for issue in analysis.get('issues', []):
        file_path = issue.get('file')
        original = issue.get('original_code')
        fixed = issue.get('fixed_code')
        
        if not all([file_path, original, fixed]):
            continue
        
        # Skip if they're the same
        if original == fixed:
            continue
            
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                file_content = f.read()
                
            # Replace the code
            if original in file_content:
                new_content = file_content.replace(original, fixed)
                
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                
                # Add to patch info
                patch_lines.append(f"Modified {file_path}")
            else:
                print(f"Warning: Could not find the original code in {file_path}")
        except Exception as e:
            print(f"Error applying change to {file_path}: {e}")
    
    # Create patch summary
    if patch_lines:
        patch_path = Path(".github/ai-changes.patch")
        with open(patch_path, 'w', encoding='utf-8') as f:
            f.write("\n".join(patch_lines))
        
        return True
    
    return False

def create_ai_response(analysis, issue_number):
    """Create a markdown response for a GitHub issue."""
    response = f"## AI Analysis for Issue #{issue_number}\n\n"
    
    response += "### Analysis\n"
    response += analysis.get('analysis', 'No analysis provided') + "\n\n"
    
    if analysis.get('issues'):
        response += "### Issues Identified\n"
        for i, issue in enumerate(analysis['issues'], 1):
            response += f"**Issue {i}**: {issue.get('description', 'No description')}\n"
            response += f"File: `{issue.get('file', 'Not specified')}`\n"
            response += "```java\n// Original code\n" + issue.get('original_code', '') + "\n```\n"
            response += "```java\n// Fixed code\n" + issue.get('fixed_code', '') + "\n```\n\n"
    
    response += "### Recommendations\n"
    response += analysis.get('recommendations', 'No recommendations provided') + "\n\n"
    
    # Add information about automatic PR if any issues were found
    if analysis.get('issues'):
        response += "### Automated Fix\n"
        response += "I've created a pull request with these fixes for your review.\n\n"
    
    response += "---\n"
    response += "_This analysis was performed automatically by the AI Assistant._"
    
    # Save to file for the workflow
    with open(".github/ai-response.md", 'w', encoding='utf-8') as f:
        f.write(response)
    
    return response

def main():
    """Main function to process the AI task."""
    args = setup_argparse()
    headers = setup_github_api(args.token)
    
    # Set output variables (for GitHub Actions)
    output = {
        'create-pr': 'false',
        'commit-message': 'AI automated fix',
        'pr-title': 'AI: Automated code improvements',
        'pr-body': 'This PR contains automated fixes generated by the AI assistant.'
    }
    
    # Process based on task
    if args.task == 'analyze-issue' and args.issue:
        # Get issue details
        issue = get_issue_details(args.repo, args.issue, headers)
        if not issue:
            print("Could not retrieve issue details")
            return 1
        
        # Get relevant repository files
        files = get_repository_files(args.repo, headers)
        code_files = {}
        
        for file in files[:15]:  # Limit to 15 files to avoid token limits
            content = get_file_content(args.repo, file, headers)
            if content:
                code_files[file] = content
        
        # Analyze with AI
        analysis = analyze_code_with_ai(code_files, issue['body'])
        
        # Create response for the issue
        create_ai_response(analysis, args.issue)
        
        # Check if we should create a PR with fixes
        if analysis.get('issues'):
            created_patch = create_patch_from_analysis(analysis)
            if created_patch:
                output['create-pr'] = 'true'
                output['commit-message'] = f'AI: Fix issues from #{args.issue}'
                output['pr-title'] = f'AI: Fix for issue #{args.issue}'
                output['pr-body'] = f'This PR addresses issues identified in #{args.issue}\n\n{analysis.get("analysis")}'
    
    elif args.task in ['analyze-code', 'fix-bugs', 'implement-feature', 'improve-performance']:
        # Direct task from workflow dispatch
        files = get_repository_files(args.repo, headers)
        code_files = {}
        
        for file in files[:15]:  # Limit to 15 files to avoid token limits
            content = get_file_content(args.repo, file, headers)
            if content:
                code_files[file] = content
        
        # Analyze with AI
        analysis = analyze_code_with_ai(code_files, args.description)
        
        # Process the results
        if analysis.get('issues'):
            created_patch = create_patch_from_analysis(analysis)
            if created_patch:
                output['create-pr'] = 'true'
                output['commit-message'] = f'AI: {args.task}'
                output['pr-title'] = f'AI: {args.task.replace("-", " ").title()}'
                output['pr-body'] = f'This PR provides {args.task.replace("-", " ")} as requested.\n\n{analysis.get("analysis")}'
        
        # Save analysis for reference
        with open(".github/ai-analysis.json", 'w', encoding='utf-8') as f:
            json.dump(analysis, f, indent=2)
    
    # Set outputs for GitHub Actions
    with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
        for key, value in output.items():
            f.write(f"{key}={value}\n")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())