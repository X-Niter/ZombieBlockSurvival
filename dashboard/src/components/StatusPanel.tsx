
export default function StatusPanel() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div className="bg-zinc-800 p-4 rounded-xl shadow-lg">
        <h2 className="font-semibold text-lg text-pink-400">AI Status</h2>
        <p className="text-green-400 mt-2">✅ Connected to OpenAI</p>
      </div>
      <div className="bg-zinc-800 p-4 rounded-xl shadow-lg">
        <h2 className="font-semibold text-lg text-blue-400">GitHub Status</h2>
        <p className="text-green-400 mt-2">✅ GitHub Access Granted</p>
      </div>
    </div>
  )
}
