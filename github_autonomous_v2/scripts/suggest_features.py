#!/usr/bin/env python3
import os
import json
from github import Github
import openai

def gather_code():
    code = ''
    for root, dirs, files in os.walk('.'):
        if root.startswith('./.github'):
            continue
        for fname in files:
            if fname.endswith(('.py','.sh','.yml','.java')):
                with open(os.path.join(root, fname), 'r') as f:
                    code += f.read() + '\n'
    return code

def main():
    token = os.getenv('GITHUB_TOKEN')
    api_key = os.getenv('OPENAI_API_KEY')
    repo = Github(token).get_repo(os.getenv('GITHUB_REPOSITORY'))
    code = gather_code()
    prompt = (
        "You are a senior software engineer. Analyze the following repository code and suggest "
        "three improvements or new feature ideas in JSON format as [{'title':'','description':''},...]:\n"
        + code
    )
    openai.api_key = api_key
    resp = openai.ChatCompletion.create(
        model='gpt-4o-mini',
        messages=[{'role':'user','content':prompt}]
    )
    try:
        suggestions = json.loads(resp.choices[0].message.content)
    except json.JSONDecodeError:
        return
    for s in suggestions:
        title = s.get('title', '').strip()
        desc = s.get('description', '').strip()
        if title and not any(title.lower() in issue.title.lower() for issue in repo.get_issues(state='open')):
            repo.create_issue(title=title, body=desc)

if __name__ == '__main__':
    main()
