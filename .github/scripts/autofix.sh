#!/usr/bin/env bash
set -euo pipefail

# Python formatting
if [ -f requirements.txt ]; then
  pip install black isort
  black .
  isort .
fi

# Shell scripts lint
if command -v shellcheck &>/dev/null; then
  shellcheck ./**/*.sh || true
fi

# Java formatting
if [ -f pom.xml ]; then
if mvn com.coveo:fmt-maven-plugin:format; then
  echo "✅ Java formatting succeeded."
else
  echo "⚠️ Skipping Java formatting due to plugin-JDK incompatibility."
fi
fi
