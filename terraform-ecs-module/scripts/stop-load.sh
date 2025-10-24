#!/bin/sh

# Stop stress-ng processes
echo "Stopping CPU load generation..." > /tmp/cpu_stop.log

# Read PID from file if it exists
if [ -f /tmp/cpu_load.pid ]; then
    PID=$(cat /tmp/cpu_load.pid)
    echo "Stopping process with PID: $PID" >> /tmp/cpu_stop.log
    kill $PID 2>/dev/null
    sleep 2
    # Force kill if still running
    kill -9 $PID 2>/dev/null
    rm -f /tmp/cpu_load.pid
fi

# Kill any remaining stress-ng processes
pkill -f stress-ng >> /tmp/cpu_stop.log 2>&1
sleep 1
pkill -9 -f stress-ng >> /tmp/cpu_stop.log 2>&1

echo "CPU load generation stopped at $(date)" >> /tmp/cpu_stop.log

# Verify no stress-ng processes are running
if pgrep stress-ng > /dev/null; then
    echo "Warning: Some stress-ng processes are still running" >> /tmp/cpu_stop.log
    pkill -9 stress-ng
else
    echo "All stress-ng processes stopped successfully" >> /tmp/cpu_stop.log
fi

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"stopped\", \"message\": \"CPU load generation stopped\", \"timestamp\": \"$(date)\"}"