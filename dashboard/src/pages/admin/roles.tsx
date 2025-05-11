
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'
import { useEffect, useState } from 'react'

export default function RoleManager() {
  const [users, setUsers] = useState({})
  const [refresh, setRefresh] = useState(false)

  const load = async () => {
    const res = await fetch('/api/admin/get-roles')
    const data = await res.json()
    setUsers(data)
  }

  const assign = async (username: string, role: string) => {
    await fetch('/api/admin/set-role', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, role })
    })
    setRefresh(!refresh)
  }

  useEffect(() => { load() }, [refresh])

  const pendingUsers = Object.entries(users).filter(([_, role]) => role === 'pending')

  return (
    <MainLayout>
      <Head><title>User Roles | Admin</title></Head>
      <div className="max-w-2xl mx-auto py-10">
        <h1 className="text-3xl font-bold text-yellow-400 mb-4">👥 Role Management</h1>

        <div className="bg-zinc-900 p-6 rounded-xl border border-zinc-700 space-y-4">
          <h2 className="text-lg font-bold text-zinc-200 mb-2">Pending Approvals</h2>
          {pendingUsers.length === 0 && <p className="text-zinc-500 text-sm">No users pending approval.</p>}
          <ul className="space-y-3">
            {pendingUsers.map(([user]) => (
              <li key={user} className="flex items-center justify-between">
                <span className="text-white">{user}</span>
                <div className="space-x-2">
                  <button onClick={() => assign(user, 'dev')} className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-sm">Approve as Dev</button>
                  <button onClick={() => assign(user, 'viewer')} className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded text-sm">Approve as Viewer</button>
                  <button onClick={() => assign(user, 'admin')} className="bg-purple-600 hover:bg-purple-700 text-white px-3 py-1 rounded text-sm">Promote to Admin</button>
                </div>
              </li>
            ))}
          </ul>
        </div>

        <div className="mt-8">
          <h2 className="text-lg font-bold text-zinc-200 mb-2">All Users</h2>
          <ul className="text-sm text-zinc-400 space-y-1">
            {Object.entries(users).map(([u, role]) => (
              <li key={u}><b className="text-white">{u}</b>: {role as string}</li>
            ))}
          </ul>
        </div>
      </div>
    </MainLayout>
  )
}
