#!/usr/bin/env python3
"""
Self-Test and Fix Script

This script performs automated self-testing of the repository code and 
generates fixes for any detected issues. It represents a key component
of the autonomous development system's ability to self-improve.
"""

import os
import sys
import json
import logging
import subprocess
import time
from datetime import datetime
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('self_test_and_fix')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_WORKSPACE = os.getenv("GITHUB_WORKSPACE") or os.getcwd()
GITHUB_API_URL = "https://api.github.com"

def run_command(command, label=""):
    """Run a command and return the output and error"""
    logger.info(f"Running {label}: {command}")
    try:
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            shell=True,
            text=True
        )
        stdout, stderr = process.communicate()
        
        if process.returncode != 0:
            logger.warning(f"{label} failed with code {process.returncode}")
            logger.warning(f"Error: {stderr}")
            return False, stdout, stderr
        
        return True, stdout, stderr
    except Exception as e:
        logger.error(f"Error running {label}: {e}")
        return False, "", str(e)

def run_tests():
    """Run project tests"""
    success, stdout, stderr = run_command("mvn test", "Unit Tests")
    
    return {
        "success": success,
        "output": stdout,
        "error": stderr
    }

def run_checkstyle():
    """Run checkstyle analysis"""
    success, stdout, stderr = run_command("mvn checkstyle:check", "Checkstyle")
    
    return {
        "success": success,
        "output": stdout,
        "error": stderr
    }

def run_spotbugs():
    """Run SpotBugs analysis"""
    success, stdout, stderr = run_command("mvn spotbugs:check", "SpotBugs")
    
    return {
        "success": success,
        "output": stdout,
        "error": stderr
    }

def analyze_issues(test_results, checkstyle_results, spotbugs_results):
    """Analyze test, checkstyle, and spotbugs issues"""
    issues = []
    
    # Test failures
    if not test_results["success"]:
        errors = test_results["error"] or test_results["output"]
        if "BUILD FAILURE" in errors:
            issues.append({
                "type": "test_failure",
                "details": errors
            })
    
    # Checkstyle issues
    if not checkstyle_results["success"]:
        errors = checkstyle_results["error"] or checkstyle_results["output"]
        if "Checkstyle violations" in errors:
            issues.append({
                "type": "checkstyle",
                "details": errors
            })
    
    # SpotBugs issues
    if not spotbugs_results["success"]:
        errors = spotbugs_results["error"] or spotbugs_results["output"]
        if "SpotBugs violations" in errors:
            issues.append({
                "type": "spotbugs",
                "details": errors
            })
    
    return issues

def get_file_content(file_path):
    """Get the content of a file"""
    try:
        with open(file_path, 'r') as f:
            return f.read()
    except Exception as e:
        logger.error(f"Error reading file {file_path}: {e}")
        return None

def generate_fix(issue):
    """Generate a fix for an issue"""
    try:
        openai.api_key = OPENAI_API_KEY
        
        # Determine the file from the error message
        file_path = None
        if issue["type"] in ["checkstyle", "spotbugs"]:
            # Look for file paths in the error
            lines = issue["details"].split("\n")
            for line in lines:
                if "/src/main/java/" in line:
                    parts = line.split("/src/main/java/")
                    if len(parts) > 1:
                        partial_path = parts[1].split(":")[0].strip()
                        file_path = os.path.join(GITHUB_WORKSPACE, "src/main/java", partial_path)
                        break
        
        # Get file content if we found a file
        file_content = None
        if file_path and os.path.exists(file_path):
            file_content = get_file_content(file_path)
        
        # Create the prompt based on the issue type
        if issue["type"] == "test_failure":
            prompt = f"""You are an AI assistant for a Minecraft plugin development team.
You are examining test failures in a Maven Java project.

Here is the error output:
{issue["details"]}

Based on this error, provide a detailed analysis of what's going wrong and suggest a fix.
If you can identify specific files that need to be changed, explain what changes should be made.

Your response should be in the following JSON format:
```json
{{
  "analysis": "Detailed explanation of the issue",
  "fix_suggestion": "Specific code changes or actions to fix the issue",
  "files_to_modify": ["list of files that need to be changed, if identifiable"]
}}
```"""
        elif issue["type"] in ["checkstyle", "spotbugs"]:
            prompt = f"""You are an AI assistant for a Minecraft plugin development team.
You are examining {issue["type"]} issues in a Maven Java project.

Here is the error output:
{issue["details"]}

{"Here is the content of the identified file:" if file_content else ""}
{file_content if file_content else ""}

Based on this information, provide a detailed analysis of the {issue["type"]} issues and suggest fixes.
Be specific about what changes need to be made to fix each issue.

Your response should be in the following JSON format:
```json
{{
  "analysis": "Detailed explanation of the issues",
  "fix_suggestion": "Specific code changes to fix the issues",
  "files_to_modify": ["{os.path.basename(file_path)}" if file_path else ""]
}}
```"""
        else:
            prompt = f"""You are an AI assistant for a Minecraft plugin development team.
You are examining a build issue in a Maven Java project.

Here is the error output:
{issue["details"]}

Based on this information, provide a detailed analysis of the issue and suggest a fix.
Be specific about what changes need to be made to fix the issue.

Your response should be in the following JSON format:
```json
{{
  "analysis": "Detailed explanation of the issue",
  "fix_suggestion": "Specific code changes or actions to fix the issue",
  "files_to_modify": ["list of files that need to be changed, if identifiable"]
}}
```"""
        
        # Get AI response
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
                json_str = json_match.group(1)
                return json.loads(json_str)
            else:
                # Try to find any JSON object
                json_match = re.search(r'{.*}', result, re.DOTALL)
                if json_match:
                    json_str = json_match.group(0)
                    return json.loads(json_str)
                else:
                    logger.error("No JSON found in AI response")
                    return None
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from AI response: {e}")
            return None
    except Exception as e:
        logger.error(f"Failed to generate fix: {e}")
        return None

