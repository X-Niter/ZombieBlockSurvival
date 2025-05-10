#!/usr/bin/env bash
set -euo pipefail

# Python formatting
if [ -f requirements.txt ]; then
  pip install black isort
  black .
  isort .
fi

# Shell scripts lint optional: shellcheck
if command -v shellcheck &>/dev/null; then
  shellcheck ./**/*.sh || true
fi

# Java formatting
if [ -f pom.xml ]; then
  mvn com.coveo:fmt-maven-plugin:format
fi
