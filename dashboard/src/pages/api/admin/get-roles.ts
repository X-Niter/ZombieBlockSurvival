
import type { NextApiRequest, NextApiResponse } from 'next'
import fs from 'fs'
import path from 'path'

export default function handler(_: NextApiRequest, res: NextApiResponse) {
  const file = path.join(process.cwd(), 'secure/users.json')

  if (!fs.existsSync(file)) {
    return res.status(200).json({})
  }

  const users = JSON.parse(fs.readFileSync(file, 'utf8'))
  res.status(200).json(users)
}
