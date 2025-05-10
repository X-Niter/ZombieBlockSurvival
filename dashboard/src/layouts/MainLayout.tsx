import NavBar from '@/components/NavBar'
import Footer from '@/components/Footer'

export default function MainLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-zinc-900 to-black text-white font-mono">
      <NavBar />
      <main className="flex-grow p-4 sm:p-6 md:p-10 backdrop-blur-sm bg-black/60 rounded-lg shadow-xl border border-zinc-700 mx-2 md:mx-auto max-w-6xl">
        {children}
      </main>
      <Footer />
    </div>
  )
}