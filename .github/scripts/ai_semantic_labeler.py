import os
import openai

def main():
    issue_title = "Bug: Inventory doesn't update in multiplayer"
    issue_body = "When two players use the inventory at the same time, items get duplicated or disappear."

    prompt = f"""You are a GitHub issue labeler.
Read the following issue and output the best semantic labels from this list: [bug, enhancement, question, discussion, performance, security, design, documentation].

Title: {issue_title}
Body: {issue_body}

Respond with a comma-separated list of labels only."""  # Instructional for clean parsing

    openai.api_key = os.getenv("OPENAI_API_KEY")
    response = openai.ChatCompletion.create(
        model="gpt-4-turbo",
        messages=[{"role": "user", "content": prompt}]
    )
    print("Labels:", response['choices'][0]['message']['content'])

if __name__ == "__main__":
    main()
