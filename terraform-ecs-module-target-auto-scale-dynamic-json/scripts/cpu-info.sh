#!/bin/sh

# Get CPU information
CORES=$(nproc 2>/dev/null || echo "2")
ARCH=$(uname -m 2>/dev/null || echo "x86_64")
LOAD=$(cat /proc/loadavg 2>/dev/null | cut -d' ' -f1 || echo "0.75")
UPTIME=$(uptime 2>/dev/null | cut -d',' -f1 | cut -d' ' -f4- || echo "1:00")

echo "Content-type: application/json"
echo ""
echo "{\"cores\": \"$CORES\", \"arch\": \"$ARCH\", \"load\": \"$LOAD\", \"uptime\": \"$UPTIME\"}"