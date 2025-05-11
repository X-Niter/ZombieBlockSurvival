
import type { NextApiRequest, NextApiResponse } from 'next'

export default function handler(_: NextApiRequest, res: NextApiResponse) {
  // Simulated results for now
  res.status(200).json([
    {
      commit: "2d5e8b1",
      author: "X-Niter",
      date: "2025-05-10",
      result: "✅ No secrets found",
    },
    {
      commit: "6f2a3de",
      author: "Contributor42",
      date: "2025-05-09",
      result: "⚠️ Possible AWS key detected",
    },
  ])
}
