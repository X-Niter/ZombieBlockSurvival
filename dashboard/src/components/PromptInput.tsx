
import { useState } from 'react'

export default function PromptInput() {
  const [prompt, setPrompt] = useState('')
  const [result, setResult] = useState('')

  const sendPrompt = async () => {
    setResult('⏳ Sending...')
    setTimeout(() => {
      setResult(`🧠 AI says: "${prompt}" (simulated)`)
    }, 1200)
  }

  return (
    <div className="bg-zinc-800 p-6 rounded-xl shadow-lg mt-6">
      <h2 className="text-xl font-bold mb-2 text-purple-400">Custom Prompt</h2>
      <textarea
        className="w-full p-2 bg-zinc-700 rounded-md text-white"
        rows={4}
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder="Ask AI to refactor code, fix a bug, or suggest improvements..."
      />
      <button onClick={sendPrompt} className="mt-3 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-md text-white">
        Send Prompt
      </button>
      {result && <p className="mt-4 text-green-400 whitespace-pre-wrap">{result}</p>}
    </div>
  )
}
