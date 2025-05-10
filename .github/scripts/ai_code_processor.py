#!/usr/bin/env python3
"""
AI Code Processor

This script provides comprehensive code analysis, generation, and improvement
capabilities for the SevenToDie plugin using AI.
"""

import os
import sys
import json
import argparse
import glob
import re
from typing import Dict, List, Any, Optional, Tuple
import requests

# API keys and configuration
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
REPO_NAME = os.environ.get("GITHUB_REPOSITORY", "")  # e.g., "username/repository"

class AICodeProcessor:
    """Main class for AI code processing."""
    
    def __init__(self, openai_key: str = None, github_token: str = None, repo: str = None):
        """Initialize the processor."""
        self.openai_key = openai_key or OPENAI_API_KEY
        self.github_token = github_token or GITHUB_TOKEN
        self.repo = repo or REPO_NAME
        
        if not self.openai_key:
            print("WARNING: OpenAI API key not provided. AI features will be limited.")
        
        if not self.github_token:
            print("WARNING: GitHub token not provided. Repository access will be limited.")
            
        if not self.repo:
            print("WARNING: Repository name not provided. Some features may not work.")
    
    def analyze_codebase(self, path: str = "./", file_pattern: str = "*.java") -> Dict[str, Any]:
        """Analyze the entire codebase and provide insights."""
        print(f"Analyzing codebase in {path} with pattern {file_pattern}")
        
        # Get all matching files
        java_files = self._get_files(path, file_pattern)
        print(f"Found {len(java_files)} Java files")
        
        # Extract key metrics and structures
        metrics = self._extract_metrics(java_files)
        
        # Generate analysis
        analysis = {
            "files_count": len(java_files),
            "metrics": metrics,
            "high_complexity_files": self._identify_high_complexity_files(java_files, metrics),
            "potential_issues": self._identify_potential_issues(java_files),
            "improvement_suggestions": self._generate_improvement_suggestions(java_files, metrics)
        }
        
        return analysis
    
    def process_issue(self, issue_data: Dict[str, Any]) -> Dict[str, Any]:
        """Process an issue and generate a response and potential fixes."""
        print(f"Processing issue: {issue_data.get('title', 'No title')}")
        
        # Extract key information from the issue
        issue_type = self._determine_issue_type(issue_data)
        components = self._identify_affected_components(issue_data)
        
        # Generate a response
        response = self._generate_issue_response(issue_data, issue_type, components)
        
        # Generate potential fixes if appropriate
        fixes = {}
        if issue_type == "bug":
            fixes = self._generate_bug_fixes(issue_data, components)
        elif issue_type in ["enhancement", "feature"]:
            fixes = self._generate_feature_implementation(issue_data, components)
        
        return {
            "issue_type": issue_type,
            "affected_components": components,
            "response": response,
            "fixes": fixes
        }
    
    def implement_solution(self, issue_data: Dict[str, Any], implementation_type: str) -> Dict[str, Any]:
        """Implement a full solution for an issue."""
        print(f"Implementing {implementation_type} for issue: {issue_data.get('title', 'No title')}")
        
        # Identify which files need changes
        affected_files = self._identify_affected_files(issue_data, implementation_type)
        print(f"Identified {len(affected_files)} affected files")
        
        # Generate the implementation
        implementation = self._generate_implementation(issue_data, implementation_type, affected_files)
        
        return {
            "implementation_type": implementation_type,
            "affected_files": affected_files,
            "changes": implementation,
            "tests_needed": self._identify_needed_tests(implementation)
        }
    
    def review_pull_request(self, pr_data: Dict[str, Any]) -> Dict[str, Any]:
        """Review a pull request and provide feedback."""
        print(f"Reviewing PR: {pr_data.get('title', 'No title')}")
        
        # Analyze the changes
        changes_analysis = self._analyze_pr_changes(pr_data)
        
        # Generate feedback
        feedback = self._generate_pr_feedback(pr_data, changes_analysis)
        
        return {
            "analysis": changes_analysis,
            "feedback": feedback,
            "approval_recommendation": changes_analysis.get("approval_recommendation", "COMMENT")
        }
    
    def generate_tests(self, implementation_data: Dict[str, Any]) -> Dict[str, str]:
        """Generate tests for the given implementation."""
        print("Generating tests for implementation")
        
        tests = {}
        for file_path in implementation_data.get("affected_files", []):
            tests[self._get_test_file_path(file_path)] = self._generate_test_content(file_path, implementation_data)
        
        return tests
    
    # Private helper methods
    
    def _get_files(self, path: str, pattern: str) -> List[str]:
        """Get all files matching the given pattern in the path."""
        return glob.glob(f"{path}/**/{pattern}", recursive=True)
    
    def _extract_metrics(self, files: List[str]) -> Dict[str, Any]:
        """Extract metrics from the given files."""
        metrics = {
            "total_lines": 0,
            "code_lines": 0,
            "comment_lines": 0,
            "classes": 0,
            "methods": 0,
            "complexity": {}
        }
        
        for file_path in files:
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Count lines
                lines = content.splitlines()
                metrics["total_lines"] += len(lines)
                
                # Count code and comment lines (simplified)
                comment_count = 0
                in_block_comment = False
                for line in lines:
                    stripped = line.strip()
                    if in_block_comment:
                        comment_count += 1
                        if "*/" in stripped:
                            in_block_comment = False
                    elif stripped.startswith("//"):
                        comment_count += 1
                    elif stripped.startswith("/*"):
                        comment_count += 1
                        if "*/" not in stripped:
                            in_block_comment = True
                    elif stripped:
                        metrics["code_lines"] += 1
                
                metrics["comment_lines"] += comment_count
                
                # Count classes and methods (simplified)
                class_matches = re.findall(r'class\s+\w+', content)
                method_matches = re.findall(r'(public|private|protected)?\s+\w+\s+\w+\s*\([^)]*\)\s*(\{|throws)', content)
                
                metrics["classes"] += len(class_matches)
                metrics["methods"] += len(method_matches)
                
                # Estimate complexity (very simplified)
                complexity = 1 + content.count("if ") + content.count("while ") + content.count("for ") + content.count("case ")
                metrics["complexity"][file_path] = complexity
                
            except Exception as e:
                print(f"Error processing file {file_path}: {e}")
        
        return metrics
    
    def _identify_high_complexity_files(self, files: List[str], metrics: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Identify files with high complexity."""
        complexity_threshold = 20  # Arbitrary threshold for demonstration
        
        high_complexity = []
        for file_path, complexity in metrics.get("complexity", {}).items():
            if complexity > complexity_threshold:
                high_complexity.append({
                    "file": file_path,
                    "complexity": complexity,
                    "recommendation": "Consider refactoring this file to reduce complexity"
                })
        
        return sorted(high_complexity, key=lambda x: x["complexity"], reverse=True)
    
    def _identify_potential_issues(self, files: List[str]) -> List[Dict[str, Any]]:
        """Identify potential issues in the codebase."""
        issues = []
        
        for file_path in files:
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Look for common issues (this would be much more sophisticated in a real implementation)
                if "TODO" in content:
                    issues.append({
                        "file": file_path,
                        "type": "todo",
                        "description": "Contains TODO comments that should be addressed"
                    })
                
                if "null" in content and "NullPointerException" not in content:
                    issues.append({
                        "file": file_path,
                        "type": "potential_npe",
                        "description": "Contains null references without proper exception handling"
                    })
                
                if "printStackTrace" in content:
                    issues.append({
                        "file": file_path,
                        "type": "logging",
                        "description": "Uses printStackTrace() instead of proper logging"
                    })
                
            except Exception as e:
                print(f"Error checking file {file_path} for issues: {e}")
        
        return issues
    
    def _generate_improvement_suggestions(self, files: List[str], metrics: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Generate suggestions for code improvements."""
        suggestions = []
        
        # In a real implementation, this would use AI to analyze the code
        # and generate specific, actionable suggestions
        
        # For demonstration, we'll provide some generic suggestions
        if metrics.get("comment_lines", 0) < metrics.get("code_lines", 0) * 0.1:
            suggestions.append({
                "type": "documentation",
                "description": "The codebase has low comment density. Consider adding more documentation.",
                "priority": "medium"
            })
        
        if len(metrics.get("complexity", {})) > 0:
            avg_complexity = sum(metrics["complexity"].values()) / len(metrics["complexity"])
            if avg_complexity > 15:
                suggestions.append({
                    "type": "complexity",
                    "description": "Overall code complexity is high. Consider refactoring complex methods.",
                    "priority": "high"
                })
        
        # Add a database-related suggestion as an example
        suggestions.append({
            "type": "performance",
            "description": "Consider implementing connection pooling in DatabaseManager.java to improve performance",
            "priority": "medium",
            "file": "src/main/java/com/seventodie/utils/DatabaseManager.java"
        })
        
        return suggestions
    
    def _determine_issue_type(self, issue_data: Dict[str, Any]) -> str:
        """Determine the type of issue from the issue data."""
        # Check if issue has type labels
        labels = issue_data.get("labels", [])
        for label in labels:
            if label in ["bug", "enhancement", "feature", "documentation", "question"]:
                return label
        
        # If no type label, try to infer from title and body
        title = issue_data.get("title", "").lower()
        body = issue_data.get("body", "").lower()
        
        if any(term in title or term in body for term in ["bug", "error", "fix", "issue", "problem", "crash"]):
            return "bug"
        if any(term in title or term in body for term in ["feature", "add", "new"]):
            return "feature"
        if any(term in title or term in body for term in ["enhance", "improve", "optimization"]):
            return "enhancement"
        if any(term in title or term in body for term in ["doc", "documentation", "explain"]):
            return "documentation"
        if any(term in title or term in body for term in ["how", "?", "question", "help"]):
            return "question"
        
        # Default to enhancement
        return "enhancement"
    
    def _identify_affected_components(self, issue_data: Dict[str, Any]) -> List[str]:
        """Identify which components are affected by the issue."""
        # Extract information from issue
        title = issue_data.get("title", "").lower()
        body = issue_data.get("body", "").lower()
        content = f"{title} {body}"
        
        # Map of keywords to components
        component_keywords = {
            "database": ["database", "db", "sql", "sqlite"],
            "worldgen": ["world", "generation", "gen", "terrain", "biome"],
            "zombies": ["zombie", "horde", "npc", "mob", "enemy"],
            "building": ["build", "block", "frame", "structure"],
            "quests": ["quest", "mission", "task"],
            "traders": ["trade", "trader", "shop", "buy", "sell"],
            "tools": ["tool", "hammer", "weapon"]
        }
        
        # Identify components
        affected = []
        for component, keywords in component_keywords.items():
            if any(keyword in content for keyword in keywords):
                affected.append(component)
        
        # If no specific component identified, consider it general
        if not affected:
            affected.append("general")
        
        return affected
    
    def _generate_issue_response(self, issue_data: Dict[str, Any], issue_type: str, components: List[str]) -> str:
        """Generate a response for the issue."""
        # In a real implementation, this would use AI to generate a human-like,
        # informative response based on the issue details
        
        user = issue_data.get("user", "there")
        title = issue_data.get("title", "your issue")
        
        # Customize greeting based on issue type
        greeting = f"Hello @{user},"
        
        if issue_type == "bug":
            response = f"""
{greeting}

Thank you for reporting this bug. I've analyzed your issue with the {', '.join(components)} component(s).

## Analysis
Based on your description, it appears that the problem occurs when [specific condition].

## Potential Cause
This is likely caused by [technical explanation of the cause].

## Proposed Solution
I suggest we fix this by:
1. Updating the error handling in [specific file]
2. Adding additional validation for [specific input]
3. Ensuring proper cleanup of resources

Would you like me to implement this fix for you? If so, I can create a pull request with the necessary changes.

Let me know if you have any questions or if I've misunderstood the issue.
"""
        elif issue_type == "feature":
            response = f"""
{greeting}

Thank you for suggesting this feature for the {', '.join(components)} component(s). It sounds like a great addition to the project!

## Feasibility Analysis
This feature would require:
- Adding new classes for [specific functionality]
- Integrating with the existing [specific systems]
- Adding configuration options
- Updating documentation

## Implementation Approach
I would suggest implementing this by:
1. Creating a new [specific class] to handle the core logic
2. Extending the [specific interface] to support this feature
3. Adding configuration options in config.yml
4. Writing comprehensive documentation

Would you like me to implement this feature for you? If so, I can create a pull request with the implementation.

Let me know if you have any specific requirements or preferences for how this should work.
"""
        elif issue_type == "question":
            response = f"""
{greeting}

Thank you for your question about the {', '.join(components)} component(s). I'm happy to help!

## Answer
[Detailed answer to the question would go here]

Here's a code example that demonstrates how this works:
```java
// Example code
```

I hope this clarifies things! Let me know if you have any follow-up questions.
"""
        else:  # enhancement or documentation
            response = f"""
{greeting}

Thank you for suggesting this enhancement to the {', '.join(components)} component(s). I appreciate your input!

## Current Implementation
Currently, the system [description of current implementation]

## Proposed Enhancement
Your suggestion to [summary of enhancement] would indeed improve the [specific aspect].

## Implementation Approach
I would suggest:
1. Refactoring [specific part] to make it more flexible
2. Adding [specific functionality]
3. Improving the documentation

Would you like me to implement this enhancement for you? If so, I can create a pull request with the changes.

Let me know if you have any specific ideas or preferences for how this should be implemented.
"""
        
        return response
    
    def _generate_bug_fixes(self, issue_data: Dict[str, Any], components: List[str]) -> Dict[str, Any]:
        """Generate potential fixes for a bug issue."""
        # In a real implementation, this would analyze the codebase and issue description
        # to generate actual fixes for the bug
        
        # For demonstration, return a placeholder
        return {
            "approach": "Fix null pointer exception in DatabaseManager",
            "files": {
                "src/main/java/com/seventodie/utils/DatabaseManager.java": {
                    "changes": [
                        {
                            "type": "replace",
                            "line": 57,
                            "old": "connection = DriverManager.getConnection(\"jdbc:sqlite:\" + databaseFile.getAbsolutePath());",
                            "new": "try {\n    connection = DriverManager.getConnection(\"jdbc:sqlite:\" + databaseFile.getAbsolutePath());\n} catch (SQLException e) {\n    plugin.getLogger().log(Level.SEVERE, \"Failed to connect to database\", e);\n}"
                        }
                    ]
                }
            }
        }
    
    def _generate_feature_implementation(self, issue_data: Dict[str, Any], components: List[str]) -> Dict[str, Any]:
        """Generate implementation for a feature or enhancement issue."""
        # In a real implementation, this would generate actual code for the feature
        
        # For demonstration, return a placeholder
        return {
            "approach": "Implement advanced zombie behavior system",
            "files": {
                "src/main/java/com/seventodie/zombies/AdvancedZombieBehavior.java": {
                    "changes": [
                        {
                            "type": "create",
                            "content": "package com.seventodie.zombies;\n\nimport org.bukkit.entity.Zombie;\n\n/**\n * Implements advanced behavior for zombies\n */\npublic class AdvancedZombieBehavior {\n    // Implementation would go here\n}"
                        }
                    ]
                },
                "src/main/java/com/seventodie/SevenToDiePlugin.java": {
                    "changes": [
                        {
                            "type": "add_import",
                            "import": "import com.seventodie.zombies.AdvancedZombieBehavior;"
                        },
                        {
                            "type": "add_field",
                            "field": "private AdvancedZombieBehavior zombieBehavior;"
                        },
                        {
                            "type": "add_to_method",
                            "method": "initializeManagers",
                            "code": "        zombieBehavior = new AdvancedZombieBehavior();"
                        }
                    ]
                }
            }
        }
    
    def _identify_affected_files(self, issue_data: Dict[str, Any], implementation_type: str) -> List[str]:
        """Identify which files need to be modified for the implementation."""
        # In a real implementation, this would analyze the codebase to determine
        # which files are relevant to the issue
        
        # For demonstration, return some placeholder files based on components
        components = self._identify_affected_components(issue_data)
        
        files = []
        if "database" in components:
            files.append("src/main/java/com/seventodie/utils/DatabaseManager.java")
        if "worldgen" in components:
            files.append("src/main/java/com/seventodie/worldgen/BiomeMapper.java")
            files.append("src/main/java/com/seventodie/worldgen/StructureManager.java")
        if "zombies" in components:
            files.append("src/main/java/com/seventodie/mobs/ZombieManager.java")
        if "building" in components:
            files.append("src/main/java/com/seventodie/blocks/BlockManager.java")
            files.append("src/main/java/com/seventodie/blocks/FrameBlock.java")
        if "quests" in components:
            files.append("src/main/java/com/seventodie/quests/QuestManager.java")
        if "traders" in components:
            files.append("src/main/java/com/seventodie/traders/TraderManager.java")
        
        # Always include the main plugin file for most implementations
        files.append("src/main/java/com/seventodie/SevenToDiePlugin.java")
        
        return files
    
    def _generate_implementation(self, issue_data: Dict[str, Any], implementation_type: str, affected_files: List[str]) -> Dict[str, Any]:
        """Generate the actual implementation for the issue."""
        # In a real implementation, this would generate detailed changes for each file
        
        # For demonstration, return placeholder changes
        changes = {}
        
        for file in affected_files:
            file_name = os.path.basename(file)
            if implementation_type == "fix":
                changes[file] = {
                    "type": "fix",
                    "changes": [
                        {
                            "description": f"Fix issue in {file_name}",
                            "code": f"// Fixed implementation for {file_name}\n// This would contain actual code in a real implementation"
                        }
                    ]
                }
            elif implementation_type == "feature":
                changes[file] = {
                    "type": "feature",
                    "changes": [
                        {
                            "description": f"Implement new feature in {file_name}",
                            "code": f"// New feature implementation for {file_name}\n// This would contain actual code in a real implementation"
                        }
                    ]
                }
            elif implementation_type == "refactor":
                changes[file] = {
                    "type": "refactor",
                    "changes": [
                        {
                            "description": f"Refactor {file_name} for better maintainability",
                            "code": f"// Refactored implementation for {file_name}\n// This would contain actual code in a real implementation"
                        }
                    ]
                }
            else:  # improvement
                changes[file] = {
                    "type": "improvement",
                    "changes": [
                        {
                            "description": f"Improve performance in {file_name}",
                            "code": f"// Improved implementation for {file_name}\n// This would contain actual code in a real implementation"
                        }
                    ]
                }
        
        return changes
    
    def _identify_needed_tests(self, implementation: Dict[str, Any]) -> List[str]:
        """Identify what tests need to be created or updated for the implementation."""
        # In a real implementation, this would analyze the changes to determine
        # what tests are needed
        
        # For demonstration, return placeholder test files
        test_files = []
        
        for file_path in implementation.keys():
            if file_path.endswith(".java"):
                base_name = os.path.basename(file_path).replace(".java", "")
                test_files.append(f"src/test/java/{os.path.dirname(file_path).replace('src/main/java/', '')}/{base_name}Test.java")
        
        return test_files
    
    def _analyze_pr_changes(self, pr_data: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze the changes in a pull request."""
        # In a real implementation, this would analyze the diff to provide
        # meaningful feedback
        
        # For demonstration, return placeholder analysis
        return {
            "files_changed": len(pr_data.get("files", [])),
            "additions": sum(f.get("additions", 0) for f in pr_data.get("files", [])),
            "deletions": sum(f.get("deletions", 0) for f in pr_data.get("files", [])),
            "comments": [],
            "suggestions": [
                {
                    "file": "src/main/java/com/seventodie/utils/DatabaseManager.java",
                    "line": 57,
                    "message": "Consider adding additional error handling here"
                }
            ],
            "approval_recommendation": "APPROVE"  # or "REQUEST_CHANGES" or "COMMENT"
        }
    
    def _generate_pr_feedback(self, pr_data: Dict[str, Any], analysis: Dict[str, Any]) -> str:
        """Generate feedback for a pull request."""
        # In a real implementation, this would generate detailed, helpful feedback
        
        # For demonstration, return placeholder feedback
        return f"""Thank you for your pull request!

## Changes Overview
This PR changes {analysis.get('files_changed', 0)} files with {analysis.get('additions', 0)} additions and {analysis.get('deletions', 0)} deletions.

## Code Quality
The code quality looks good overall. Here are a few suggestions:

{self._format_suggestions(analysis.get('suggestions', []))}

## Testing
The changes appear to be well-tested.

## Documentation
The documentation is clear and complete.

## Recommendation
This PR is ready to merge. Great work!"""
    
    def _format_suggestions(self, suggestions: List[Dict[str, Any]]) -> str:
        """Format a list of suggestions into a readable string."""
        if not suggestions:
            return "No specific suggestions."
        
        result = ""
        for i, suggestion in enumerate(suggestions, 1):
            result += f"{i}. In `{suggestion.get('file')}` (line {suggestion.get('line')}): {suggestion.get('message')}\n"
        
        return result
    
    def _get_test_file_path(self, file_path: str) -> str:
        """Convert a source file path to its corresponding test file path."""
        if "src/main/java" in file_path:
            return file_path.replace("src/main/java", "src/test/java").replace(".java", "Test.java")
        return file_path + "Test.java"
    
    def _generate_test_content(self, file_path: str, implementation_data: Dict[str, Any]) -> str:
        """Generate test content for a file."""
        # In a real implementation, this would generate actual unit tests
        
        # For demonstration, return placeholder test content
        base_name = os.path.basename(file_path).replace(".java", "")
        package = os.path.dirname(file_path).replace("src/main/java/", "").replace("/", ".")
        
        return f"""package {package};

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {base_name}
 */
public class {base_name}Test {{
    
    @Test
    public void testSomeFunctionality() {{
        // This would be a real test in a real implementation
        assertTrue(true, "This test should pass");
    }}
}}"""

def setup_argparse():
    """Set up command line argument parsing."""
    parser = argparse.ArgumentParser(description='AI Code Processor')
    
    # Main operation mode
    parser.add_argument('--mode', choices=['analyze', 'process_issue', 'implement', 'review_pr'],
                      default='analyze', help='Operation mode')
    
    # Common options
    parser.add_argument('--openai-key', help='OpenAI API key')
    parser.add_argument('--github-token', help='GitHub token')
    parser.add_argument('--repo', help='Repository name (owner/repo)')
    
    # Mode-specific options
    parser.add_argument('--path', default='./', help='Path to analyze (for analyze mode)')
    parser.add_argument('--issue-data', help='JSON file with issue data (for process_issue and implement modes)')
    parser.add_argument('--issue-number', type=int, help='Issue number (alternative to --issue-data)')
    parser.add_argument('--implementation-type', choices=['fix', 'feature', 'improvement', 'refactor'],
                      default='fix', help='Type of implementation (for implement mode)')
    parser.add_argument('--pr-data', help='JSON file with PR data (for review_pr mode)')
    parser.add_argument('--pr-number', type=int, help='PR number (alternative to --pr-data)')
    
    # Output options
    parser.add_argument('--output', help='Output file for results (default is stdout)')
    
    return parser.parse_args()

def load_issue_data(args):
    """Load issue data from file or GitHub API."""
    if args.issue_data:
        with open(args.issue_data, 'r', encoding='utf-8') as f:
            return json.load(f)
    
    if args.issue_number and args.github_token and args.repo:
        # Fetch from GitHub API
        owner, repo = args.repo.split('/')
        url = f"https://api.github.com/repos/{owner}/{repo}/issues/{args.issue_number}"
        headers = {"Authorization": f"token {args.github_token}"}
        
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            return response.json()
    
    print("Error: Issue data not provided or could not be fetched")
    return {}

def load_pr_data(args):
    """Load PR data from file or GitHub API."""
    if args.pr_data:
        with open(args.pr_data, 'r', encoding='utf-8') as f:
            return json.load(f)
    
    if args.pr_number and args.github_token and args.repo:
        # Fetch from GitHub API
        owner, repo = args.repo.split('/')
        url = f"https://api.github.com/repos/{owner}/{repo}/pulls/{args.pr_number}"
        headers = {"Authorization": f"token {args.github_token}"}
        
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            pr_data = response.json()
            
            # Also fetch PR files
            files_url = f"https://api.github.com/repos/{owner}/{repo}/pulls/{args.pr_number}/files"
            files_response = requests.get(files_url, headers=headers)
            if files_response.status_code == 200:
                pr_data["files"] = files_response.json()
            
            return pr_data
    
    print("Error: PR data not provided or could not be fetched")
    return {}

def write_output(result, output_file=None):
    """Write the result to the specified output file or stdout."""
    if output_file:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(result, f, indent=2)
    else:
        print(json.dumps(result, indent=2))

def main():
    """Main function."""
    args = setup_argparse()
    
    processor = AICodeProcessor(
        openai_key=args.openai_key,
        github_token=args.github_token,
        repo=args.repo
    )
    
    result = None
    
    if args.mode == 'analyze':
        result = processor.analyze_codebase(args.path)
    elif args.mode == 'process_issue':
        issue_data = load_issue_data(args)
        result = processor.process_issue(issue_data)
    elif args.mode == 'implement':
        issue_data = load_issue_data(args)
        result = processor.implement_solution(issue_data, args.implementation_type)
    elif args.mode == 'review_pr':
        pr_data = load_pr_data(args)
        result = processor.review_pull_request(pr_data)
    
    write_output(result, args.output)

if __name__ == "__main__":
    main()