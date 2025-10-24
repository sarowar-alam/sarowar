#!/bin/bash

# Stop stress-ng processes
echo "Stopping CPU load generation..." > /tmp/cpu_stop.log

# Kill stress-ng processes
pkill -f stress-ng >> /tmp/cpu_stop.log 2>&1

# Wait a bit for processes to terminate
sleep 2

# Force kill if any remain
pkill -9 -f stress-ng >> /tmp/cpu_stop.log 2>&1

# Remove PID file
rm -f /tmp/cpu_load.pid

echo "CPU load generation stopped" >> /tmp/cpu_stop.log

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"stopped\", \"message\": \"CPU load generation stopped\"}"