
import type { NextApiRequest, NextApiResponse } from 'next'

export default function handler(_: NextApiRequest, res: NextApiResponse) {
  const fallback = "ZombieBlockSurvival"
  const fullRepo = process.env.GITHUB_REPOSITORY || fallback
  const parts = fullRepo.split("/")
  const repoName = parts.length === 2 ? parts[1] : fullRepo
  res.status(200).json({ name: repoName })
}
