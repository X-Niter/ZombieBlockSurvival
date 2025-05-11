
import type { NextApiRequest, NextApiResponse } from 'next'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === 'POST') {
    const manifest = {
      name: 'ZombieBlockSurvival Auto-AI',
      url: 'https://yourwebsite.com',
      redirect_url: 'https://yourwebsite.com/api/github/install',
      hook_attributes: {
        url: 'https://yourwebsite.com/api/github/webhook'
      },
      public: true,
      default_events: ['push', 'pull_request', 'workflow_run'],
      default_permissions: {
        contents: 'read',
        pull_requests: 'write',
        metadata: 'read',
        actions: 'read'
      }
    }

    const resApp = await fetch('https://api.github.com/app-manifests/create', {
      method: 'POST',
      headers: {
        Accept: 'application/vnd.github+json',
        Authorization: "Bearer " + process.env.GITHUB_TOKEN
      },
      body: JSON.stringify(manifest)
    })

    const data = await resApp.json()
    return res.status(200).json(data)
  }

  res.status(405).json({ error: 'Only POST allowed' })
}
