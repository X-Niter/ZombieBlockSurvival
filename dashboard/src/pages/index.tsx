
import Head from "next/head";
import Link from "next/link";
import { useEffect, useState } from "react";

export default function Home() {
  const [ghStatus, setGhStatus] = useState("Checking...");
  const [aiStatus, setAiStatus] = useState("Checking...");
  const [latestRun, setLatestRun] = useState(null);
  const [prompt, setPrompt] = useState("");
  const [aiResponse, setAiResponse] = useState("");

  useEffect(() => {
    // Fake status check (replace with real GitHub/OpenAI calls)
    setTimeout(() => {
      setGhStatus("✅ Connected");
      setAiStatus("✅ Available");
      setLatestRun({
        workflow: "Talisman Security",
        status: "Success",
        commit: "Fix lint issues in plugin loader",
      });
    }, 1000);
  }, []);

  const runAiPrompt = async () => {
    // Simulate AI processing
    setAiResponse("Analyzing your repo... No critical issues detected. Great job!");
  };

  return (
    <>
      <Head>
        <title>ZombieBlockSurvival :: Dev Command</title>
      </Head>
      <main className="min-h-screen p-6 bg-zinc-950 text-white font-mono">
        <h1 className="text-3xl font-bold mb-4 text-red-400">Welcome, Survivor</h1>
        <p className="mb-8 text-zinc-400">Your post-apocalyptic AI dev center.</p>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          <div className="bg-zinc-900 p-4 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold text-pink-400 mb-2">AI Status</h2>
            <p>{aiStatus}</p>
          </div>

          <div className="bg-zinc-900 p-4 rounded-xl shadow-md">
            <h2 className="text-lg font-semibold text-green-400 mb-2">GitHub Status</h2>
            <p>{ghStatus}</p>
          </div>

          <div className="bg-zinc-900 p-4 rounded-xl shadow-md col-span-1 md:col-span-2 xl:col-span-1">
            <h2 className="text-lg font-semibold text-purple-400 mb-2">Custom Prompt</h2>
            <textarea
              className="w-full bg-zinc-800 text-white p-2 rounded resize-none mb-2"
              rows={4}
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Describe what you'd like AI to review or improve..."
            />
            <button
              onClick={runAiPrompt}
              className="bg-purple-600 hover:bg-purple-500 transition px-4 py-2 rounded text-white"
            >
              Send Prompt
            </button>
            {aiResponse && <p className="mt-2 text-sm text-green-300">{aiResponse}</p>}
          </div>

          <div className="bg-zinc-900 p-4 rounded-xl shadow-md col-span-full">
            <h2 className="text-lg font-semibold text-cyan-400 mb-2">Latest Workflow Run</h2>
            {latestRun ? (
              <ul className="list-disc list-inside text-zinc-300">
                <li><strong>Workflow:</strong> {latestRun.workflow}</li>
                <li><strong>Status:</strong> {latestRun.status}</li>
                <li><strong>Commit:</strong> {latestRun.commit}</li>
              </ul>
            ) : (
              <p>Loading workflow data...</p>
            )}
          </div>
        </div>

        <footer className="mt-16 text-zinc-600 text-sm text-center">
          &copy; 2025 ZombieBlockSurvival. Built to outlast the apocalypse.
        </footer>
      </main>
    </>
  );
}
