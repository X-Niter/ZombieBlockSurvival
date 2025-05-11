
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const configPath = path.join(process.cwd(), 'secure/onboarding.json')
  try {
    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'))
    res.status(200).json({ releaseApprovalUI: config.releaseApprovalUI || false })
  } catch (err) {
    res.status(200).json({ releaseApprovalUI: false })
  }
}
