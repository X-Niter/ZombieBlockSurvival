
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'
import { useEffect, useState } from 'react'

export default function ReleaseManager() {
  const [enabled, setEnabled] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('/api/onboarding/get-release-setting')
      .then(res => res.json())
      .then(data => {
        setEnabled(data.releaseApprovalUI === true)
        setLoading(false)
      })
  }, [])

  if (loading) return <MainLayout><p className="text-zinc-300 p-10">Loading...</p></MainLayout>

  if (!enabled) {
    return (
      <MainLayout>
        <div className="text-center py-20">
          <h1 className="text-2xl text-zinc-400">🚫 Release approval system not enabled.</h1>
          <p className="text-zinc-500 text-sm mt-2">You can enable it during onboarding or in config.</p>
        </div>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <Head>
        <title>Release Approval Panel</title>
      </Head>
      <div className="space-y-6 py-10 px-4 max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-amber-400">📦 Approve Stable Release</h1>
        <p className="text-sm text-zinc-400">View and promote alpha/beta pre-releases to stable.</p>
        <div className="text-sm text-zinc-500 pt-4">
          (This panel is under construction and will list releases fetched from GitHub.)
        </div>
      </div>
    </MainLayout>
  )
}
