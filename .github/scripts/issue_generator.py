#!/usr/bin/env python3
import os
import sys
from github import Github
import openai

def main(issue_number):
    token = os.getenv("GITHUB_TOKEN")
    api_key = os.getenv("OPENAI_API_KEY")
    repo_name = os.getenv("GITHUB_REPOSITORY")
    g = Github(token)
    repo = g.get_repo(repo_name)
    issue = repo.get_issue(int(issue_number))
    issue_body = issue.body

    prompt = f"Suggest a code fix for the following issue:\n{issue_body}"
    openai.api_key = api_key
    response = openai.ChatCompletion.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
    )
    suggestion = response.choices[0].message.content

    branch_name = f"ai-fix-{issue_number}"
    main_branch = repo.get_branch("main")
    repo.create_git_ref(ref=f"refs/heads/{branch_name}", sha=main_branch.commit.sha)

    pr = repo.create_pull(
        title=f"AI Suggestion for Issue #{issue_number}",
        body=suggestion,
        head=branch_name,
        base="main",
    )
    print(f"Created PR #{pr.number}")

if __name__ == "__main__":
    main(sys.argv[1])
