
import type { NextApiRequest, NextApiResponse } from "next";

// Dummy store (in memory – replace with encrypted storage later)
let cachedAppInstallData: {
  clientId: string;
  clientSecret: string;
  token: string;
} | null = null;

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === "POST") {
    const { clientId, clientSecret, token } = req.body;

    if (!clientId || !clientSecret || !token) {
      return res.status(400).json({ error: "Missing required fields." });
    }

    cachedAppInstallData = { clientId, clientSecret, token };
    console.log("Received GitHub App credentials:", cachedAppInstallData);

    return res.status(200).json({ success: true, message: "Installation data received." });
  }

  return res.status(405).json({ error: "Method not allowed" });
}
