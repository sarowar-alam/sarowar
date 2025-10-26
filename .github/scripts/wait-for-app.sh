#!/bin/bash

set -e

URL=$1
MAX_RETRIES=30
RETRY_COUNT=0
SLEEP_DURATION=10

echo "⏳ Waiting for application to be ready at $URL"

until curl -f -s -o /dev/null "$URL/health"; do
  RETRY_COUNT=$((RETRY_COUNT+1))
  
  if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Application failed to become ready after $MAX_RETRIES attempts"
    exit 1
  fi
  
  echo "⏳ Application not ready yet (attempt $RETRY_COUNT/$MAX_RETRIES), retrying in ${SLEEP_DURATION}s..."
  sleep $SLEEP_DURATION
done

echo "✅ Application is healthy!"