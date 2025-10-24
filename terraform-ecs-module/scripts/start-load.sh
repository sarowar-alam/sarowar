#!/bin/sh

# Start stress-ng to generate REAL CPU load
echo "Starting REAL CPU load generation..." > /tmp/cpu_load.log

# Kill any existing stress-ng processes first
pkill -f stress-ng 2>/dev/null
sleep 2

# Start stress-ng with matrix multiplication (CPU intensive)
# Using all available CPU cores for maximum load
stress-ng --cpu $(nproc) --cpu-method matrixprod --timeout 300s > /tmp/stress-ng-output.log 2>&1 &

# Store the PID
STRESS_PID=$!
echo $STRESS_PID > /tmp/cpu_load.pid

echo "Started stress-ng with PID: $STRESS_PID" >> /tmp/cpu_load.log
echo "CPU cores: $(nproc)" >> /tmp/cpu_load.log
echo "Stress-ng command: stress-ng --cpu $(nproc) --cpu-method matrixprod --timeout 300s" >> /tmp/cpu_load.log

# Verify the process is running
sleep 1
if ps -p $STRESS_PID > /dev/null 2>&1; then
    echo "Process $STRESS_PID is running successfully" >> /tmp/cpu_load.log
    STATUS="started"
    MESSAGE="Real CPU load generation started successfully"
else
    echo "Process $STRESS_PID failed to start" >> /tmp/cpu_load.log
    STATUS="failed"
    MESSAGE="Failed to start CPU load generation"
    # Try alternative method
    stress-ng --cpu $(nproc) --timeout 300s > /tmp/stress-ng-fallback.log 2>&1 &
    echo $! > /tmp/cpu_load.pid
    if ps -p $! > /dev/null 2>&1; then
        STATUS="started"
        MESSAGE="Real CPU load started with fallback method"
    fi
fi

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"status\": \"$STATUS\", \"pid\": \"$STRESS_PID\", \"cores\": \"$(nproc)\", \"message\": \"$MESSAGE\"}"