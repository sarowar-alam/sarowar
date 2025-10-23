#!/bin/bash

# Start stress-ng to generate CPU load
# Using all available CPU cores with matrix multiplication for maximum load
stress-ng --cpu $(nproc) --cpu-method matrixprod --timeout 600s &

# Store the PID
echo $! > /tmp/cpu_load.pid

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"started\", \"pid\": \"$!\", \"cores\": \"$(nproc)\"}"