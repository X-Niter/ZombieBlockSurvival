import os
import openai

def append_to_log(content: str):
    log_path = ".github/docs/AI_STRATEGY_LOG.md"
    if not os.path.exists(".github/docs"):
        os.makedirs(".github/docs", exist_ok=True)
    with open(log_path, "a", encoding="utf-8") as f:
        f.write(content + "\n\n---\n\n")

def main():
    title = "Fix: Crash on missing chunk data"
    diff_summary = "Check was added to validate chunk before tick access."

    prompt = f"""Summarize the following code fix into a general coding lesson or strategy that the AI can reuse.

Title: {title}
Diff: {diff_summary}

Format the lesson as a markdown section titled with the fix type or goal."""

    openai.api_key = os.getenv("OPENAI_API_KEY")
    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[{"role": "user", "content": prompt}]
    )

    lesson = response['choices'][0]['message']['content']
    append_to_log(lesson)
    print("✅ Strategy lesson logged.")

if __name__ == "__main__":
    main()
