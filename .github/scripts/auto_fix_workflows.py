#!/usr/bin/env python3
"""
Auto Fix Workflows Script

This script automatically analyzes workflows that have failed and fixes common issues:
1. Syntax errors in workflow YAML files
2. Missing dependencies in Python scripts
3. Missing permissions in workflow definitions
4. Environment variable issues
5. Self-healing for the AI scripts themselves
"""

import os
import sys
import re
import json
import yaml
import glob
import logging
import subprocess
import datetime
import openai
import requests
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('auto_fix_workflows')

# Global variables
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
GITHUB_REPOSITORY = os.getenv("GITHUB_REPOSITORY")
GITHUB_WORKSPACE = os.getenv("GITHUB_WORKSPACE") or os.getcwd()
GITHUB_API_URL = "https://api.github.com"
WORKFLOW_DIR = os.path.join(GITHUB_WORKSPACE, ".github", "workflows")
SCRIPTS_DIR = os.path.join(GITHUB_WORKSPACE, ".github", "scripts")

def check_workflow_syntax(workflow_file):
    """Check YAML syntax of a workflow file"""
    try:
        with open(workflow_file, 'r') as f:
            yaml.safe_load(f)
        return True, None
    except yaml.YAMLError as e:
        return False, str(e)

def fix_workflow_yaml(workflow_file, error_message):
    """Fix YAML syntax issues in a workflow file"""
    try:
        with open(workflow_file, 'r') as f:
            content = f.read()
        
        # Use AI to fix the YAML
        openai.api_key = OPENAI_API_KEY
        
        prompt = f"""Fix the YAML syntax error in this GitHub Actions workflow file:

```yaml
{content}
```

Error message: {error_message}

Common issues to check:
1. Indentation (must use spaces, not tabs)
2. Missing or extra colons
3. Missing quotes around strings that contain special characters
4. Improper list formatting
5. Multiline string formatting

Return ONLY the corrected YAML content with no explanations or comments."""

        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=2000
        )
        
        fixed_yaml = response['choices'][0]['message']['content'].strip()
        
        # Validate the fixed YAML
        try:
            yaml.safe_load(fixed_yaml)
            
            # Save the fixed content
            with open(workflow_file, 'w') as f:
                f.write(fixed_yaml)
                
            return True, f"Fixed YAML syntax in {os.path.basename(workflow_file)}"
        except yaml.YAMLError as e:
            return False, f"AI-generated YAML still has syntax errors: {str(e)}"
        
    except Exception as e:
        return False, f"Failed to fix workflow YAML: {str(e)}"

def check_workflow_run_history():
    """Check recent workflow runs for failures"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        # Get list of workflows
        workflows_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/actions/workflows"
        response = requests.get(workflows_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get workflows: {response.status_code} {response.text}")
            return []
        
        workflows = response.json()['workflows']
        
        failed_runs = []
        for workflow in workflows:
            # Get recent runs of this workflow
            runs_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/actions/workflows/{workflow['id']}/runs?status=failure&per_page=5"
            runs_response = requests.get(runs_url, headers=headers)
            
            if runs_response.status_code != 200:
                logger.error(f"Failed to get runs: {runs_response.status_code} {runs_response.text}")
                continue
            
            workflow_runs = runs_response.json()['workflow_runs']
            for run in workflow_runs:
                # Get logs URL if available
                if 'jobs_url' in run:
                    jobs_response = requests.get(run['jobs_url'], headers=headers)
                    if jobs_response.status_code == 200:
                        for job in jobs_response.json()['jobs']:
                            failed_runs.append({
                                'workflow_name': workflow['name'],
                                'workflow_file': workflow['path'],
                                'run_id': run['id'],
                                'conclusion': job.get('conclusion', 'unknown'),
                                'job_name': job['name'],
                                'logs_url': job.get('logs_url', None),
                                'steps': job.get('steps', [])
                            })
        
        return failed_runs
    except Exception as e:
        logger.error(f"Failed to check workflow run history: {e}")
        return []

def get_workflow_logs(logs_url):
    """Get logs of a workflow run"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        response = requests.get(logs_url, headers=headers)
        
        if response.status_code != 200:
            logger.error(f"Failed to get logs: {response.status_code} {response.text}")
            return ""
        
        return response.text
    except Exception as e:
        logger.error(f"Failed to get workflow logs: {e}")
        return ""

