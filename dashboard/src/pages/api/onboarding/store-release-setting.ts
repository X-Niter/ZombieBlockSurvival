
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' })

  const { releaseApprovalUI } = req.body
  const configPath = path.join(process.cwd(), 'secure/onboarding.json')

  const config = { releaseApprovalUI: !!releaseApprovalUI }
  fs.mkdirSync(path.dirname(configPath), { recursive: true })
  fs.writeFileSync(configPath, JSON.stringify(config, null, 2))

  res.status(200).json({ success: true, config })
}
