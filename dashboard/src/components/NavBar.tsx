import Link from 'next/link'

export default function NavBar() {
  return (
    <nav className="w-full py-4 px-6 flex justify-between items-center border-b border-zinc-800 bg-black bg-opacity-70 shadow-md">
      <h1 className="text-xl sm:text-2xl font-bold text-amber-400 tracking-widest">🧟 ZombieBlockSurvival</h1>
      <div className="space-x-4 text-sm sm:text-base text-gray-300">
        <Link href="/" className="hover:text-amber-500 transition">Dashboard</Link>
        <Link href="/workflow-status" className="hover:text-amber-500 transition">Workflows</Link>
        <Link href="/changelogs" className="hover:text-amber-500 transition">Changelog</Link>
      </div>
    </nav>
  )
}