def extract_missing_dependencies(logs):
    """Extract missing Python dependencies from logs"""
    # Common patterns for missing dependencies
    patterns = [
        r"No module named '([^']+)'",
        r"ImportError: cannot import name '([^']+)' from '([^']+)'",
        r"ModuleNotFoundError: No module named '([^']+)'",
        r"ImportError: cannot import name '([^']+)'",
        r"from: can't read /var/mail/([^\\s]+)",
        r"pip: command not found"
    ]
    
    missing_deps = set()
    
    for pattern in patterns:
        matches = re.findall(pattern, logs)
        if matches:
            for match in matches:
                if isinstance(match, tuple):
                    missing_deps.add(match[0])
                else:
                    missing_deps.add(match)
    
    return list(missing_deps)

def fix_missing_dependencies(workflow_file, missing_deps):
    """Fix missing dependencies in a workflow file"""
    try:
        with open(workflow_file, 'r') as f:
            content = yaml.safe_load(f)
        
        # Check each job for Python setup steps
        modified = False
        for job_name, job in content.get('jobs', {}).items():
            for step_idx, step in enumerate(job.get('steps', [])):
                # Find pip install steps
                if any(cmd in step.get('run', '') for cmd in ['pip install', 'python -m pip']):
                    # Add missing dependencies to the pip install command
                    run_cmd = step['run']
                    for dep in missing_deps:
                        if dep not in run_cmd:
                            if 'pip install' in run_cmd:
                                line_with_pip = [l for l in run_cmd.split('\n') if 'pip install' in l][0]
                                new_line = f"{line_with_pip} {dep}"
                                run_cmd = run_cmd.replace(line_with_pip, new_line)
                            else:
                                run_cmd += f"\npip install {dep}"
                    
                    if run_cmd != step['run']:
                        content['jobs'][job_name]['steps'][step_idx]['run'] = run_cmd
                        modified = True
        
        if modified:
            with open(workflow_file, 'w') as f:
                yaml.dump(content, f, sort_keys=False)
            
            return True, f"Added missing dependencies ({', '.join(missing_deps)}) to {os.path.basename(workflow_file)}"
        else:
            # If no existing pip step found, add a new step
            for job_name, job in content.get('jobs', {}).items():
                for step_idx, step in enumerate(job.get('steps', [])):
                    # Find Python setup step and add after it
                    if step.get('uses', '').startswith('actions/setup-python'):
                        new_step = {
                            'name': 'Install dependencies',
                            'run': f"python -m pip install --upgrade pip\npip install {' '.join(missing_deps)}"
                        }
                        content['jobs'][job_name]['steps'].insert(step_idx + 1, new_step)
                        
                        with open(workflow_file, 'w') as f:
                            yaml.dump(content, f, sort_keys=False)
                        
                        return True, f"Added new step with missing dependencies ({', '.join(missing_deps)}) to {os.path.basename(workflow_file)}"
            
            return False, f"Could not add missing dependencies to {os.path.basename(workflow_file)}"
    except Exception as e:
        return False, f"Failed to fix missing dependencies: {str(e)}"

def check_scripts_syntax():
    """Check Python syntax of all scripts in .github/scripts"""
    script_issues = []
    
    for script_file in glob.glob(os.path.join(SCRIPTS_DIR, "*.py")):
        try:
            # Check syntax
            result = subprocess.run(['python', '-m', 'py_compile', script_file], 
                                  capture_output=True, text=True)
            
            if result.returncode != 0:
                script_issues.append({
                    'file': script_file,
                    'issue_type': 'syntax',
                    'error': result.stderr
                })
                continue
            
            # Check for common issues like hardcoded values
            with open(script_file, 'r') as f:
                content = f.read()
            
            # Check for missing environment variable checks
            env_vars = ['GITHUB_TOKEN', 'OPENAI_API_KEY', 'GITHUB_REPOSITORY', 
                       'GITHUB_WORKSPACE', 'GITHUB_EVENT_PATH']
            
            for var in env_vars:
                if var in content and f"os.getenv(\"{var}\")" not in content and f"os.environ[\"{var}\"]" not in content:
                    script_issues.append({
                        'file': script_file,
                        'issue_type': 'env_var',
                        'error': f"Environment variable {var} is used but not properly checked"
                    })
            
            # Check for exception handling around critical sections
            if "except Exception as e:" not in content and "except:" not in content:
                script_issues.append({
                    'file': script_file,
                    'issue_type': 'exception_handling',
                    'error': "Missing exception handling"
                })
            
            # Check for token usage validation
            if "GITHUB_TOKEN" in content and "if not GITHUB_TOKEN:" not in content:
                script_issues.append({
                    'file': script_file,
                    'issue_type': 'token_validation',
                    'error': "GITHUB_TOKEN usage without validation"
                })
            
            # Check for OpenAI API key validation
            if "OPENAI_API_KEY" in content and "if not OPENAI_API_KEY:" not in content:
                script_issues.append({
                    'file': script_file,
                    'issue_type': 'api_key_validation',
                    'error': "OPENAI_API_KEY usage without validation"
                })
            
        except Exception as e:
            script_issues.append({
                'file': script_file,
                'issue_type': 'analysis_error',
                'error': str(e)
            })
    
    return script_issues

