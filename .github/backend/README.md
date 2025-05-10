# 🧠 GitHub AI App Backend

This Flask server handles:
- Webhook events from GitHub
- Authentication via JWT (GitHub App style)
- Auto-analysis and AI learning triggers

## Setup

1. Register a GitHub App: https://github.com/settings/apps
2. Use `/webhook` as your webhook URL.
3. Generate a private key and set `APP_ID` + `PRIVATE_KEY_PATH`.
4. Run with:

```bash
pip install flask pyjwt cryptography
python app.py
```
