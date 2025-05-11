import type { NextApiRequest, NextApiResponse } from "next";

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const manifest = {
    name: "ZombieBlockSurvival Dev Assistant",
    url: "https://your-site.com",
    hook_attributes: {
      url: "https://your-site.com/api/github/webhook"
    },
    redirect_url: "https://your-site.com/onboarding/complete",
    public: true,
    default_permissions: {
      contents: "read",
      issues: "write",
      pull_requests: "write",
      workflows: "write"
    },
    default_events: [
      "push",
      "pull_request",
      "issues",
      "workflow_run"
    ]
  };

  res.status(200).json(manifest);
}
