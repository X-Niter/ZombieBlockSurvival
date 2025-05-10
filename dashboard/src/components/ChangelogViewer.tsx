
export default function ChangelogViewer() {
  const sample = [
    { version: 'v1.0.1', changes: ['Added AI workflow runner', 'Improved token handling', 'Refactored status panel'] },
    { version: 'v1.0.0', changes: ['Initial UI created', 'Dashboard layout finalized'] }
  ]

  return (
    <div className="bg-zinc-800 p-6 rounded-xl shadow-lg mt-6">
      <h2 className="text-xl font-bold mb-2 text-amber-400">Changelogs</h2>
      <div className="space-y-4">
        {sample.map((log, i) => (
          <div key={i}>
            <h3 className="text-lg font-semibold text-white">{log.version}</h3>
            <ul className="list-disc ml-6 text-gray-300">
              {log.changes.map((item, idx) => <li key={idx}>{item}</li>)}
            </ul>
          </div>
        ))}
      </div>
    </div>
  )
}
