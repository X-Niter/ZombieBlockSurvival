
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Only POST supported' })

  const { username } = req.body
  if (!username) return res.status(400).json({ error: 'Username is required' })

  const sessionPath = path.join(process.cwd(), 'secure/session.json')
  fs.mkdirSync(path.dirname(sessionPath), { recursive: true })
  fs.writeFileSync(sessionPath, JSON.stringify({ username }, null, 2))

  res.status(200).json({ success: true, username })
}
