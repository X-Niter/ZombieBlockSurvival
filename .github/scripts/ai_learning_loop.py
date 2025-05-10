import os
import openai

def main():
    merged_title = "Fix: Correct logic for chunk load boundaries"
    merged_diff = "Diff shows we adjusted tick scheduling to prevent double-load of border chunks."

    prompt = f"""You are helping an AI improve how it analyzes and writes code.

A PR titled '{merged_title}' was recently merged. Here's the summary of the change:
{merged_diff}

Teach the AI what pattern it learned and how it could recognize or apply this type of fix in the future.

Explain concisely and technically for future prompts.""" 

    openai.api_key = os.getenv("OPENAI_API_KEY")
    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[{"role": "user", "content": prompt}]
    )

    lesson = response['choices'][0]['message']['content']
    print("🧠 Learned Strategy:\n", lesson)

    # Append to strategies.log
    log_path = ".github/ai/strategies.log"
    with open(log_path, "a") as log_file:
        log_file.write(f"## PR: {merged_title}\n")
        log_file.write(f"{lesson}\n\n")

if __name__ == "__main__":
    main()
