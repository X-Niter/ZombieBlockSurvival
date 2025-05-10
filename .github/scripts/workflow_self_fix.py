#!/usr/bin/env python3
import os
import re

version_patterns = {
    r'actions/checkout@v\\d+': 'actions/checkout@v4',
    r'actions/setup-python@v\\d+': 'actions/setup-python@v4',
    r'actions/setup-java@v\\d+': 'actions/setup-java@v3',
    r'actions/cache@v\\d+': 'actions/cache@v3',
    r'actions/upload-artifact@v\\d+': 'actions/upload-artifact@v3',
}

permissions_block = "permissions:\n  contents: read\n  checks: write\n\nconcurrency:\n  group: ${{ github.workflow }}-${{ github.ref }}\n  cancel-in-progress: true\n\n"

for root, dirs, files in os.walk('.github/workflows'):
    for file in files:
        if file.endswith(('.yml', '.yaml')):
            path = os.path.join(root, file)
            content = open(path).read()
            # bump versions
            for pat, repl in version_patterns.items():
                content = re.sub(pat, repl, content)
            # insert permissions if missing
            if 'permissions:' not in content:
                lines = content.splitlines(keepends=True)
                idx = next((i for i, l in enumerate(lines) if l.strip().startswith('jobs:')), None)
                if idx is not None:
                    lines.insert(idx, permissions_block)
                content = ''.join(lines)
            open(path, 'w').write(content)
