import os
import openai

def main():
    # Simulated content (in real use, you'd pull issue text from GitHub API)
    issue_title = "Example bug: Feature fails on edge case"
    issue_body = "When trying to do X, Y fails because Z..."

    prompt = f"Summarize and triage this GitHub issue. Label it and suggest fixes:

Title: {issue_title}

Body: {issue_body}"
    openai.api_key = os.getenv("OPENAI_API_KEY")
    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[{"role": "user", "content": prompt}]
    )
    print(response['choices'][0]['message']['content'])

if __name__ == "__main__":
    main()
