
import NavBar from '@/components/NavBar'
import Footer from '@/components/Footer'

export default function MainLayout({ children }) {
  return (
    <div className="flex flex-col min-h-screen bg-zinc-900 text-white">
      <NavBar />
      <main className="flex-grow">{children}</main>
      <Footer />
    </div>
  )
}
