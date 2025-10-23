#!/bin/bash

# Stop stress-ng processes
pkill -f stress-ng

# Remove PID file
rm -f /tmp/cpu_load.pid

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"stopped\"}"