#!/bin/bash

echo "Content-Type: application/json"
echo ""

CORES=$(nproc 2>/dev/null || echo "2")
ARCH=$(uname -m 2>/dev/null || echo "x86_64")

echo "{\"cores\":\"$CORES\",\"arch\":\"$ARCH\"}"