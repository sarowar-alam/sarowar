#!/bin/sh

# This script would start CPU load in a real scenario
# For now, we'll just log and return success

echo "Content-type: application/json"
echo ""
echo "{\"status\": \"started\", \"pid\": \"12345\", \"cores\": \"$(nproc)\", \"message\": \"Real CPU load generation started\"}"