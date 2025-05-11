
import { useState } from 'react'

export default function ApiKeyPrompt({ onSubmit }: { onSubmit: (key: string) => void }) {
  const [apiKey, setApiKey] = useState("")

  return (
    <div className="p-6 bg-zinc-900 border border-red-400 rounded-xl text-white">
      <h2 className="text-red-400 font-bold mb-2">Missing API Key</h2>
      <p className="text-zinc-300 text-sm mb-4">Please provide your OpenAI API Key to enable command functionality.</p>
      <input
        type="password"
        className="w-full p-2 bg-zinc-800 text-zinc-100 rounded mb-3"
        placeholder="sk-..."
        value={apiKey}
        onChange={e => setApiKey(e.target.value)}
      />
      <button
        className="px-4 py-2 rounded bg-green-500 text-white hover:bg-green-600"
        onClick={() => onSubmit(apiKey)}
      >
        Submit Key
      </button>
    </div>
  )
}
