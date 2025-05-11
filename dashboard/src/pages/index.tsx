import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'

export default function Home() {
  return (
    <MainLayout>
      <Head>
        <title>Dashboard | AI Command Center</title>
      </Head>
      <section className="py-10 px-4 max-w-6xl mx-auto">
        <div className="mb-10 text-center">
          <h1 className="text-4xl font-bold text-red-400">Welcome, Survivor</h1>
          <p className="text-zinc-400 mt-2">Your post-apocalyptic AI command center.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="rounded-xl bg-zinc-900 border border-zinc-700 p-4 shadow-md">
            <h2 className="text-pink-400 font-bold text-lg">AI Status</h2>
            <p className="text-green-400 mt-1">✅ Connected to OpenAI</p>
          </div>

          <div className="rounded-xl bg-zinc-900 border border-zinc-700 p-4 shadow-md">
            <h2 className="text-cyan-400 font-bold text-lg">GitHub Status</h2>
            <p className="text-green-400 mt-1">✅ GitHub Access Granted</p>
          </div>

          <div className="rounded-xl bg-zinc-900 border border-zinc-700 p-4 shadow-md">
            <h2 className="text-purple-400 font-bold text-lg">Custom Prompt</h2>
            <textarea className="w-full p-2 mt-2 rounded bg-zinc-800 text-zinc-200 resize-none" rows={3} placeholder="Ask AI to refactor, fix, or suggest..."></textarea>
            <button className="mt-3 px-4 py-2 rounded bg-purple-500 text-white hover:bg-purple-600">Send Prompt</button>
          </div>

          <div className="rounded-xl bg-zinc-900 border border-zinc-700 p-4 shadow-md md:col-span-2">
            <h2 className="text-cyan-300 font-bold text-lg">AI Workflow Engine</h2>
            <button className="mt-3 px-4 py-2 rounded bg-cyan-500 text-white hover:bg-cyan-600">Run Workflow</button>
          </div>

          <div className="rounded-xl bg-zinc-900 border border-zinc-700 p-4 shadow-md md:col-span-3">
            <h2 className="text-amber-400 font-bold text-lg">Changelogs</h2>
            <ul className="text-zinc-300 mt-2 list-disc list-inside text-sm space-y-1">
              <li><b>v1.0.1</b> - Added AI workflow runner, token handling, and refactored status panel</li>
              <li><b>v1.0.0</b> - Initial UI created, layout finalized</li>
            </ul>
            <p className="text-zinc-500 mt-4 text-xs text-right">© 2025 ZombieBlockSurvival. Built to outlast the apocalypse.</p>
          </div>
        </div>
      </section>
    </MainLayout>
  )
}