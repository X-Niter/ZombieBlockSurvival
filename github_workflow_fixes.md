# GitHub Workflow Fixes

This document contains all the necessary fixes for GitHub workflow YAML files to address syntax errors and update deprecated actions.

## Common Issues Found

1. Multiline string syntax errors (incorrect use of backticks without proper line handling)
2. Outdated GitHub Actions (v3 instead of v4)
3. Deprecated `::set-output` commands (should use `$GITHUB_OUTPUT`)
4. Unescaped special characters in strings

## Required Updates Per File

### 1. ai-orchestrator.yml
```yaml
# Replace all instances of:
uses: actions/checkout@v3
# With:
uses: actions/checkout@v4

# Replace all instances of:
uses: actions/setup-java@v3
# With:
uses: actions/setup-java@v4

# Replace all multiline strings with array.join('\n') format, for example:
const aiResponse = `Hello @${issue.data.user.login},

Thank you for your issue...`
# Should become:
const aiResponse = [
  `Hello @${issue.data.user.login},`,
  '',
  'Thank you for your issue...',
  ''
].join('\n');

# Replace all deprecated set-output commands:
echo "::set-output name=build_failed::true"
# With:
echo "build_failed=true" >> $GITHUB_OUTPUT
```

### 2. issue-triage.yml
```yaml
# Replace:
uses: actions/checkout@v3
# With:
uses: actions/checkout@v4

# Fix multiline strings in JavaScript:
const triageReply = `Hello @${issue.data.user.login}...`
# Should become:
const triageReply = [
  `Hello @${issue.data.user.login}! 👋`,
  '',
  `Thank you for your ${type}. I'm the AI assistant for this project, and I've automatically triaged this issue.`,
  '',
  `**Type:** ${type}`,
  `**Priority:** ${priority}`,
  `**Component:** ${component}`,
  '',
  `I'll analyze your issue in detail and respond shortly with more information. If you have any additional details to share in the meantime, please add them as a comment.`
].join('\n');
```

### 3. ai-implementation.yml
```yaml
# Replace:
uses: actions/checkout@v3
uses: actions/setup-java@v3
# With:
uses: actions/checkout@v4
uses: actions/setup-java@v4

# Fix multi-line string in status update (line 125-134):
body: `I'm now working on ${implementationType === 'fix' ? 'fixing' : ...} this issue.

Steps:
1. ✅ Analyzing the issue
2. 🔄 Designing a solution
...`
# Should become:
body: [
  `I'm now working on ${implementationType === 'fix' ? 'fixing' : implementationType === 'feature' ? 'implementing' : implementationType === 'refactor' ? 'refactoring' : 'improving'} this issue.`,
  '',
  'Steps:',
  '1. ✅ Analyzing the issue',
  '2. 🔄 Designing a solution',
  '3. ⏳ Implementing the code changes',
  '4. ⏳ Testing the changes',
  '5. ⏳ Creating a pull request',
  '',
  'I\'ll keep you updated on my progress.'
].join('\n'),

# Fix multi-line string in PR body (line 366-383):
const prBody = `This pull request implements...`
# Should become:
const prBody = [
  `This pull request implements ${implementationType === 'fix' ? 'a fix for' : implementationType === 'feature' ? 'the feature requested in' : implementationType === 'refactor' ? 'the refactoring requested in' : 'the improvements requested in'} issue #${issueNumber}.`,
  '',
  '## Implementation Details',
  `${implementationNotes}`,
  '',
  '## Build Status',
  `${buildMessage}`,
  '',
  '## Testing',
  `${buildSuccess ? '✅ All tests pass' : '⚠️ Some tests are failing and need attention'}`,
  '',
  '## Next Steps',
  '1. Please review this implementation',
  '2. Suggest any changes needed',
  '3. Approve and merge when ready',
  '',
  `Closes #${issueNumber}`
].join('\n');

# Fix multi-line string in issue comment (line 410-419):
body: `I've created a pull request with my implementation: #${pr.data.number}...`
# Should become:
body: [
  `I've created a pull request with my implementation: #${pr.data.number}`,
  '',
  'Steps completed:',
  '1. ✅ Analyzed the issue',
  '2. ✅ Designed a solution',
  '3. ✅ Implemented the code changes',
  '4. ✅ Tested the changes',
  '5. ✅ Created a pull request',
  '',
  'Please review the pull request and let me know if you need any adjustments or have questions about the implementation.'
].join('\n')
```

### 4. ai-issue-processor.yml
```yaml
# Replace:
uses: actions/checkout@v3
# With:
uses: actions/checkout@v4

# Fix multiline string in PR body:
body: `This PR contains AI-suggested fixes for issue #${issueNumber}.\n\nPlease review the changes and merge if appropriate.`,
# Should become:
body: [
  `This PR contains AI-suggested fixes for issue #${issueNumber}.`,
  '',
  'Please review the changes and merge if appropriate.'
].join('\n'),
```

### 5. code-analysis.yml
```yaml
# Replace:
uses: actions/checkout@v3
uses: actions/setup-java@v3
# With:
uses: actions/checkout@v4
uses: actions/setup-java@v4

# Fix multiline string:
const issueBody = `
## Build Failures Detected
...
`;
# Should become:
const issueBody = [
  '## Build Failures Detected',
  '',
  'The automated code analysis has detected the following issues:',
  '',
  `${errors.join('\n')}`,
  '',
  'Please review these issues. The AI assistant will attempt to fix these automatically.'
].join('\n');
```

### 6. auto-fix.yml
```yaml
# Replace:
uses: actions/checkout@v3
uses: actions/setup-java@v3
# With:
uses: actions/checkout@v4
uses: actions/setup-java@v4

# Fix multiline strings:
fs.writeFileSync('ai-fix-placeholder.md', 
  `# AI-generated fix for issue #${issueData.number}\n\n` +
  `This is a placeholder for actual code fixes that would be generated\n` +
  `by the AI based on the issue analysis.`);
# Should become:
fs.writeFileSync('ai-fix-placeholder.md', [
  `# AI-generated fix for issue #${issueData.number}`,
  '',
  'This is a placeholder for actual code fixes that would be generated',
  'by the AI based on the issue analysis.'
].join('\n'));

# And:
body: `This PR contains automated fixes for issue #${issueData.number}.\n\n` +
      `Original issue: ${issueData.title}\n\n` +
      `Please review the changes and merge if appropriate.`,
# Should become:
body: [
  `This PR contains automated fixes for issue #${issueData.number}.`,
  '',
  `Original issue: ${issueData.title}`,
  '',
  'Please review the changes and merge if appropriate.'
].join('\n'),
```

### 7. ai-development.yml
```yaml
# Replace:
uses: actions/checkout@v3
# With:
uses: actions/checkout@v4
```

### 8. comment-command-handler.yml
No major YAML syntax issues detected, but for consistency:
```yaml
# Add formatted response for multiline strings if any are added in the future
# Example:
body: [
  `I've received your command \`@ai ${task.replace('-', ' ')}\`. Processing now...`,
  '',
  'I\'ll post the results here when complete.'
].join('\n')
```

## Implementation Instructions

1. For each file mentioned above, create an updated version with "-updated.yml" suffix
2. Use the `str_replace_editor` tool to implement the changes
3. After thorough verification, you can replace the original files with the updated versions

## Notes on YAML Syntax

- Use single quotes ('') for strings containing special characters but no variables
- Use double quotes ("") for strings containing variables but need escaping
- Use array-join pattern for multiline strings in JavaScript code blocks
- Follow GitHub's recommended syntax for workflow commands (GITHUB_OUTPUT, etc.)