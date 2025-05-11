
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default function handler(_: NextApiRequest, res: NextApiResponse) {
  const sessionPath = path.join(process.cwd(), 'secure/session.json')
  if (!fs.existsSync(sessionPath)) return res.status(200).json({ username: null })
  const session = JSON.parse(fs.readFileSync(sessionPath, 'utf8'))
  res.status(200).json(session)
}
