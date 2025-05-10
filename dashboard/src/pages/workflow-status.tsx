import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'

export default function WorkflowStatus() {
  const workflows = [
    { name: 'AI Issue Triage', status: '✅ Passed', lastRun: '5 mins ago' },
    { name: 'AI Dev Agent', status: '⚠️ Warning', lastRun: '2 hours ago' },
    { name: 'AI Self Test', status: '❌ Failed', lastRun: '10 mins ago' },
    { name: 'AI Integrity Guard', status: '✅ Passed', lastRun: '1 day ago' },
    { name: 'AI Release Engine', status: '✅ Passed', lastRun: '3 hours ago' },
  ]

  return (
    <MainLayout>
      <Head>
        <title>Workflow Status</title>
      </Head>
      <div className="p-6 space-y-6">
        <h1 className="text-2xl font-bold text-amber-500">🧠 Workflow Dashboard</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {workflows.map((wf, idx) => (
            <div key={idx} className="bg-zinc-800 p-4 rounded-xl shadow-lg border border-zinc-700">
              <h2 className="text-lg font-semibold">{wf.name}</h2>
              <p className="text-sm text-gray-300">{wf.status}</p>
              <p className="text-xs text-zinc-500">Last Run: {wf.lastRun}</p>
            </div>
          ))}
        </div>
      </div>
    </MainLayout>
  )
}