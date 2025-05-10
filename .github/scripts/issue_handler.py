#!/usr/bin/env python3
import os
import json
from github import Github
import openai

def load_event():
    path = os.getenv('GITHUB_EVENT_PATH')
    return json.load(open(path))

def is_duplicate(repo, title, current_number=None):
    """Check if another open issue has the same title."""
    for issue in repo.get_issues(state='open'):
        if issue.number == current_number:
            continue
        if title.strip().lower() == issue.title.strip().lower():
            return True
    return False

def main():
    token = os.getenv('GITHUB_TOKEN')
    api_key = os.getenv('OPENAI_API_KEY')
    repo = Github(token).get_repo(os.getenv('GITHUB_REPOSITORY'))
    event = load_event()
    # Determine issue or comment
    if 'issue' in event:
        issue = event['issue']
        text = issue.get('body', '')
        title = issue.get('title', '')
        number = issue.get('number')
    else:
        # comment event
        comment = event['comment']
        text = comment.get('body', '')
        issue = event['issue']
        title = issue.get('title', '')
        number = issue.get('number')
    # Deduplicate
        if is_duplicate(repo, title, number):
            return
    # Generate response
    prompt = f"""You are a senior software engineer. Provide a helpful response or fix suggestion for this issue or comment:\n{text}"""
    openai.api_key = api_key
    response = openai.ChatCompletion.create(
        model='gpt-4o-mini',
        messages=[{'role':'user','content':prompt}]
    )
    reply = response.choices[0].message.content
    issue_obj = repo.get_issue(number)
    issue_obj.create_comment(reply)

if __name__ == '__main__':
    main()
