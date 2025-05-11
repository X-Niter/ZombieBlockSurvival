import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'

export default function Changelog() {
  return (
    <MainLayout>
      <Head><title>Changelog | ZBS</title></Head>
      <section className="py-10 px-4 max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-yellow-400 mb-4">🔄 Project Changelog</h1>
        <div className="space-y-4">
          <div className="bg-zinc-900 border border-zinc-700 p-4 rounded-xl shadow-sm">
            <h2 className="text-xl font-bold text-zinc-200">v1.0.1</h2>
            <ul className="list-disc list-inside text-zinc-400 mt-2 space-y-1">
              <li>Added AI workflow runner</li>
              <li>Improved token handling</li>
              <li>Refactored status panel</li>
            </ul>
          </div>
          <div className="bg-zinc-900 border border-zinc-700 p-4 rounded-xl shadow-sm">
            <h2 className="text-xl font-bold text-zinc-200">v1.0.0</h2>
            <ul className="list-disc list-inside text-zinc-400 mt-2 space-y-1">
              <li>Initial UI created</li>
              <li>Dashboard layout finalized</li>
            </ul>
          </div>
        </div>
      </section>
    </MainLayout>
  )
}