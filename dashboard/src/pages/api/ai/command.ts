import type { NextApiRequest, NextApiResponse } from 'next'

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Only POST supported' })

  const prompt = req.body.prompt || ''
  const key = process.env.OPENAI_API_KEY || req.headers['x-openai-key'] || null

  if (!key) {
    return res.status(400).json({
      error: 'Missing OpenAI key. Please provide one via onboarding.',
      requireApiKey: true,
    })
  }

  try {
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: 'gpt-4-turbo',
        messages: [{ role: 'user', content: prompt }],
      }),
    })

    const result = await response.json()
    const reply = result.choices?.[0]?.message?.content || 'No response received.'
    return res.status(200).json({ reply })
  } catch (e) {
    return res.status(500).json({ error: 'OpenAI request failed', details: e.message })
  }
}