def fix_script_issue(script_file, issue_type, error_message):
    """Fix issues in Python scripts"""
    try:
        with open(script_file, 'r') as f:
            content = f.read()
        
        # Use AI to fix the script
        openai.api_key = OPENAI_API_KEY
        
        prompt = f"""Fix the following issue in this Python script:

```python
{content}
```

Issue type: {issue_type}
Error message: {error_message}

Common fixes to implement:
1. For syntax issues: Fix the syntax error in the code
2. For env_var issues: Add proper environment variable checks
3. For exception_handling: Add try/except blocks around critical code
4. For token_validation: Add validation checks for GITHUB_TOKEN
5. For api_key_validation: Add validation checks for OPENAI_API_KEY

Return ONLY the corrected Python code with no explanations or markdown."""

        response = openai.ChatCompletion.create(
            model="gpt-4-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1,
            max_tokens=4000
        )
        
        fixed_script = response['choices'][0]['message']['content'].strip()
        
        # If the response contains markdown code blocks, extract the code
        if fixed_script.startswith("```python"):
            fixed_script = fixed_script[10:]
        if fixed_script.endswith("```"):
            fixed_script = fixed_script[:-3]
        
        fixed_script = fixed_script.strip()
        
        # Validate the fixed script
        try:
            # Write to a temporary file for validation
            temp_file = f"{script_file}.temp"
            with open(temp_file, 'w') as f:
                f.write(fixed_script)
            
            # Check syntax
            result = subprocess.run(['python', '-m', 'py_compile', temp_file], 
                                  capture_output=True, text=True)
            
            if result.returncode != 0:
                os.remove(temp_file)
                return False, f"AI-generated script still has syntax errors: {result.stderr}"
            
            # Successful validation, replace the original file
            os.replace(temp_file, script_file)
            return True, f"Fixed {issue_type} issue in {os.path.basename(script_file)}"
            
        except Exception as e:
            if os.path.exists(temp_file):
                os.remove(temp_file)
            return False, f"Failed to validate fixed script: {str(e)}"
        
    except Exception as e:
        return False, f"Failed to fix script issue: {str(e)}"

def check_missing_permissions(workflow_file):
    """Check for missing permissions in workflows"""
    try:
        with open(workflow_file, 'r') as f:
            content = yaml.safe_load(f)
        
        missing_permissions = []
        
        # Check if the workflow uses specific APIs or tools that need permissions
        workflow_content_str = yaml.dump(content)
        
        # Check for GitHub token usage without proper permissions
        if "GITHUB_TOKEN" in workflow_content_str and "permissions:" not in workflow_content_str:
            missing_permissions.append("github_token")
        
        # Check for specific API access patterns
        if re.search(r"repos/[^/]+/[^/]+/issues", workflow_content_str) and not re.search(r"permissions:.*issues:\s*write", workflow_content_str, re.DOTALL):
            missing_permissions.append("issues")
        
        if re.search(r"repos/[^/]+/[^/]+/pulls", workflow_content_str) and not re.search(r"permissions:.*pull-requests:\s*write", workflow_content_str, re.DOTALL):
            missing_permissions.append("pull-requests")
        
        if "actions/deploy-pages" in workflow_content_str and not re.search(r"permissions:.*pages:\s*write", workflow_content_str, re.DOTALL):
            missing_permissions.append("pages")
        
        if re.search(r"git\s+push", workflow_content_str) and not re.search(r"permissions:.*contents:\s*write", workflow_content_str, re.DOTALL):
            missing_permissions.append("contents")
        
        return missing_permissions
    except Exception as e:
        logger.error(f"Failed to check permissions in {workflow_file}: {e}")
        return []

