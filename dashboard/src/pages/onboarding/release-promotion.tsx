
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'
import { useState } from 'react'

export default function ReleasePromotionSetup() {
  const [enabled, setEnabled] = useState(false)
  const [saved, setSaved] = useState(false)

  const saveSetting = async () => {
    await fetch('/api/onboarding/store-release-setting', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ releaseApprovalUI: enabled })
    })
    setSaved(true)
  }

  return (
    <MainLayout>
      <Head>
        <title>Release Promotion Setup</title>
      </Head>
      <div className="max-w-3xl mx-auto py-10 space-y-6">
        <h1 className="text-3xl font-bold text-amber-400">📦 Stable Release Control</h1>
        <p className="text-zinc-400 text-sm">
          Do you want stable plugin releases to require manual approval via the website interface?
        </p>
        <label className="flex items-center space-x-3 mt-4">
          <input
            type="checkbox"
            checked={enabled}
            onChange={() => setEnabled(!enabled)}
            className="w-5 h-5"
          />
          <span className="text-white">Enable web-based release approval panel</span>
        </label>
        <button
          onClick={saveSetting}
          className="mt-4 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded shadow"
        >
          Save Setting
        </button>
        {saved && <p className="text-green-400 text-sm mt-2">✅ Setting saved. This feature will now be available if enabled.</p>}
      </div>
    </MainLayout>
  )
}
