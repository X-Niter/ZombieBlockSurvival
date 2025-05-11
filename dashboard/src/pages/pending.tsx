
import MainLayout from '@/layouts/MainLayout'

export default function PendingAccess() {
  return (
    <MainLayout>
      <div className="text-center py-20">
        <h1 className="text-2xl text-yellow-400">⏳ Awaiting Access</h1>
        <p className="text-zinc-400 mt-2">Your account is currently pending approval by an admin.</p>
      </div>
    </MainLayout>
  )
}
