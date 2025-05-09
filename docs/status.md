---
layout: default
title: AI Workflow Status
---

# AI Workflow Status

This page shows the status of recent AI workflows and activities in the SevenToDie plugin project.

<div class="ai-dashboard">
  <h3>Recent Workflow Runs</h3>
  <div id="workflowRuns">
    <p>Please authenticate to view workflow status.</p>
  </div>
</div>

<div class="ai-dashboard">
  <h3>Recent Pull Requests</h3>
  <div id="pullRequests">
    <p>Please authenticate to view pull requests.</p>
  </div>
</div>

<div class="ai-dashboard">
  <h3>Project Health</h3>
  <div id="projectHealth">
    <p>Please authenticate to view project health metrics.</p>
  </div>
</div>

<script>
// Additional JavaScript for Status Page
document.addEventListener('DOMContentLoaded', function() {
  // Check if authenticated
  if (github.hasToken()) {
    loadStatusData();
  }
  
  // Load status data
  async function loadStatusData() {
    try {
      // Update workflow runs
      const aiWorkflowId = document.getElementById('aiWorkflowId') ? 
        document.getElementById('aiWorkflowId').value : '';
      
      if (aiWorkflowId) {
        const runs = await github.getWorkflowRuns(aiWorkflowId);
        updateWorkflowRunsUI(runs);
      }
      
      // Get pull requests
      const pullRequests = await getPullRequests();
      updatePullRequestsUI(pullRequests);
      
      // Update project health
      updateProjectHealthUI();
    } catch (error) {
      console.error('Error loading status data:', error);
    }
  }
  
  // Get repository pull requests
  async function getPullRequests() {
    try {
      const response = await fetch(`https://api.github.com/repos/${github.owner}/${github.repo}/pulls`, {
        headers: {
          'Authorization': `token ${github.token}`
        }
      });
      
      if (response.ok) {
        return await response.json();
      }
      return [];
    } catch (error) {
      console.error('Error fetching pull requests:', error);
      return [];
    }
  }
  
  // Update UI for workflow runs
  function updateWorkflowRunsUI(runs) {
    const container = document.getElementById('workflowRuns');
    container.innerHTML = '';
    
    if (!runs || runs.length === 0) {
      container.innerHTML = '<p>No recent workflow runs found.</p>';
      return;
    }
    
    runs.slice(0, 10).forEach(run => {
      const date = new Date(run.created_at);
      const statusClass = run.status === 'completed' 
        ? (run.conclusion === 'success' ? 'status-success' : 'status-failed')
        : 'status-pending';
      
      const runCard = document.createElement('div');
      runCard.className = 'feature-box';
      runCard.innerHTML = `
        <h4>${run.display_title || 'AI Workflow Run'}</h4>
        <p>Started: ${date.toLocaleString()}</p>
        <p>Status: <span class="status-badge ${statusClass}">${run.status} ${run.conclusion || ''}</span></p>
        <a href="${run.html_url}" target="_blank" class="dashboard-button">View Details</a>
      `;
      
      container.appendChild(runCard);
    });
  }
  
  // Update UI for pull requests
  function updatePullRequestsUI(pullRequests) {
    const container = document.getElementById('pullRequests');
    container.innerHTML = '';
    
    if (!pullRequests || pullRequests.length === 0) {
      container.innerHTML = '<p>No open pull requests found.</p>';
      return;
    }
    
    // Filter to show AI-created PRs first
    const aiPRs = pullRequests.filter(pr => pr.user.login.includes('[bot]'));
    const otherPRs = pullRequests.filter(pr => !pr.user.login.includes('[bot]'));
    
    // Sort by newest first
    const sortedPRs = [...aiPRs, ...otherPRs].sort((a, b) => 
      new Date(b.created_at) - new Date(a.created_at)
    );
    
    sortedPRs.slice(0, 5).forEach(pr => {
      const date = new Date(pr.created_at);
      const isAI = pr.user.login.includes('[bot]');
      
      const prCard = document.createElement('div');
      prCard.className = 'feature-box';
      prCard.innerHTML = `
        <h4>${pr.title}</h4>
        <p>Created: ${date.toLocaleString()}</p>
        <p>Author: ${pr.user.login} ${isAI ? '(AI)' : ''}</p>
        <a href="${pr.html_url}" target="_blank" class="dashboard-button">View Pull Request</a>
      `;
      
      container.appendChild(prCard);
    });
  }
  
  // Update project health metrics
  async function updateProjectHealthUI() {
    const container = document.getElementById('projectHealth');
    container.innerHTML = '';
    
    try {
      // Get issues count
      const issuesResponse = await fetch(`https://api.github.com/repos/${github.owner}/${github.repo}/issues?state=all`, {
        headers: {
          'Authorization': `token ${github.token}`
        }
      });
      
      // Get commits count
      const commitsResponse = await fetch(`https://api.github.com/repos/${github.owner}/${github.repo}/commits`, {
        headers: {
          'Authorization': `token ${github.token}`
        }
      });
      
      if (issuesResponse.ok && commitsResponse.ok) {
        const issues = await issuesResponse.json();
        const commits = await commitsResponse.json();
        
        const openIssues = issues.filter(issue => issue.state === 'open').length;
        const closedIssues = issues.filter(issue => issue.state === 'closed').length;
        
        // Simple health score calculation
        const healthScore = Math.min(100, Math.max(0, 
          100 - (openIssues > 0 ? (openIssues / (openIssues + closedIssues) * 50) : 0)
        ));
        
        let healthClass = 'status-success';
        if (healthScore < 70) healthClass = 'status-pending';
        if (healthScore < 40) healthClass = 'status-failed';
        
        const healthCard = document.createElement('div');
        healthCard.className = 'feature-box';
        healthCard.innerHTML = `
          <h4>Project Metrics</h4>
          <p>Health Score: <span class="status-badge ${healthClass}">${Math.round(healthScore)}%</span></p>
          <p>Open Issues: ${openIssues}</p>
          <p>Closed Issues: ${closedIssues}</p>
          <p>Recent Commits: ${commits.length}</p>
          <p>Last Activity: ${new Date(commits[0]?.commit?.author?.date || Date.now()).toLocaleString()}</p>
        `;
        
        container.appendChild(healthCard);
      } else {
        container.innerHTML = '<p>Error loading project health data.</p>';
      }
    } catch (error) {
      console.error('Error calculating project health:', error);
      container.innerHTML = '<p>Error loading project health data.</p>';
    }
  }
});
</script>