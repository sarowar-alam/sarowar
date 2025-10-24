#!/bin/sh

# Get REAL CPU information
CORES=$(nproc 2>/dev/null || echo "2")
ARCH=$(uname -m 2>/dev/null || echo "x86_64")
LOAD=$(cat /proc/loadavg 2>/dev/null | cut -d' ' -f1 || echo "0.00")
UPTIME=$(uptime 2>/dev/null | cut -d',' -f1 | cut -d' ' -f4- || echo "0:00")

# Check if stress-ng is running
STRESS_RUNNING="false"
if pgrep stress-ng > /dev/null 2>&1; then
    STRESS_RUNNING="true"
    LOAD="$(cat /proc/loadavg 2>/dev/null | cut -d' ' -f1 || echo "high")"
fi

# Get memory info
MEMORY=$(free -m 2>/dev/null | awk 'NR==2{printf "%.1f%%", $3*100/$2}' || echo "0%")

echo "Content-type: application/json"
echo ""
echo "{\"cores\": \"$CORES\", \"arch\": \"$ARCH\", \"load\": \"$LOAD\", \"uptime\": \"$UPTIME\", \"memory_usage\": \"$MEMORY\", \"stress_running\": \"$STRESS_RUNNING\"}"