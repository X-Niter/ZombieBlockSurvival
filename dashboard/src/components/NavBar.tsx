
import Link from 'next/link'

export default function NavBar() {
  return (
    <nav className="bg-zinc-950 border-b border-zinc-800 shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 py-3 flex justify-between items-center">
        <h1 className="text-xl font-bold text-red-500">🧟 ZombieBlockSurvival</h1>
        <div className="space-x-4 text-sm">
          <Link href="/" className="hover:text-red-300">Dashboard</Link>
        </div>
      </div>
    </nav>
  )
}
