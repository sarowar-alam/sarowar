#!/bin/bash

TAG=$1

# Remove refs/tags/ prefix if present
CLEAN_TAG=${TAG#refs/tags/}

# Validate tag format (semantic versioning)
if ! echo "$CLEAN_TAG" | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' > /dev/null; then
  echo "❌ Invalid tag format: $CLEAN_TAG"
  echo "📝 Must be in format: vX.Y.Z (e.g., v1.0.0)"
  exit 1
fi

echo "✅ Tag format is valid: $CLEAN_TAG"