def fix_missing_permissions(workflow_file, missing_permissions):
    """Add missing permissions to a workflow file"""
    try:
        with open(workflow_file, 'r') as f:
            content = yaml.safe_load(f)
        
        # Map of permission types to their properties
        permission_map = {
            "github_token": {"contents": "read"},
            "issues": {"issues": "write"},
            "pull-requests": {"pull-requests": "write"},
            "pages": {"pages": "write", "id-token": "write"},
            "contents": {"contents": "write"}
        }
        
        # Create permissions block if it doesn't exist
        if 'permissions' not in content:
            content['permissions'] = {}
        
        # Add missing permissions
        for perm in missing_permissions:
            if perm in permission_map:
                for key, value in permission_map[perm].items():
                    content['permissions'][key] = value
        
        # Write back to file
        with open(workflow_file, 'w') as f:
            yaml.dump(content, f, sort_keys=False)
        
        return True, f"Added missing permissions ({', '.join(missing_permissions)}) to {os.path.basename(workflow_file)}"
    except Exception as e:
        return False, f"Failed to fix missing permissions: {str(e)}"

def analyze_logs_for_issues(logs, workflow_file):
    """Analyze workflow logs for common issues"""
    issues = []
    
    # Check for common error patterns
    if "No module named" in logs:
        deps = extract_missing_dependencies(logs)
        if deps:
            issues.append({
                'issue_type': 'missing_dependencies',
                'details': f"Missing Python dependencies: {', '.join(deps)}",
                'fix_data': deps
            })
    
    if "permission denied" in logs.lower() or "not authorized" in logs.lower():
        issues.append({
            'issue_type': 'permission_issue',
            'details': "Possible permissions issue in workflow",
            'fix_data': check_missing_permissions(workflow_file)
        })
    
    if "could not access file" in logs.lower() or "no such file or directory" in logs.lower():
        issues.append({
            'issue_type': 'file_access',
            'details': "File access issues in workflow",
            'fix_data': None
        })
    
    if "resource exhausted" in logs.lower() or "timed out" in logs.lower():
        issues.append({
            'issue_type': 'resource_limit',
            'details': "Workflow hitting resource limits",
            'fix_data': None
        })
    
    return issues

def fix_workflow_issues(workflow_file, issues):
    """Fix identified issues in a workflow file"""
    fixes_applied = []
    
    for issue in issues:
        if issue['issue_type'] == 'missing_dependencies' and issue['fix_data']:
            success, message = fix_missing_dependencies(workflow_file, issue['fix_data'])
            if success:
                fixes_applied.append(message)
        
        elif issue['issue_type'] == 'permission_issue' and issue['fix_data']:
            success, message = fix_missing_permissions(workflow_file, issue['fix_data'])
            if success:
                fixes_applied.append(message)
    
    return fixes_applied

def commit_and_push_fixes(files_changed, fixes_applied):
    """Commit and push fixes to the repository"""
    try:
        # Set Git configuration
        subprocess.run(['git', 'config', '--global', 'user.name', 'AI Auto-Fix Bot'], check=True)
        subprocess.run(['git', 'config', '--global', 'user.email', 'ai-bot@example.com'], check=True)
        
        # Stage changes
        for file in files_changed:
            subprocess.run(['git', 'add', file], check=True)
        
        # Create commit message
        commit_message = "🤖 Auto-fix workflow and script issues\n\n"
        for fix in fixes_applied:
            commit_message += f"- {fix}\n"
        
        # Commit changes
        subprocess.run(['git', 'commit', '-m', commit_message], check=True)
        
        # Push changes
        subprocess.run(['git', 'push'], check=True)
        
        return True, "Successfully committed and pushed fixes"
    except subprocess.CalledProcessError as e:
        return False, f"Failed to commit and push fixes: {e}"
    except Exception as e:
        return False, f"Error during commit and push: {e}"

