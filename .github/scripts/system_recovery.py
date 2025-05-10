#!/usr/bin/env python3
"""
System Recovery Script

This is a recovery tool that can restore the autonomous system to a working state
when something has critically failed. It can:

1. Reconstruct missing workflow files
2. Generate missing AI scripts
3. Reset critical system components
4. Detect and fix corrupted workflow definitions
5. Provide a "last resort" recovery mechanism
"""

import os
import sys
import glob
import json
import yaml
import logging
import shutil
import hashlib
import datetime
import subprocess
import openai
import requests

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('system_recovery')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_WORKSPACE = os.getenv("GITHUB_WORKSPACE") or os.getcwd()
SCRIPTS_DIR = os.path.join(GITHUB_WORKSPACE, ".github", "scripts")
WORKFLOWS_DIR = os.path.join(GITHUB_WORKSPACE, ".github", "workflows")

# Critical files that must exist
CRITICAL_SCRIPTS = [
    "ai_conversation_responder.py",
    "ai_implementation.py",
    "ai_triage_issue.py",
    "self_test_and_fix.py",
    "auto_fix_workflows.py",
    "system_recovery.py"
]

CRITICAL_WORKFLOWS = [
    "ai_improvement_pr.yml",
    "issue_triage.yml",
    "pr_comment_response.yml",
    "self_test_and_fix.yml",
    "auto_fix_system.yml"
]

def check_critical_files():
    """Check if all critical files exist and are valid"""
    missing_scripts = []
    for script in CRITICAL_SCRIPTS:
        script_path = os.path.join(SCRIPTS_DIR, script)
        if not os.path.exists(script_path):
            missing_scripts.append(script)
        else:
            # Check if file is valid Python
            try:
                with open(script_path, 'r') as f:
                    content = f.read()
                compile(content, script_path, 'exec')
            except Exception as e:
                logger.error(f"Script {script} is invalid: {e}")
                missing_scripts.append(script)  # Consider it missing if it's invalid
    
    missing_workflows = []
    for workflow in CRITICAL_WORKFLOWS:
        workflow_path = os.path.join(WORKFLOWS_DIR, workflow)
        if not os.path.exists(workflow_path):
            missing_workflows.append(workflow)
        else:
            # Check if file is valid YAML
            try:
                with open(workflow_path, 'r') as f:
                    yaml.safe_load(f)
            except Exception as e:
                logger.error(f"Workflow {workflow} is invalid YAML: {e}")
                missing_workflows.append(workflow)  # Consider it missing if it's invalid
    
    return missing_scripts, missing_workflows

def get_file_template(filename, file_type):
    """Get a template for recreating a missing file using GPT-4"""
    openai.api_key = OPENAI_API_KEY
    
    # Determine prompt based on file type and name
    if file_type == "script":
        if "conversation_responder" in filename:
            description = "GitHub issue/PR comment responder script that generates helpful responses using OpenAI API"
        elif "implementation" in filename:
            description = "Script that generates code implementations for issues labeled with 'ai-fix' or 'ai-implement'"
        elif "triage" in filename:
            description = "Script that analyzes new GitHub issues, categorizes them, and applies appropriate labels"
        elif "self_test" in filename:
            description = "Script that tests the repository code for issues and generates fixes"
        elif "auto_fix" in filename:
            description = "Script that fixes issues in workflow files and other AI scripts"
        elif "recovery" in filename:
            description = "Script that can recover the system from catastrophic failures by regenerating missing files"
        else:
            description = "A script for the autonomous development system"
    else:  # workflow
        if "improvement" in filename:
            description = "Workflow that triggers on 'ai-fix' label to generate a PR with fixes"
        elif "issue_triage" in filename:
            description = "Workflow that analyzes new issues and categorizes them"
        elif "comment_response" in filename:
            description = "Workflow that responds to comments on PRs and issues"
        elif "self_test" in filename:
            description = "Workflow that regularly tests the codebase for issues"
        elif "auto_fix" in filename:
            description = "Workflow that fixes workflows and scripts when they fail"
        else:
            description = "A workflow for the autonomous development system"
    
    # Generate the template using GPT-4
    prompt = f"""You need to create a file to restore a critical component of an autonomous development system for a GitHub repository.

File name: {filename}
Type: {file_type}
Purpose: {description}

This is for a repository that contains a Minecraft plugin recreating 7 Days To Die gameplay. The autonomous system uses AI to develop and improve the code without constant human intervention.

The file should:
1. Be complete and ready to use
2. Follow best practices and include proper error handling
3. Use environment variables properly (GITHUB_TOKEN, OPENAI_API_KEY, etc.)
4. Actually implement the functionality described

Generate ONLY the file content without any explanations or markdown code blocks."""

    try:
        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.2,
            max_tokens=4000
        )
        
        content = response['choices'][0]['message']['content'].strip()
        
        # Remove markdown code blocks if they exist
        if content.startswith("```"):
            content = content.split("```")[1]
            if content.startswith("python") or content.startswith("yaml"):
                content = content[content.find("\n")+1:]
        
        if content.endswith("```"):
            content = content[:content.rfind("```")]
        
        content = content.strip()
        
        return content
    except Exception as e:
        logger.error(f"Failed to get template for {filename}: {e}")
        return None

