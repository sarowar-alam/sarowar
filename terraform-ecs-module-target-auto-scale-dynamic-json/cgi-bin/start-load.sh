#!/bin/bash

echo "Content-Type: application/json"
echo ""

# Start stress-ng in background
stress-ng --cpu 0 --cpu-method matrixprod --timeout 300s > /dev/null 2>&1 &
PID=$!

echo "{\"pid\":\"$PID\",\"status\":\"started\"}"