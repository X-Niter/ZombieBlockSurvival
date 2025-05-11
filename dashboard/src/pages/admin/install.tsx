
import Head from "next/head";
import { useState } from "react";

export default function InstallApp() {
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [token, setToken] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async () => {
    setSubmitted(true);
    await fetch('/api/github/install', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientId, clientSecret, token })
    });
    console.log("Submitted:", { clientId, clientSecret, token });
  };

  return (
    <>
      <Head>
        <title>GitHub App Install :: Admin</title>
      </Head>
      <main className="min-h-screen bg-zinc-950 text-white font-mono p-6">
        <h1 className="text-3xl font-bold text-yellow-400 mb-6">Install GitHub Integration</h1>
        <p className="text-zinc-400 mb-8">Enter your GitHub App details to link this tool with your repositories.</p>

        <div className="max-w-xl bg-zinc-900 p-6 rounded-xl shadow-md">
          <label className="block mb-4">
            <span className="text-sm text-zinc-300">Client ID</span>
            <input
              type="text"
              className="w-full mt-1 p-2 bg-zinc-800 text-white rounded"
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              placeholder="GitHub App Client ID"
            />
          </label>
          <label className="block mb-4">
            <span className="text-sm text-zinc-300">Client Secret</span>
            <input
              type="password"
              className="w-full mt-1 p-2 bg-zinc-800 text-white rounded"
              value={clientSecret}
              onChange={(e) => setClientSecret(e.target.value)}
              placeholder="GitHub App Client Secret"
            />
          </label>
          <label className="block mb-6">
            <span className="text-sm text-zinc-300">Token (PAT or App JWT)</span>
            <input
              type="password"
              className="w-full mt-1 p-2 bg-zinc-800 text-white rounded"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Access Token"
            />
          </label>

          <button
            className="bg-blue-600 hover:bg-blue-500 transition px-4 py-2 rounded text-white w-full"
            onClick={handleSubmit}
          >
            Submit & Install
          </button>

          {submitted && (
            <p className="mt-4 text-green-400">Installation info submitted (mock). Hook up backend to complete.</p>
          )}
        </div>
      </main>
    </>
  );
}