def recreate_critical_file(filename, file_type):
    """Recreate a missing critical file"""
    try:
        content = get_file_template(filename, file_type)
        
        if not content:
            logger.error(f"Failed to generate content for {filename}")
            return False
        
        # Determine the destination path
        if file_type == "script":
            dest_path = os.path.join(SCRIPTS_DIR, filename)
        else:  # workflow
            dest_path = os.path.join(WORKFLOWS_DIR, filename)
        
        # Ensure directory exists
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        
        # Write the file
        with open(dest_path, 'w') as f:
            f.write(content)
        
        # Validate the file
        if file_type == "script":
            # Make script executable
            os.chmod(dest_path, 0o755)
            
            # Check if it's valid Python
            try:
                with open(dest_path, 'r') as f:
                    content = f.read()
                compile(content, dest_path, 'exec')
                logger.info(f"Successfully recreated and validated script: {filename}")
                return True
            except Exception as e:
                logger.error(f"Recreated script {filename} is invalid: {e}")
                return False
        else:  # workflow
            # Check if it's valid YAML
            try:
                with open(dest_path, 'r') as f:
                    yaml.safe_load(f)
                logger.info(f"Successfully recreated and validated workflow: {filename}")
                return True
            except Exception as e:
                logger.error(f"Recreated workflow {filename} is invalid YAML: {e}")
                return False
    except Exception as e:
        logger.error(f"Failed to recreate {filename}: {e}")
        return False

def fix_directory_structure():
    """Ensure the directory structure is correct"""
    required_dirs = [
        os.path.join(GITHUB_WORKSPACE, ".github"),
        SCRIPTS_DIR,
        WORKFLOWS_DIR,
        os.path.join(GITHUB_WORKSPACE, "dashboard"),
        os.path.join(GITHUB_WORKSPACE, "dashboard", "src"),
        os.path.join(GITHUB_WORKSPACE, "dashboard", "src", "data"),
        os.path.join(GITHUB_WORKSPACE, "dashboard", "src", "pages"),
        os.path.join(GITHUB_WORKSPACE, "dashboard", "src", "components"),
        os.path.join(GITHUB_WORKSPACE, "dashboard", "public"),
        os.path.join(GITHUB_WORKSPACE, "plugin"),
        os.path.join(GITHUB_WORKSPACE, "plugin", "src"),
        os.path.join(GITHUB_WORKSPACE, "docs"),
        os.path.join(GITHUB_WORKSPACE, "docs", "images")
    ]
    
    for directory in required_dirs:
        if not os.path.exists(directory):
            os.makedirs(directory, exist_ok=True)
            logger.info(f"Created missing directory: {directory}")

def check_dashboard_data_files():
    """Ensure dashboard data files exist"""
    data_dir = os.path.join(GITHUB_WORKSPACE, "dashboard", "src", "data")
    required_files = [
        "ai_interactions.json",
        "ai_implementations.json",
        "issue_triage_logs.json",
        "health_check_logs.json",
        "self_test_logs.json",
        "auto_fix_logs.json"
    ]
    
    for file in required_files:
        file_path = os.path.join(data_dir, file)
        if not os.path.exists(file_path):
            with open(file_path, 'w') as f:
                f.write("[]")
            logger.info(f"Created missing dashboard data file: {file}")

