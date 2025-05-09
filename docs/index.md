---
layout: default
title: SevenToDie Plugin Dashboard
---

# SevenToDie AI Development Dashboard

Welcome to the AI-powered development dashboard for the SevenToDie Minecraft plugin, which recreates 7 Days To Die gameplay mechanics on Paper 1.21.4.

This interactive dashboard allows you to:
- Trigger AI workflows to analyze code, fix bugs, and implement features
- Create issues that will be automatically analyzed by AI
- Track the status of AI-driven development tasks
- Monitor the project's overall health

## Setup

<div id="setupPanel" class="ai-dashboard">
  <h3>Authentication Setup</h3>
  <p>To use this dashboard, you need to provide a GitHub personal access token with 'repo' and 'workflow' permissions.</p>
  
  <form id="tokenForm" class="form-group">
    <label for="githubToken">GitHub Personal Access Token:</label>
    <input type="password" id="githubToken" placeholder="ghp_..." required>
    <p><small>This token is stored in your browser session only and is never sent to our servers.</small></p>
    <button type="submit" class="dashboard-button">Authenticate</button>
  </form>
  
  <div class="feature-box">
    <h4>How to Create a Token:</h4>
    <ol>
      <li>Go to <a href="https://github.com/settings/tokens" target="_blank">GitHub Token Settings</a></li>
      <li>Click "Generate new token" → "Generate new token (classic)"</li>
      <li>Give it a name like "SevenToDie Dashboard"</li>
      <li>Select at least these scopes: <code>repo</code>, <code>workflow</code></li>
      <li>Click "Generate token" and copy the token value</li>
    </ol>
  </div>
</div>

<div id="dashboardPanel" style="display: none;">
  <div class="ai-dashboard">
    <h3>Repository Information</h3>
    <p>Currently connected to: <span id="repoInfo">Not connected</span></p>
    <p id="userInfo">Not authenticated</p>
    
    <input type="hidden" id="aiWorkflowId" value="">
  </div>
  
  <div class="ai-dashboard">
    <h3>Trigger AI Development Task</h3>
    <form id="aiTaskForm">
      <div class="form-group">
        <label for="taskType">Task Type:</label>
        <select id="taskType" required>
          <option value="analyze-code">Analyze Code</option>
          <option value="fix-bugs">Fix Bugs</option>
          <option value="implement-feature">Implement Feature</option>
          <option value="improve-performance">Improve Performance</option>
        </select>
      </div>
      
      <div class="form-group">
        <label for="taskDescription">Description:</label>
        <textarea id="taskDescription" placeholder="Describe what you want the AI to do..." required></textarea>
      </div>
      
      <button type="submit" class="dashboard-button">Run Task</button>
      <p id="taskStatus"></p>
    </form>
  </div>
  
  <div class="ai-dashboard">
    <h3>Create Issue for AI Analysis</h3>
    <form id="issueForm">
      <div class="form-group">
        <label for="issueType">Issue Type:</label>
        <select id="issueType" required>
          <option value="bug">Bug Report</option>
          <option value="feature">Feature Request</option>
          <option value="other">Other</option>
        </select>
      </div>
      
      <div class="form-group">
        <label for="issueTitle">Title:</label>
        <input type="text" id="issueTitle" placeholder="Brief, descriptive title" required>
      </div>
      
      <div class="form-group">
        <label for="issueBody">Description:</label>
        <textarea id="issueBody" placeholder="Detailed description of the issue or feature..." required></textarea>
      </div>
      
      <button type="submit" class="dashboard-button">Create Issue</button>
      <p id="issueStatus"></p>
    </form>
  </div>
  
  <div class="ai-dashboard">
    <h3>Recent AI Tasks</h3>
    <div id="recentTasks">
      <p>Loading recent tasks...</p>
    </div>
  </div>
  
  <div class="ai-dashboard">
    <h3>Recent Issues</h3>
    <div id="recentIssues">
      <p>Loading recent issues...</p>
    </div>
  </div>
</div>

<script src="{{ '/assets/js/dashboard.js' | relative_url }}"></script>