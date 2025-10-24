#!/bin/sh

# This script would stop CPU load in a real scenario
# For now, we'll just log and return success

echo "Content-type: application/json"
echo ""
echo "{\"status\": \"stopped\", \"message\": \"CPU load generation stopped\"}"