
import fs from 'fs'
import path from 'path'

export function getUserRole(username: string): 'admin' | 'dev' | 'viewer' | null {
  try {
    const file = path.join(process.cwd(), 'secure/users.json')
    if (!fs.existsSync(file)) return null
    const users = JSON.parse(fs.readFileSync(file, 'utf8'))
    return users[username] || null
  } catch {
    return null
  }
}
