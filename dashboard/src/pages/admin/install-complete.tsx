
import MainLayout from '@/layouts/MainLayout'

export default function InstallComplete() {
  return (
    <MainLayout>
      <div className="text-center py-20">
        <h1 className="text-2xl font-bold text-green-400">🎉 GitHub App Installed</h1>
        <p className="text-zinc-400 mt-2">Your GitHub App is now connected and ready for action.</p>
      </div>
    </MainLayout>
  )
}
