
import MainLayout from '@/layouts/MainLayout'
import { useEffect, useState } from 'react'
import Head from 'next/head'

export default function TalismanReport() {
  const [results, setResults] = useState([])

  useEffect(() => {
    const fetchData = async () => {
      const res = await fetch('/api/github/talisman-logs')
      const data = await res.json()
      setResults(data)
    }

    fetchData()
    const interval = setInterval(fetchData, 10000) // Refresh every 10s
    return () => clearInterval(interval)
  }, [])

  return (
    <MainLayout>
      <Head><title>Talisman Report | Security</title></Head>
      <div className="max-w-4xl mx-auto py-10">
        <h1 className="text-3xl font-bold text-red-400 mb-6">🧪 Talisman Scan Report</h1>
        <div className="space-y-4">
          {results.map((entry, i) => (
            <div key={i} className="bg-zinc-900 border border-zinc-700 rounded-lg p-4">
              <p><b className="text-white">Commit:</b> {entry.commit}</p>
              <p><b className="text-white">Author:</b> {entry.author}</p>
              <p><b className="text-white">Date:</b> {entry.date}</p>
              <p><b className="text-white">Result:</b> <span className={entry.result.includes('⚠️') ? 'text-yellow-400' : 'text-green-400'}>{entry.result}</span></p>
            </div>
          ))}
        </div>
      </div>
    </MainLayout>
  )
}
