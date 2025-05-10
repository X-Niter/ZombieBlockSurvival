
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'
import StatusPanel from '@/components/StatusPanel'
import PromptInput from '@/components/PromptInput'
import ChangelogViewer from '@/components/ChangelogViewer'
import WorkflowEngine from '@/components/WorkflowEngine'

export default function Home() {
  return (
    <MainLayout>
      <Head>
        <title>ZombieBlockSurvival Dashboard</title>
      </Head>
      <div className="p-6 space-y-6">
        <h1 className="text-3xl font-bold text-red-600">Welcome, Survivor</h1>
        <p className="text-gray-400">Your post-apocalyptic AI command center.</p>
        <StatusPanel />
        <PromptInput />
        <WorkflowEngine />
        <ChangelogViewer />
      </div>
    </MainLayout>
  )
}