def commit_restored_files(restored_files):
    """Commit and push restored files"""
    try:
        if not restored_files:
            logger.info("No files to commit")
            return True
        
        # Configure git
        subprocess.run(['git', 'config', '--global', 'user.name', 'System Recovery Bot'], check=True)
        subprocess.run(['git', 'config', '--global', 'user.email', 'recovery-bot@example.com'], check=True)
        
        # Stage files
        for file in restored_files:
            subprocess.run(['git', 'add', file], check=True)
        
        # Commit
        commit_message = "🔄 System Recovery: Restored critical files\n\n"
        commit_message += "The following files were restored by the system recovery process:\n\n"
        for file in restored_files:
            commit_message += f"- {file}\n"
        
        subprocess.run(['git', 'commit', '-m', commit_message], check=True)
        
        # Push
        subprocess.run(['git', 'push'], check=True)
        
        logger.info(f"Successfully committed and pushed {len(restored_files)} restored files")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"Git command failed: {e}")
        return False
    except Exception as e:
        logger.error(f"Failed to commit restored files: {e}")
        return False

def create_recovery_report(restored_files, remaining_issues):
    """Create an issue with a recovery report"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        title = "🚨 System Recovery Process Completed"
        
        body = "## 🛠️ System Recovery Report\n\n"
        body += "The autonomous system recovery process has completed.\n\n"
        
        if restored_files:
            body += "### 🔄 Restored Files\n\n"
            for file in restored_files:
                body += f"- ✅ {file}\n"
            body += "\n"
        
        if remaining_issues:
            body += "### ⚠️ Remaining Issues\n\n"
            for issue in remaining_issues:
                body += f"- ❌ {issue}\n"
            body += "\nManual intervention may be required to resolve these issues.\n\n"
        else:
            body += "### ✅ System Status\n\n"
            body += "All critical components have been restored and the system is operational.\n\n"
        
        body += f"Timestamp: {datetime.datetime.now().isoformat()}\n\n"
        body += "---\n*This issue was automatically created by the system recovery process.*"
        
        issue_data = {
            'title': title,
            'body': body,
            'labels': ['automated', 'system-recovery']
        }
        
        issue_url = f"https://api.github.com/repos/{GITHUB_REPOSITORY}/issues"
        response = requests.post(issue_url, headers=headers, json=issue_data)
        
        if response.status_code == 201:
            logger.info(f"Created recovery report issue: {response.json()['html_url']}")
            return True
        else:
            logger.error(f"Failed to create issue: {response.status_code} {response.text}")
            return False
    except Exception as e:
        logger.error(f"Failed to create recovery report: {e}")
        return False

def main():
    """Main function to recover the system"""
    try:
        logger.info("Starting system recovery")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        # Fix directory structure
        fix_directory_structure()
        
        # Check dashboard data files
        check_dashboard_data_files()
        
        # Check critical files
        missing_scripts, missing_workflows = check_critical_files()
        
        restored_files = []
        remaining_issues = []
        
        # Recreate missing scripts
        for script in missing_scripts:
            logger.info(f"Attempting to recreate missing script: {script}")
            success = recreate_critical_file(script, "script")
            
            if success:
                restored_files.append(os.path.join(SCRIPTS_DIR, script))
            else:
                remaining_issues.append(f"Failed to restore script: {script}")
        
        # Recreate missing workflows
        for workflow in missing_workflows:
            logger.info(f"Attempting to recreate missing workflow: {workflow}")
            success = recreate_critical_file(workflow, "workflow")
            
            if success:
                restored_files.append(os.path.join(WORKFLOWS_DIR, workflow))
            else:
                remaining_issues.append(f"Failed to restore workflow: {workflow}")
        
        # Commit restored files
        if restored_files:
            commit_success = commit_restored_files(restored_files)
            if not commit_success:
                remaining_issues.append("Failed to commit restored files")
        
        # Create recovery report
        create_recovery_report(restored_files, remaining_issues)
        
        logger.info("System recovery completed")
        
        # Exit with error if there are remaining issues
        if remaining_issues:
            logger.warning(f"Recovery completed with {len(remaining_issues)} remaining issues")
            sys.exit(1)
    except Exception as e:
        logger.error(f"Unexpected error in system recovery: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()