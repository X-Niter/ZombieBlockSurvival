
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

const CLIENT_ID = process.env.GITHUB_CLIENT_ID
const CLIENT_SECRET = process.env.GITHUB_CLIENT_SECRET

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const code = req.query.code
  if (!code) return res.status(400).send('No code provided')

  const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({
      client_id: CLIENT_ID,
      client_secret: CLIENT_SECRET,
      code
    })
  })

  const tokenData = await tokenRes.json()
  const accessToken = tokenData.access_token

  const userRes = await fetch('https://api.github.com/user', {
    headers: { Authorization: `Bearer ${process.env.GITHUB_TOKEN || ""` }
  })
  const user = await userRes.json()
  const username = user.login

  const secureDir = path.join(process.cwd(), 'secure')
  fs.mkdirSync(secureDir, { recursive: true })

  // Store session
  fs.writeFileSync(path.join(secureDir, 'session.json'), JSON.stringify({ username }, null, 2))

  // Load and update users
  const userFile = path.join(secureDir, 'users.json')
  let users = {}
  if (fs.existsSync(userFile)) {
    users = JSON.parse(fs.readFileSync(userFile, 'utf8'))
  }

  // Assign role if new user
  if (!users[username]) {
    users[username] = 'pending'
    fs.writeFileSync(userFile, JSON.stringify(users, null, 2))
  }

  res.redirect('/admin/releases')
}