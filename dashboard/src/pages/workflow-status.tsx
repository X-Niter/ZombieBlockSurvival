import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'

export default function WorkflowStatus() {
  return (
    <MainLayout>
      <Head><title>Workflow Status | ZBS</title></Head>
      <section className="py-10 px-4 max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-cyan-400 mb-4">⚙️ GitHub Workflow Status</h1>
        <p className="text-zinc-300">Live view of recent GitHub Actions related to plugin builds, tests, and releases.</p>
        <div className="mt-6 p-4 border border-zinc-700 bg-zinc-900 rounded-xl text-zinc-400">
          <p>✅ Workflows are up-to-date. All systems operational.</p>
          {/* This could be upgraded to fetch & render real-time status from GitHub's API */}
        </div>
      </section>
    </MainLayout>
  )
}