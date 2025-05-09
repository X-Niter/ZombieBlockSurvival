// SevenToDie Plugin Dashboard JavaScript

// GitHub API functions
const github = {
  // Base settings
  owner: '',
  repo: '',
  token: '', // Set via user input, not stored permanently

  // Initialize with repository details
  init: function(owner, repo) {
    this.owner = owner;
    this.repo = repo;
    
    // Extract from URL if not provided
    if (!this.owner || !this.repo) {
      const repoMatch = location.pathname.match(/\/([^\/]+)\/([^\/]+)/);
      if (repoMatch && repoMatch.length >= 3) {
        this.owner = repoMatch[1];
        this.repo = repoMatch[2];
      }
    }

    document.getElementById('repoInfo').textContent = `${this.owner}/${this.repo}`;
  },

  // Set personal access token
  setToken: function(token) {
    this.token = token;
    sessionStorage.setItem('github_token', token);
    return this.validateToken();
  },

  // Check if token exists in session
  hasToken: function() {
    const token = sessionStorage.getItem('github_token');
    if (token) {
      this.token = token;
      return true;
    }
    return false;
  },

  // Validate the token works
  validateToken: async function() {
    try {
      const response = await fetch('https://api.github.com/user', {
        headers: {
          'Authorization': `token ${this.token}`
        }
      });
      
      if (response.ok) {
        const user = await response.json();
        document.getElementById('userInfo').textContent = `Authenticated as: ${user.login}`;
        document.getElementById('setupPanel').style.display = 'none';
        document.getElementById('dashboardPanel').style.display = 'block';
        this.loadWorkflows();
        return true;
      } else {
        alert('Invalid token or insufficient permissions');
        return false;
      }
    } catch (error) {
      console.error('Token validation error:', error);
      alert('Error validating token. Please try again.');
      return false;
    }
  },

  // Get workflows
  getWorkflows: async function() {
    try {
      const response = await fetch(`https://api.github.com/repos/${this.owner}/${this.repo}/actions/workflows`, {
        headers: {
          'Authorization': `token ${this.token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        return data.workflows;
      } else {
        console.error('Failed to fetch workflows:', response.statusText);
        return [];
      }
    } catch (error) {
      console.error('Error fetching workflows:', error);
      return [];
    }
  },

  // Trigger a workflow
  runWorkflow: async function(workflowId, inputs) {
    try {
      const response = await fetch(`https://api.github.com/repos/${this.owner}/${this.repo}/actions/workflows/${workflowId}/dispatches`, {
        method: 'POST',
        headers: {
          'Authorization': `token ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ref: 'main', // or 'master' - adjust as needed
          inputs: inputs
        })
      });
      
      if (response.ok) {
        return true;
      } else {
        console.error('Failed to trigger workflow:', response.statusText);
        return false;
      }
    } catch (error) {
      console.error('Error triggering workflow:', error);
      return false;
    }
  },

  // Get recent workflow runs
  getWorkflowRuns: async function(workflowId) {
    try {
      const response = await fetch(`https://api.github.com/repos/${this.owner}/${this.repo}/actions/workflows/${workflowId}/runs`, {
        headers: {
          'Authorization': `token ${this.token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        return data.workflow_runs;
      } else {
        console.error('Failed to fetch workflow runs:', response.statusText);
        return [];
      }
    } catch (error) {
      console.error('Error fetching workflow runs:', error);
      return [];
    }
  },

  // Get issues
  getIssues: async function() {
    try {
      const response = await fetch(`https://api.github.com/repos/${this.owner}/${this.repo}/issues`, {
        headers: {
          'Authorization': `token ${this.token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        return data;
      } else {
        console.error('Failed to fetch issues:', response.statusText);
        return [];
      }
    } catch (error) {
      console.error('Error fetching issues:', error);
      return [];
    }
  },

  // Create new issue
  createIssue: async function(title, body, labels) {
    try {
      const response = await fetch(`https://api.github.com/repos/${this.owner}/${this.repo}/issues`, {
        method: 'POST',
        headers: {
          'Authorization': `token ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title: title,
          body: body,
          labels: labels
        })
      });
      
      if (response.ok) {
        const data = await response.json();
        return data;
      } else {
        console.error('Failed to create issue:', response.statusText);
        return null;
      }
    } catch (error) {
      console.error('Error creating issue:', error);
      return null;
    }
  }
};

// UI Management Functions
const dashboard = {
  // Initialize the dashboard
  init: function() {
    this.setupEventListeners();
    
    // Check for existing token
    if (github.hasToken()) {
      github.validateToken();
    }
  },
  
  // Set up all event listeners
  setupEventListeners: function() {
    // Token submission
    const tokenForm = document.getElementById('tokenForm');
    if (tokenForm) {
      tokenForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const token = document.getElementById('githubToken').value;
        github.setToken(token);
      });
    }
    
    // AI Task form
    const aiTaskForm = document.getElementById('aiTaskForm');
    if (aiTaskForm) {
      aiTaskForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const task = document.getElementById('taskType').value;
        const description = document.getElementById('taskDescription').value;
        
        const workflowId = document.getElementById('aiWorkflowId').value;
        if (!workflowId) {
          alert('AI workflow not found. Make sure your repository has the AI Development workflow configured.');
          return;
        }
        
        const success = await github.runWorkflow(workflowId, {
          task: task,
          description: description
        });
        
        if (success) {
          document.getElementById('taskStatus').textContent = 'Task submitted successfully!';
          document.getElementById('taskStatus').className = 'status-badge status-success';
          // Refresh status after a delay
          setTimeout(() => dashboard.updateTasksStatus(), 5000);
        } else {
          document.getElementById('taskStatus').textContent = 'Error submitting task';
          document.getElementById('taskStatus').className = 'status-badge status-failed';
        }
      });
    }
    
    // Issue creation form
    const issueForm = document.getElementById('issueForm');
    if (issueForm) {
      issueForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const title = document.getElementById('issueTitle').value;
        const body = document.getElementById('issueBody').value;
        const type = document.getElementById('issueType').value;
        
        let labels = ['ai-ready'];
        if (type === 'bug') {
          labels.push('bug');
        } else if (type === 'feature') {
          labels.push('enhancement');
        }
        
        const issue = await github.createIssue(title, body, labels);
        
        if (issue) {
          document.getElementById('issueStatus').textContent = 'Issue created successfully!';
          document.getElementById('issueStatus').className = 'status-badge status-success';
          
          // Provide link to the issue
          const issueLink = document.createElement('a');
          issueLink.href = issue.html_url;
          issueLink.textContent = `View issue #${issue.number}`;
          issueLink.target = '_blank';
          
          const statusDiv = document.getElementById('issueStatus').parentNode;
          statusDiv.appendChild(document.createElement('br'));
          statusDiv.appendChild(issueLink);
          
          // Clear form
          document.getElementById('issueTitle').value = '';
          document.getElementById('issueBody').value = '';
        } else {
          document.getElementById('issueStatus').textContent = 'Error creating issue';
          document.getElementById('issueStatus').className = 'status-badge status-failed';
        }
      });
    }
  },
  
  // Load workflows and populate the UI
  loadWorkflows: async function() {
    const workflows = await github.getWorkflows();
    
    // Find and store AI workflow ID
    const aiWorkflow = workflows.find(w => w.name === 'AI Development Assistant');
    if (aiWorkflow) {
      document.getElementById('aiWorkflowId').value = aiWorkflow.id;
    }
    
    // Update recent runs
    this.updateTasksStatus();
    
    // Update issues list
    this.updateIssuesList();
  },
  
  // Update tasks status display
  updateTasksStatus: async function() {
    const workflowId = document.getElementById('aiWorkflowId').value;
    if (!workflowId) return;
    
    const runs = await github.getWorkflowRuns(workflowId);
    const recentTasks = document.getElementById('recentTasks');
    recentTasks.innerHTML = '';
    
    if (runs.length === 0) {
      recentTasks.innerHTML = '<p>No recent AI tasks found.</p>';
      return;
    }
    
    // Display recent runs (up to 5)
    runs.slice(0, 5).forEach(run => {
      const taskCard = document.createElement('div');
      taskCard.className = 'task-card';
      
      let statusClass = 'status-pending';
      if (run.status === 'completed') {
        statusClass = run.conclusion === 'success' ? 'status-success' : 'status-failed';
      }
      
      const date = new Date(run.created_at);
      const dateString = date.toLocaleString();
      
      taskCard.innerHTML = `
        <h4>${run.display_title || 'AI Task'}</h4>
        <p>Created: ${dateString}</p>
        <p>Status: <span class="status-badge ${statusClass}">${run.status}</span></p>
        <a href="${run.html_url}" target="_blank" class="dashboard-button">View Details</a>
      `;
      
      recentTasks.appendChild(taskCard);
    });
  },
  
  // Update issues list
  updateIssuesList: async function() {
    const issues = await github.getIssues();
    const issuesList = document.getElementById('recentIssues');
    issuesList.innerHTML = '';
    
    if (issues.length === 0) {
      issuesList.innerHTML = '<p>No issues found.</p>';
      return;
    }
    
    // Display recent issues (up to 5)
    issues.slice(0, 5).forEach(issue => {
      const issueCard = document.createElement('div');
      issueCard.className = 'task-card';
      
      const date = new Date(issue.created_at);
      const dateString = date.toLocaleString();
      
      // Determine if AI has responded
      const aiResponded = issue.comments > 0; // This is an approximation
      
      issueCard.innerHTML = `
        <h4>${issue.title}</h4>
        <p>Created: ${dateString}</p>
        <p>AI Response: <span class="status-badge ${aiResponded ? 'status-success' : 'status-pending'}">${aiResponded ? 'Received' : 'Pending'}</span></p>
        <a href="${issue.html_url}" target="_blank" class="dashboard-button">View Issue</a>
      `;
      
      issuesList.appendChild(issueCard);
    });
  }
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
  // Get repository info from config or URL
  const repoOwner = '';  // Will be extracted or entered by user
  const repoName = '';   // Will be extracted or entered by user
  
  github.init(repoOwner, repoName);
  dashboard.init();
});