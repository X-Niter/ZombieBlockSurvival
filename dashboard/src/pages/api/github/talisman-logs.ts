
import type { NextApiRequest, NextApiResponse } from 'next'

export default async function handler(_: NextApiRequest, res: NextApiResponse) {
  const token = process.env.GITHUB_TOKEN
  const repo = process.env.GITHUB_REPOSITORY || 'X-Niter/ZombieBlockSurvival'
  const [owner, repoName] = repo.split('/')

  if (!token) return res.status(401).json({ error: 'Missing GITHUB_TOKEN' })

  const runsRes = await fetch(
    `https://api.github.com/repos/\${owner}/\${repoName}/actions/runs?per_page=5`,
    {
      headers: {
        Authorization: `Bearer \${token}`,
        Accept: 'application/vnd.github+json',
      }
    }
  )
  const runs = await runsRes.json()
  const relevant = []

  for (const run of runs.workflow_runs || []) {
    const logsRes = await fetch(run.logs_url, {
      headers: {
        Authorization: `Bearer \${token}`,
        Accept: 'application/vnd.github+json',
      }
    })
    const logs = await logsRes.text()

    if (logs.includes('Talisman Report')) {
      relevant.push({
        commit: run.head_commit?.id?.substring(0, 7) || run.head_sha?.substring(0, 7),
        author: run.head_commit?.author?.name || 'Unknown',
        date: run.head_commit?.timestamp?.split('T')[0] || run.created_at?.split('T')[0],
        result: logs.includes('secret') || logs.includes('detected') ? '⚠️ Possible secret detected' : '✅ No secrets found'
      })
    }
  }

  res.status(200).json(relevant)
}
