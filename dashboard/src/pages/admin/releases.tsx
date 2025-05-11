
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'
import { useEffect, useState } from 'react'

export default function AdminReleases() {
  const [role, setRole] = useState(null)

  useEffect(() => {
    fetch('/api/admin/get-session')
      .then(res => res.json())
      .then(({ username }) => {
        if (!username) return
        fetch('/api/admin/get-roles')
          .then(res => res.json())
          .then(users => {
            const r = users[username] || null
            if (r === 'pending') {
              window.location.href = '/pending'
            } else {
              setRole(r)
            }
          })
      })
  }, [])

  if (!role) {
    return (
      <MainLayout>
        <div className="text-center py-20">
          <h1 className="text-xl text-red-400">🚫 Access Denied</h1>
          <p className="text-zinc-500 text-sm">Only logged-in users with roles can access this panel.</p>
        </div>
      </MainLayout>
    )
  }

  return (
    <MainLayout>
      <Head><title>Release Panel | Admin</title></Head>
      <div className="max-w-5xl mx-auto p-8">
        <h1 className="text-3xl font-bold text-yellow-400 mb-6">🚨 Admin Release Controls</h1>
        <div className="grid md:grid-cols-2 gap-6">
          <div className="p-6 bg-zinc-900 border border-zinc-700 rounded-xl">
            <h2 className="text-xl font-bold text-zinc-200 mb-2">Pending AI Fix Approval</h2>
            <p className="text-zinc-400 text-sm mb-4">Approve AI-generated pull requests to finalize releases.</p>
            {role === 'admin' && (
              <button className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded">✅ Approve Release</button>
            )}
          </div>
          {role === 'admin' && (
            <div className="p-6 bg-zinc-900 border border-zinc-700 rounded-xl">
              <h2 className="text-xl font-bold text-zinc-200 mb-2">User Access Management</h2>
              <p className="text-zinc-400 text-sm mb-4">Control which users may trigger AI merges or auto-promote builds.</p>
              <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">🔧 Manage Users</button>
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  )
}
