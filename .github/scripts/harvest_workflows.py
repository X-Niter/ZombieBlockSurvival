# harvest_workflows.py
import os, json
from github import Github

gh = Github(os.getenv("GITHUB_TOKEN"))
query = 'language:Java topic:paper-plugin in:path .github/workflows'
repos = gh.search_repositories(query, sort='stars', order='desc')[:5]
manifest = []
for r in repos:
    try:
        contents = r.get_contents(".github/workflows")
        workflows = [f.path for f in contents if f.path.endswith(".yml")]
    except:
        workflows = []
    manifest.append({'repo': r.full_name, 'workflows': workflows})
with open('.github/community/workflows_manifest.json', 'w') as f:
    json.dump(manifest, f, indent=2)
