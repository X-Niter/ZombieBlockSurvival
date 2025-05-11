
import type { NextApiRequest, NextApiResponse } from 'next'

export default async function handler(_: NextApiRequest, res: NextApiResponse) {
  const token = process.env.GITHUB_TOKEN
  const repo = process.env.GITHUB_REPOSITORY || 'X-Niter/ZombieBlockSurvival'
  const [owner, repoName] = repo.split('/')

  if (!token) return res.status(401).json({ error: 'Missing GITHUB_TOKEN' })

  const workflows = await fetch(
    \`https://api.github.com/repos/\${owner}/\${repoName}/actions/runs?per_page=5\`,
    {
      headers: {
        Authorization: \`Bearer \${token}\`,
        Accept: 'application/vnd.github+json',
      }
    }
  )
  const data = await workflows.json()
  return res.status(200).json(data.workflow_runs || [])
}
