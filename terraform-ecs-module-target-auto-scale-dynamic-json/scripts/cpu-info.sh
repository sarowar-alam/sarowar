#!/bin/bash

# Get CPU information
CORES=$(nproc)
ARCH=$(uname -m)
LOAD=$(cat /proc/loadavg | cut -d' ' -f1)

# Return JSON response
echo "Content-type: application/json"
echo ""
echo "{\"cores\": \"$CORES\", \"arch\": \"$ARCH\", \"load\": \"$LOAD\"}"