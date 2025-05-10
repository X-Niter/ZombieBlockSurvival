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
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    const loadData = async () => {
      try {
        // Load data from files
        const interactions = await loadLocalData('ai_interactions.json');
        const implementations = await loadLocalData('ai_implementations.json');
        
        setAiInteractions(interactions || []);
        setAiImplementations(implementations || []);
        
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
      </main>
    </div>
  );
};

export default Dashboard;