import openai
from github import Github
import os

g = Github(os.environ['GITHUB_TOKEN'])
repo = g.get_repo(os.environ['GITHUB_REPOSITORY'])

openai.api_key = os.environ['OPENAI_API_KEY']

summary_prompt = """
Analyze the following project state and generate:
- A summary of open issues
- Notable CI or workflow failures
- Suggestions for project or automation improvements

Format output as markdown and include links to referenced issues or PRs if possible.
"""

issues = repo.get_issues(state='open')
open_issues = '\n'.join([f"- #{i.number}: {i.title}" for i in issues if not i.pull_request])
ci_failures = [i for i in issues if 'CI failed' in i.title]

response = openai.ChatCompletion.create(
  model='gpt-4',
  messages=[
    { "role": "system", "content": "You are an expert software engineering assistant reviewing GitHub repositories." },
    { "role": "user", "content": summary_prompt + "\nOpen issues:\n" + open_issues }
  ]
)

summary = response.choices[0].message.content.strip()

# Post the summary
repo.create_issue(
    title="🔍 Weekly AI Retrospective Report",
    body=summary
)
