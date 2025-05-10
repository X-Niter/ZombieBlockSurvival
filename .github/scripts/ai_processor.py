#!/usr/bin/env python3
"""
AI Processor for GitHub Actions

This script processes GitHub issues and workflow dispatch events, analyzing code and issues
to generate automated fixes and responses using AI.
"""

import os
import sys
import json
import argparse
import requests
from typing import Dict, List, Any, Optional

def setup_argparse():
    """Set up argument parsing for the script."""
    parser = argparse.ArgumentParser(description='Process GitHub issues with AI')
    parser.add_argument('--issue-number', type=int, help='GitHub issue number to process')
    parser.add_argument('--repo', type=str, help='Repository name in format owner/repo')
    parser.add_argument('--token', type=str, help='GitHub token for authentication')
    parser.add_argument('--api-key', type=str, help='OpenAI API key')
    parser.add_argument('--mode', choices=['issue', 'pr', 'scan'], 
                        default='issue', help='Processing mode')
    return parser.parse_args()

def setup_github_api(token):
    """Configure the GitHub API."""
    return {
        'Authorization': f'token {token}',
        'Accept': 'application/vnd.github.v3+json'
    }

def get_issue_details(repo, issue_number, headers):
    """Fetch details about a GitHub issue."""
    url = f'https://api.github.com/repos/{repo}/issues/{issue_number}'
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Error fetching issue: {response.status_code}")
        print(response.json())
        sys.exit(1)
    return response.json()

def get_repository_files(repo, headers, path=""):
    """Recursively get repository files."""
    url = f'https://api.github.com/repos/{repo}/contents/{path}'
    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Error fetching repository contents: {response.status_code}")
        return []
    
    contents = response.json()
    files = []
    
    for item in contents:
        if item['type'] == 'file' and item['name'].endswith('.java'):
            files.append({
                'path': item['path'],
                'download_url': item['download_url']
            })
        elif item['type'] == 'dir':
            files.extend(get_repository_files(repo, headers, item['path']))
    
    return files

def get_file_content(repo, file_path, headers):
    """Get content of a specific file."""
    url = f'https://api.github.com/repos/{repo}/contents/{file_path}'
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
    api_key = os.environ.get('OPENAI_API_KEY')
    if not api_key:
        print("OpenAI API key is not set")
        return {
            "analysis": "No AI analysis available - API key not configured",
            "recommendations": [],
            "code_fixes": {}
        }
    
    # This is a placeholder for the actual OpenAI API call
    # In a real implementation, you would format the code and issue
    # into a prompt and send it to the OpenAI API
    
    print("Analyzing code with AI...")
    print(f"Number of files to analyze: {len(code_files)}")
    if issue_description:
        print(f"Issue description: {issue_description[:100]}...")
    
    # Return mock analysis for demonstration
    return {
        "analysis": "This is a placeholder for AI-generated code analysis",
        "recommendations": [
            "Implement error handling for database connection failures",
            "Add logging for critical operations",
            "Optimize database queries"
        ],
        "code_fixes": {
            "src/main/java/com/seventodie/utils/DatabaseManager.java": [
                {
                    "line": 57,
                    "original": "connection = DriverManager.getConnection(\"jdbc:sqlite:\" + databaseFile.getAbsolutePath());",
                    "replacement": "try {\n    connection = DriverManager.getConnection(\"jdbc:sqlite:\" + databaseFile.getAbsolutePath());\n} catch (SQLException e) {\n    plugin.getLogger().log(Level.SEVERE, \"Failed to connect to database\", e);\n}"
                }
            ]
        }
    }

def create_patch_from_analysis(analysis):
    """Create a git patch file from the AI analysis."""
    if not analysis or 'code_fixes' not in analysis:
        return None
    
    patches = []
    for file_path, fixes in analysis['code_fixes'].items():
        # In a real implementation, you would create proper git patches
        # This is a simplified version that just outputs the changes
        patch = f"--- a/{file_path}\n+++ b/{file_path}\n"
        for fix in fixes:
            patch += f"@@ -{fix['line']},1 +{fix['line']},5 @@\n"
            patch += f"-{fix['original']}\n+{fix['replacement']}\n"
        patches.append(patch)
    
    return "\n".join(patches)

def create_ai_response(analysis, issue_number):
    """Create a markdown response for a GitHub issue."""
    if not analysis:
        return "Unable to perform AI analysis at this time."
    
    response = f"## AI Analysis for Issue #{issue_number}\n\n"
    response += f"{analysis.get('analysis', 'No analysis available')}\n\n"
    
    if 'recommendations' in analysis and analysis['recommendations']:
        response += "### Recommendations\n\n"
        for i, rec in enumerate(analysis['recommendations'], 1):
            response += f"{i}. {rec}\n"
    
    if 'code_fixes' in analysis and analysis['code_fixes']:
        response += "\n### Proposed Code Changes\n\n"
        for file_path, fixes in analysis['code_fixes'].items():
            response += f"**{file_path}**\n\n"
            for fix in fixes:
                response += "```diff\n"
                response += f"- {fix['original']}\n+ {fix['replacement']}\n"
                response += "```\n\n"
    
    response += "\nI'll create a pull request with these changes shortly. Please review and provide feedback."
    
    return response

def main():
    """Main function to process the AI task."""
    args = setup_argparse()
    
    # Ensure required arguments are provided
    if not args.repo or not args.token:
        print("Repository and GitHub token are required")
        sys.exit(1)
    
    # Set up GitHub API
    headers = setup_github_api(args.token)
    
    # Set OpenAI API key if provided
    if args.api_key:
        os.environ['OPENAI_API_KEY'] = args.api_key
    
    # Process based on mode
    if args.mode == 'issue' and args.issue_number:
        # Get issue details
        issue = get_issue_details(args.repo, args.issue_number, headers)
        
        # Get repository files for analysis
        code_files = get_repository_files(args.repo, headers)
        
        # Load file contents
        for file in code_files:
            file['content'] = get_file_content(args.repo, file['path'], headers)
        
        # Analyze code with AI based on issue
        analysis = analyze_code_with_ai(code_files, issue['body'])
        
        # Create response for the issue
        response = create_ai_response(analysis, args.issue_number)
        
        # Output results for GitHub Actions
        with open(os.environ.get('GITHUB_OUTPUT', 'github_output.txt'), 'w') as f:
            f.write(f"analysis={json.dumps(analysis)}\n")
            f.write(f"response={response}\n")
        
        print("AI analysis complete!")
        
    elif args.mode == 'scan':
        # Perform a full repository scan
        code_files = get_repository_files(args.repo, headers)
        
        # Load file contents
        for file in code_files:
            file['content'] = get_file_content(args.repo, file['path'], headers)
        
        # Analyze code with AI
        analysis = analyze_code_with_ai(code_files)
        
        # Create a patch file if fixes are suggested
        patch = create_patch_from_analysis(analysis)
        if patch:
            with open('ai_fixes.patch', 'w') as f:
                f.write(patch)
        
        # Output results for GitHub Actions
        with open(os.environ.get('GITHUB_OUTPUT', 'github_output.txt'), 'w') as f:
            f.write(f"analysis={json.dumps(analysis)}\n")
            if patch:
                f.write(f"patch_file=ai_fixes.patch\n")
        
        print("Repository scan complete!")
    
    else:
        print(f"Unsupported mode: {args.mode} or missing issue number")
        sys.exit(1)

if __name__ == "__main__":
    main()