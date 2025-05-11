
import type { NextApiRequest, NextApiResponse } from 'next'

export default function handler(_: NextApiRequest, res: NextApiResponse) {
  const manifest = {
    name: "ZombieBlockSurvival AI Automation",
    url: "https://github.com/X-Niter/ZombieBlockSurvival",
    hook_attributes: {
      url: "https://zombieblocksurvival.com/api/github/webhook"
    },
    redirect_url: "https://zombieblocksurvival.com/admin/install-complete",
    public: true,
    default_permissions: {
      contents: "read",
      pull_requests: "write",
      actions: "read",
      metadata: "read"
    },
    default_events: ["push", "pull_request", "workflow_run"]
  }

  res.status(200).json(manifest)
}
