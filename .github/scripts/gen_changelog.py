import os
import openai
import subprocess

openai.api_key = os.getenv("OPENAI_API_KEY")

# Get last 10 commit messages
commits = subprocess.getoutput("git log -10 --pretty=format:'%s'").strip().split("\n")
prompt = "\n".join(f"- {msg.strip('- ')}" for msg in commits)

completion = openai.ChatCompletion.create(
    model="gpt-4",
    messages=[{
        "role": "user",
        "content": f"You are a dev assistant. Summarize these recent commit messages into a Markdown changelog with emojis and grouped changes:

{prompt}"
    }],
    max_tokens=300
)

changelog = completion.choices[0].message.content.strip()
print(changelog)
