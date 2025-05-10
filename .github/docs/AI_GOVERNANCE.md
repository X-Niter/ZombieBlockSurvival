# 🤖 AI Governance

This project is augmented by autonomous AI agents. To ensure safe and sustainable operation:

- 🔐 **Approval Required**: No AI-generated change is merged without human review.
- 📜 **Logs and Traceability**: All AI interactions are logged in `.ai-history/`.
- 🧠 **Models**: OpenAI GPT-4-Turbo, configured for low-temperature reliable edits.
- 🛑 **Failsafe Triggers**: If 3 failures occur from AI edits, automation halts.

## 🔄 Periodic Review

A weekly review of AI performance is executed via `retrospect.yml`, producing a summary in `AI_RETROSPECT.md`.

---

> AI is a tool, not the master.