def create_auto_fixed_issue(fixes_applied):
    """Create an issue to document the auto-fixes"""
    try:
        headers = {
            'Authorization': f'token {GITHUB_TOKEN}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        title = "🤖 Autonomous System Self-Improvement Report"
        body = "## 🛠️ Auto-Fix System Report\n\n"
        body += "The autonomous system detected and fixed the following issues:\n\n"
        
        for fix in fixes_applied:
            body += f"- ✅ {fix}\n"
        
        body += "\n## 📊 System Health\n\n"
        body += "The autonomous system is now running with improved reliability.\n\n"
        body += f"Timestamp: {datetime.datetime.now().isoformat()}\n\n"
        body += "---\n*This issue was automatically created by the self-healing system.*"
        
        issue_data = {
            'title': title,
            'body': body,
            'labels': ['automated', 'self-improvement']
        }
        
        issue_url = f"{GITHUB_API_URL}/repos/{GITHUB_REPOSITORY}/issues"
        response = requests.post(issue_url, headers=headers, json=issue_data)
        
        if response.status_code in [201, 200]:
            logger.info(f"Created auto-fix documentation issue: {response.json()['html_url']}")
            return True
        else:
            logger.error(f"Failed to create issue: {response.status_code} {response.text}")
            return False
    except Exception as e:
        logger.error(f"Failed to create auto-fix documentation issue: {e}")
        return False

def log_to_dashboard(fixes_applied):
    """Log auto-fixes to dashboard data"""
    try:
        dashboard_dir = os.path.join(GITHUB_WORKSPACE, "dashboard", "src", "data")
        os.makedirs(dashboard_dir, exist_ok=True)
        
        log_file = os.path.join(dashboard_dir, "auto_fix_logs.json")
        
        # Create log entry
        log_entry = {
            "timestamp": datetime.datetime.now().isoformat(),
            "fixes_count": len(fixes_applied),
            "fixes": fixes_applied
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
            
        logger.info(f"Logged auto-fixes to dashboard data: {log_file}")
        return True
    except Exception as e:
        logger.error(f"Failed to log to dashboard: {e}")
        return False

def main():
    """Main function to auto-fix workflows"""
    try:
        logger.info("Starting auto-fix workflows")
        
        # Check for required environment variables
        if not all([GITHUB_TOKEN, OPENAI_API_KEY, GITHUB_REPOSITORY]):
            logger.error("Missing required environment variables")
            sys.exit(1)
        
        files_changed = []
        fixes_applied = []
        
        # First, check all workflow YAML files for syntax errors
        logger.info("Checking workflow files for syntax errors")
        for workflow_file in glob.glob(os.path.join(WORKFLOW_DIR, "*.yml")):
            success, error = check_workflow_syntax(workflow_file)
            if not success:
                logger.warning(f"Syntax error in {workflow_file}: {error}")
                fix_success, fix_message = fix_workflow_yaml(workflow_file, error)
                if fix_success:
                    files_changed.append(workflow_file)
                    fixes_applied.append(fix_message)
                    logger.info(fix_message)
        
        # Check for missing permissions in workflow files
        logger.info("Checking workflow files for missing permissions")
        for workflow_file in glob.glob(os.path.join(WORKFLOW_DIR, "*.yml")):
            missing_perms = check_missing_permissions(workflow_file)
            if missing_perms:
                logger.warning(f"Missing permissions in {workflow_file}: {', '.join(missing_perms)}")
                fix_success, fix_message = fix_missing_permissions(workflow_file, missing_perms)
                if fix_success:
                    files_changed.append(workflow_file)
                    fixes_applied.append(fix_message)
                    logger.info(fix_message)
        
        # Check Python scripts for issues
        logger.info("Checking Python scripts for issues")
        script_issues = check_scripts_syntax()
        for issue in script_issues:
            logger.warning(f"Script issue in {issue['file']}: {issue['error']}")
            fix_success, fix_message = fix_script_issue(issue['file'], issue['issue_type'], issue['error'])
            if fix_success:
                files_changed.append(issue['file'])
                fixes_applied.append(fix_message)
                logger.info(fix_message)
        
        # Check recent workflow runs for failures
        logger.info("Checking recent workflow runs for failures")
        failed_runs = check_workflow_run_history()
        for run in failed_runs:
            if run['logs_url']:
                logs = get_workflow_logs(run['logs_url'])
                workflow_file = os.path.join(GITHUB_WORKSPACE, run['workflow_file'])
                
                if logs and os.path.exists(workflow_file):
                    issues = analyze_logs_for_issues(logs, workflow_file)
                    if issues:
                        for issue in issues:
                            logger.warning(f"Found issue in {workflow_file}: {issue['details']}")
                        
                        fixes = fix_workflow_issues(workflow_file, issues)
                        if fixes:
                            files_changed.append(workflow_file)
                            fixes_applied.extend(fixes)
        
        # Commit and push fixes if any were applied
        if files_changed:
            # Remove duplicates
            files_changed = list(set(files_changed))
            
            logger.info(f"Applying {len(fixes_applied)} fixes to {len(files_changed)} files")
            commit_success, commit_message = commit_and_push_fixes(files_changed, fixes_applied)
            
            if commit_success:
                logger.info(commit_message)
                
                # Create documentation issue
                create_auto_fixed_issue(fixes_applied)
                
                # Log to dashboard
                log_to_dashboard(fixes_applied)
            else:
                logger.error(commit_message)
        else:
            logger.info("No issues found to fix")
        
        logger.info("Auto-fix workflows completed successfully")
    except Exception as e:
        logger.error(f"Unexpected error in main function: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()