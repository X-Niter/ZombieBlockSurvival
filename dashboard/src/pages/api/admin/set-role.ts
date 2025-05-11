
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'POST only' })

  const { username, role } = req.body
  if (!username || !role) return res.status(400).json({ error: 'Missing user or role' })

  const file = path.join(process.cwd(), 'secure/users.json')
  let users = {}

  if (fs.existsSync(file)) {
    users = JSON.parse(fs.readFileSync(file, 'utf8'))
  }

  users[username] = role
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, JSON.stringify(users, null, 2))

  res.status(200).json({ success: true, users })
}
