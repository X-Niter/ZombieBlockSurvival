
import type { NextApiRequest, NextApiResponse } from 'next'

export default async function handler(_: NextApiRequest, res: NextApiResponse) {
  const token = process.env.GITHUB_TOKEN
  const repo = process.env.GITHUB_REPOSITORY || 'X-Niter/ZombieBlockSurvival'
  const [owner, repoName] = repo.split('/')

  const prs = await fetch(
    `https://api.github.com/repos/\${owner}/\${repoName}/pulls`,
    {
      headers: {
        Authorization: `Bearer \${token}`,
        Accept: 'application/vnd.github+json',
      }
    }
  ).then(res => res.json())

  const aiPR = prs.find((pr: any) =>
    pr.title.toLowerCase().includes("release-ai") || pr.labels.some((l: any) => l.name === 'release-ai')
  )

  if (!aiPR) return res.status(404).json({ error: 'No AI-generated PR found' })

  const merge = await fetch(aiPR.url + '/merge', {
    method: 'PUT',
    headers: {
      Authorization: `Bearer \${token}`,
      Accept: 'application/vnd.github+json',
    }
  })

  const result = await merge.json()
  return res.status(200).json(result)
}
