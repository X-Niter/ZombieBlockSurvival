
import Head from 'next/head'
import MainLayout from '@/layouts/MainLayout'

export default function Login() {
  return (
    <MainLayout>
      <Head><title>Login with GitHub</title></Head>
      <div className="py-20 max-w-md mx-auto text-center space-y-4">
        <h1 className="text-2xl font-bold text-yellow-400">🔐 Admin Login</h1>
        <a
          href={"https://github.com/login/oauth/authorize?client_id=" + process.env.NEXT_PUBLIC_GITHUB_CLIENT_ID + "&scope=read:user"}
          className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded inline-block mt-4"
        >
          Login with GitHub
        </a>
      </div>
    </MainLayout>
  )
}