def create_issue_for_fix(analysis, issue_type):
    """Create a GitHub issue for the fix"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        title = f"[Auto] Fix {issue_type} issues detected by self-test"
        body = f"""## Automated Issue

This issue was automatically created by the self-test and fix system after detecting {issue_type} issues.

## Analysis

{analysis['analysis']}

## Fix Suggestion

{analysis['fix_suggestion']}

## Files to Modify

{', '.join(analysis['files_to_modify']) if analysis['files_to_modify'] else 'No specific files identified'}

---
*This issue was created by the autonomous development system. Please apply the 'ai-fix' label if you'd like the system to attempt an automatic fix.*
"""
        
        issue_data = {
            'title': title,
            'body': body,
            'labels': ['bug', 'automated']
        }
        
        issues_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues"
        response = requests.post(issues_url, headers=headers, json=issue_data)
        
        if response.status_code in (201, 200):
            issue_info = response.json()
            logger.info(f"Created issue #{issue_info['number']}: {issue_info['html_url']}")
            return issue_info
        else:
            logger.error(f"Failed to create issue: {response.status_code} {response.text}")
            return None
    except Exception as e:
        logger.error(f"Failed to create issue: {e}")
        return None

def log_to_dashboard(test_results, checkstyle_results, spotbugs_results, issues, fixes):
    """Log self-test results to the dashboard"""
    try:
        dashboard_dir = os.path.join(os.getcwd(), "dashboard", "src", "data")
        os.makedirs(dashboard_dir, exist_ok=True)
        
        log_file = os.path.join(dashboard_dir, "self_test_logs.json")
        
        # Create log entry
        log_entry = {
            "timestamp": datetime.now().isoformat(),
            "test_success": test_results["success"],
            "checkstyle_success": checkstyle_results["success"],
            "spotbugs_success": spotbugs_results["success"],
            "issues_found": len(issues),
            "fixes_generated": len(fixes),
            "issues": issues,
            "fixes": fixes
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
            
        logger.info(f"Logged self-test results to dashboard data: {log_file}")
    except Exception as e:
        logger.error(f"Failed to log to dashboard: {e}")

def main():
    """Main function to test and fix issues"""
    try:
        logger.info("Starting self-test and fix script")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Run tests
        logger.info("Running tests")
        test_results = run_tests()
        
        # Run checkstyle
        logger.info("Running checkstyle")
        checkstyle_results = run_checkstyle()
        
        # Run spotbugs
        logger.info("Running spotbugs")
        spotbugs_results = run_spotbugs()
        
        # Analyze issues
        issues = analyze_issues(test_results, checkstyle_results, spotbugs_results)
        logger.info(f"Found {len(issues)} issues")
        
        # Generate fixes for issues
        fixes = []
        for issue in issues:
            logger.info(f"Generating fix for {issue['type']} issue")
            fix = generate_fix(issue)
            if fix:
                fixes.append({
                    "issue_type": issue["type"],
                    "analysis": fix
                })
                
                # Create issue for fix
                issue_info = create_issue_for_fix(fix, issue["type"])
                if issue_info:
                    fix["issue_number"] = issue_info["number"]
                    fix["issue_url"] = issue_info["html_url"]
        
        logger.info(f"Generated {len(fixes)} fixes")
        
        # Log to dashboard
        log_to_dashboard(test_results, checkstyle_results, spotbugs_results, issues, fixes)
        
        logger.info("Self-test and fix script completed successfully")
        
        # Return success if no issues were found or if we generated fixes for all issues
        if len(issues) == 0 or len(fixes) == len(issues):
            return 0
        else:
            return 1
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())