
import type { NextApiRequest, NextApiResponse } from 'next'
import crypto from 'crypto'

const GITHUB_WEBHOOK_SECRET = process.env.GITHUB_WEBHOOK_SECRET || ''

function verifySignature(req: NextApiRequest, body: string) {
  const signature = req.headers['x-hub-signature-256'] as string
  const hmac = crypto.createHmac('sha256', GITHUB_WEBHOOK_SECRET)
  const digest = 'sha256=' + hmac.update(body).digest('hex')
  return crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(digest))
}

export const config = {
  api: {
    bodyParser: false,
  },
}

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  let body = ''
  req.on('data', chunk => { body += chunk })
  req.on('end', () => {
    if (!verifySignature(req, body)) {
      return res.status(401).send('Invalid signature')
    }

    const event = req.headers['x-github-event']
    const payload = JSON.parse(body)

    console.log(`[Webhook] Event: ${event}`, payload)

    // Extend this logic for push, PR, workflow_run, etc.
    res.status(200).send('OK')
  })
}
