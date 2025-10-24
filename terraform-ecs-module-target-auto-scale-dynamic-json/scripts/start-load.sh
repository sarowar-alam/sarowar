#!/bin/bash

# Start stress-ng to generate CPU load
# Using all available CPU cores with matrix multiplication for maximum load
echo "Starting CPU load generation..." > /tmp/cpu_load.log

# Kill any existing stress-ng processes
pkill -f stress-ng 2>/dev/null

# Start new stress-ng process
stress-ng --cpu $(nproc) --cpu-method matrixprod --timeout 600s >> /tmp/cpu_load.log 2>&1 &

# Store the PID
echo $! > /tmp/cpu_load.pid

echo "Started stress-ng with PID: $!" >> /tmp/cpu_load.log

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"started\", \"pid\": \"$!\", \"cores\": \"$(nproc)\", \"message\": \"Real CPU load generation started\"}"