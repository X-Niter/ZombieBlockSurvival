import os
import openai

def main():
    issue_title = "Refactor AI logic engine"
    issue_body = "We need to optimize how the AI analyzes code structure. It's currently too slow for large PRs."

    openai.api_key = os.getenv("OPENAI_API_KEY")

    prompt = f"""You're a senior software engineer helping on a GitHub repo.
Someone wrote an issue or PR titled:
{issue_title}

With the following content:
{issue_body}

Write a conversational response as if you're giving helpful, thoughtful feedback. Focus on being insightful, honest, and encouraging.""" 

    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[{"role": "user", "content": prompt}]
    )

    reply = response['choices'][0]['message']['content']
    print("AI Reply:
", reply)

if __name__ == "__main__":
    main()
