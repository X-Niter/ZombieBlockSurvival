
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const code = req.query.code
  const installation_id = req.query.installation_id

  const installInfo = {
    code,
    installation_id,
    timestamp: new Date().toISOString()
  }

  const installPath = path.join(process.cwd(), 'secure/github_app_install.json')
  fs.mkdirSync(path.dirname(installPath), { recursive: true })
  fs.writeFileSync(installPath, JSON.stringify(installInfo, null, 2))

  res.redirect('/admin/releases')
}
