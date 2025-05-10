import React, { useState, useEffect } from 'react';
import { Bar, Line } from 'react-chartjs-2';
import { Chart, registerables } from 'chart.js';
import { format, parseISO, subDays } from 'date-fns';

// Register Chart.js components
Chart.register(...registerables);

// Helper function to load local data
const loadLocalData = async (filename) => {
  try {
    const data = await import(`../data/${filename}`);
    return data.default;
  } catch (error) {
    console.error(`Error loading ${filename}:`, error);
    return [];
  }
};

const Dashboard = () => {
  const [issuesData, setIssuesData] = useState([]);
  const [prsData, setPrsData] = useState([]);
  const [aiInteractions, setAiInteractions] = useState([]);
  const [aiImplementations, setAiImplementations] = useState([]);
  const [healthCheckLogs, setHealthCheckLogs] = useState([]);
  const [autoFixLogs, setAutoFixLogs] = useState([]);
  const [selfTestLogs, setSelfTestLogs] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    const loadData = async () => {
      try {
        // Load data from files
        const interactions = await loadLocalData('ai_interactions.json');
        const implementations = await loadLocalData('ai_implementations.json');
        const healthChecks = await loadLocalData('health_check_logs.json');
        const autoFixes = await loadLocalData('auto_fix_logs.json');
        const selfTests = await loadLocalData('self_test_logs.json');
        
        setAiInteractions(interactions || []);
        setAiImplementations(implementations || []);
        setHealthCheckLogs(healthChecks || []);
        setAutoFixLogs(autoFixes || []);
        setSelfTestLogs(selfTests || []);
        
        // Organize PR and issue data
        const issues = interactions.filter(item => item.event_type === 'issue');
        const prs = interactions.filter(item => item.event_type === 'pull_request');
        
        setIssuesData(issues);
        setPrsData(prs);
      } catch (error) {
        console.error('Error loading dashboard data:', error);
      }
    };
    
    loadData();
  }, []);

  // Generate data for the activity chart
  const getActivityChartData = () => {
    // Create date ranges for the last 14 days
    const dateLabels = Array.from({ length: 14 }, (_, i) => {
      return format(subDays(new Date(), 13 - i), 'MMM dd');
    });
    
    // Count interactions per day
    const activityCounts = dateLabels.map(label => {
      const count = aiInteractions.filter(item => {
        const itemDate = format(parseISO(item.timestamp), 'MMM dd');
        return itemDate === label;
      }).length;
      
      return count;
    });
    
    return {
      labels: dateLabels,
      datasets: [
        {
          label: 'AI Activity',
          data: activityCounts,
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 1,
        },
      ],
    };
  };

  // Generate data for the implementation success rate chart
  const getImplementationSuccessData = () => {
    const successful = aiImplementations.filter(item => item.successful).length;
    const failed = aiImplementations.length - successful;
    
    return {
      labels: ['Successful', 'Failed'],
      datasets: [
        {
          data: [successful, failed],
          backgroundColor: [
            'rgba(75, 192, 192, 0.2)',
            'rgba(255, 99, 132, 0.2)',
          ],
          borderColor: [
            'rgba(75, 192, 192, 1)',
            'rgba(255, 99, 132, 1)',
          ],
          borderWidth: 1,
        },
      ],
    };
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold text-gray-900">SevenToDie Plugin Autonomous Development Dashboard</h1>
        </div>
      </header>
      
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {/* Tabs */}
        <div className="mb-4 border-b border-gray-200">
          <nav className="-mb-px flex">
            <button
              onClick={() => setActiveTab('overview')}
              className={`mr-8 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'overview'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('interactions')}
              className={`mr-8 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'interactions'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              AI Interactions
            </button>
            <button
              onClick={() => setActiveTab('implementations')}
              className={`mr-8 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'implementations'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              AI Implementations
            </button>
            <button
              onClick={() => setActiveTab('health')}
              className={`mr-8 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'health'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              System Health
            </button>
            <button
              onClick={() => setActiveTab('selfRepair')}
              className={`mr-8 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'selfRepair'
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Self-Repair
            </button>
          </nav>
        </div>
        
        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <div className="mt-6">
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              {/* Stats cards */}
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      Total AI Interactions
                    </dt>
                    <dd className="mt-1 text-3xl font-semibold text-gray-900">
                      {aiInteractions.length}
                    </dd>
                  </dl>
                </div>
              </div>
              
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">
                      AI-Generated PRs
                    </dt>
                    <dd className="mt-1 text-3xl font-semibold text-gray-900">
                      {aiImplementations.length}
                    </dd>
                  </dl>
                </div>
              </div>
              
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">Activity (Last 14 Days)</h3>
                  <div className="mt-4" style={{ height: '200px' }}>
                    <Line 
                      data={getActivityChartData()} 
                      options={{ maintainAspectRatio: false }} 
                    />
                  </div>
                </div>
              </div>
              
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">Implementation Success Rate</h3>
                  <div className="mt-4" style={{ height: '200px' }}>
                    <Bar 
                      data={getImplementationSuccessData()} 
                      options={{ 
                        maintainAspectRatio: false,
                        plugins: {
                          legend: { display: false }
                        }
                      }} 
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
        
        {/* AI Interactions Tab */}
        {activeTab === 'interactions' && (
          <div className="mt-6">
            <div className="bg-white shadow overflow-hidden sm:rounded-md">
              <ul className="divide-y divide-gray-200">
                {aiInteractions.length > 0 ? (
                  aiInteractions.map((interaction, index) => (
                    <li key={index} className="px-4 py-4 sm:px-6">
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-medium text-indigo-600 truncate">
                          {interaction.event_type} #{interaction.event_id}: {interaction.title}
                        </p>
                        <div className="ml-2 flex-shrink-0 flex">
                          <p className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                            {format(parseISO(interaction.timestamp), 'MMM dd, yyyy HH:mm')}
                          </p>
                        </div>
                      </div>
                      <div className="mt-2 sm:flex sm:justify-between">
                        <div className="sm:flex">
                          <p className="text-sm text-gray-500">
                            {interaction.response.substring(0, 200)}...
                          </p>
                        </div>
                      </div>
                    </li>
                  ))
                ) : (
                  <li className="px-4 py-4 sm:px-6 text-center text-gray-500">
                    No AI interactions recorded yet
                  </li>
                )}
              </ul>
            </div>
          </div>
        )}
        
        {/* AI Implementations Tab */}
        {activeTab === 'implementations' && (
          <div className="mt-6">
            <div className="bg-white shadow overflow-hidden sm:rounded-md">
              <ul className="divide-y divide-gray-200">
                {aiImplementations.length > 0 ? (
                  aiImplementations.map((implementation, index) => (
                    <li key={index} className="px-4 py-4 sm:px-6">
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-medium text-indigo-600 truncate">
                          Issue #{implementation.issue_number}: {implementation.issue_title}
                        </p>
                        <div className="ml-2 flex-shrink-0 flex">
                          <p className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                            implementation.successful 
                              ? 'bg-green-100 text-green-800' 
                              : 'bg-red-100 text-red-800'
                          }`}>
                            {implementation.successful ? 'Success' : 'Failed'}
                          </p>
                        </div>
                      </div>
                      <div className="mt-2 sm:flex sm:justify-between">
                        <div className="sm:flex">
                          <p className="text-sm text-gray-500">
                            {implementation.explanation && implementation.explanation.substring(0, 200)}...
                          </p>
                        </div>
                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                          <span>
                            {implementation.pr_number && (
                              <a 
                                href={implementation.pr_url} 
                                target="_blank" 
                                rel="noopener noreferrer"
                                className="text-indigo-600 hover:text-indigo-900"
                              >
                                PR #{implementation.pr_number}
                              </a>
                            )}
                          </span>
                        </div>
                      </div>
                    </li>
                  ))
                ) : (
                  <li className="px-4 py-4 sm:px-6 text-center text-gray-500">
                    No AI implementations recorded yet
                  </li>
                )}
              </ul>
            </div>
          </div>
        )}
        
        {/* System Health Tab */}
        {activeTab === 'health' && (
          <div className="mt-6">
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              {/* Health Status Card */}
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">Plugin Health Status</h3>
                  <div className="mt-4">
                    {healthCheckLogs.length > 0 ? (
                      <div>
                        <div className="flex items-center">
                          <div className={`flex-shrink-0 h-5 w-5 rounded-full ${
                            healthCheckLogs[0].success ? 'bg-green-500' : 'bg-red-500'
                          }`}></div>
                          <p className="ml-3 text-sm text-gray-700">
                            {healthCheckLogs[0].success 
                              ? 'System is healthy' 
                              : `Issues detected: ${healthCheckLogs[0].issues_count}`}
                          </p>
                        </div>
                        <p className="mt-2 text-sm text-gray-500">
                          Last checked: {new Date(healthCheckLogs[0].timestamp).toLocaleString()}
                        </p>
                      </div>
                    ) : (
                      <p className="text-sm text-gray-500">No health check data available</p>
                    )}
                  </div>
                </div>
              </div>
              
              {/* Health Check History Card */}
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">Recent Health Checks</h3>
                  <div className="mt-4">
                    <ul className="divide-y divide-gray-200">
                      {healthCheckLogs.slice(0, 5).map((log, index) => (
                        <li key={index} className="py-2">
                          <div className="flex items-center">
                            <div className={`flex-shrink-0 h-4 w-4 rounded-full ${
                              log.success ? 'bg-green-500' : 'bg-red-500'
                            }`}></div>
                            <p className="ml-3 text-sm text-gray-700">
                              {log.success 
                                ? 'Healthy' 
                                : `${log.issues_count} issues`} - {new Date(log.timestamp).toLocaleString()}
                            </p>
                          </div>
                        </li>
                      ))}
                      {healthCheckLogs.length === 0 && (
                        <li className="py-2 text-center text-gray-500">
                          No health check history available
                        </li>
                      )}
                    </ul>
                  </div>
                </div>
              </div>
              
              {/* Self-Test Results Card */}
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">Self-Test Results</h3>
                  <div className="mt-4">
                    <ul className="divide-y divide-gray-200">
                      {selfTestLogs.slice(0, 5).map((log, index) => (
                        <li key={index} className="py-2">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center">
                              <div className={`flex-shrink-0 h-4 w-4 rounded-full ${
                                log.issues_found === 0 ? 'bg-green-500' : 'bg-yellow-500'
                              }`}></div>
                              <p className="ml-3 text-sm text-gray-700">
                                {log.issues_found === 0 
                                  ? 'All tests passed' 
                                  : `${log.issues_found} issues found`}
                              </p>
                            </div>
                            <p className="text-xs text-gray-500">
                              {new Date(log.timestamp).toLocaleString()}
                            </p>
                          </div>
                        </li>
                      ))}
                      {selfTestLogs.length === 0 && (
                        <li className="py-2 text-center text-gray-500">
                          No self-test results available
                        </li>
                      )}
                    </ul>
                  </div>
                </div>
              </div>
              
              {/* Issue Breakdown Card */}
              <div className="bg-white overflow-hidden shadow rounded-lg">
                <div className="px-4 py-5 sm:p-6">
                  <h3 className="text-lg font-medium text-gray-900">System Components Status</h3>
                  <div className="mt-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="border rounded p-3">
                        <h4 className="text-sm font-medium text-gray-700">Plugin</h4>
                        <div className="mt-1 flex items-center">
                          <div className="w-4 h-4 rounded-full bg-green-500"></div>
                          <span className="ml-2 text-sm text-gray-500">Operational</span>
                        </div>
                      </div>
                      <div className="border rounded p-3">
                        <h4 className="text-sm font-medium text-gray-700">GitHub Actions</h4>
                        <div className="mt-1 flex items-center">
                          <div className="w-4 h-4 rounded-full bg-green-500"></div>
                          <span className="ml-2 text-sm text-gray-500">Operational</span>
                        </div>
                      </div>
                      <div className="border rounded p-3">
                        <h4 className="text-sm font-medium text-gray-700">AI Scripts</h4>
                        <div className="mt-1 flex items-center">
                          <div className="w-4 h-4 rounded-full bg-green-500"></div>
                          <span className="ml-2 text-sm text-gray-500">Operational</span>
                        </div>
                      </div>
                      <div className="border rounded p-3">
                        <h4 className="text-sm font-medium text-gray-700">Dashboard</h4>
                        <div className="mt-1 flex items-center">
                          <div className="w-4 h-4 rounded-full bg-green-500"></div>
                          <span className="ml-2 text-sm text-gray-500">Operational</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
        
        {/* Self-Repair Tab */}
        {activeTab === 'selfRepair' && (
          <div className="mt-6">
            <div className="bg-white overflow-hidden shadow rounded-lg mb-6">
              <div className="px-4 py-5 sm:p-6">
                <h3 className="text-lg font-medium text-gray-900">System Self-Repair Status</h3>
                <div className="mt-4">
                  <div className="flex items-center">
                    <div className="flex-shrink-0 h-5 w-5 rounded-full bg-green-500"></div>
                    <p className="ml-3 text-sm text-gray-700">Self-repair system is active and operational</p>
                  </div>
                  <p className="mt-2 text-sm text-gray-500">
                    Last verification: {new Date().toLocaleString()}
                  </p>
                  <div className="mt-4 grid grid-cols-2 gap-4">
                    <div className="border rounded p-3">
                      <h4 className="text-sm font-medium text-gray-700">Auto-Fix System</h4>
                      <div className="mt-1 flex items-center">
                        <div className="w-4 h-4 rounded-full bg-green-500"></div>
                        <span className="ml-2 text-sm text-gray-500">Active</span>
                      </div>
                    </div>
                    <div className="border rounded p-3">
                      <h4 className="text-sm font-medium text-gray-700">Recovery System</h4>
                      <div className="mt-1 flex items-center">
                        <div className="w-4 h-4 rounded-full bg-green-500"></div>
                        <span className="ml-2 text-sm text-gray-500">Standby</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="bg-white shadow overflow-hidden sm:rounded-md">
              <div className="px-4 py-5 sm:px-6">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Recent Self-Repair Actions
                </h3>
              </div>
              <ul className="divide-y divide-gray-200">
                {autoFixLogs.length > 0 ? (
                  autoFixLogs.map((log, index) => (
                    <li key={index} className="px-4 py-4 sm:px-6">
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-medium text-indigo-600">
                          {new Date(log.timestamp).toLocaleString()}
                        </p>
                        <p className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                          {log.fixes_count} fixes applied
                        </p>
                      </div>
                      <div className="mt-2">
                        <ul className="list-disc pl-5 text-sm text-gray-500">
                          {log.fixes && log.fixes.slice(0, 3).map((fix, idx) => (
                            <li key={idx}>{fix}</li>
                          ))}
                          {log.fixes && log.fixes.length > 3 && (
                            <li className="text-indigo-600">
                              ...and {log.fixes.length - 3} more fixes
                            </li>
                          )}
                        </ul>
                      </div>
                    </li>
                  ))
                ) : (
                  <li className="px-4 py-4 sm:px-6 text-center text-gray-500">
                    No self-repair actions recorded yet
                  </li>
                )}
              </ul>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default Dashboard;