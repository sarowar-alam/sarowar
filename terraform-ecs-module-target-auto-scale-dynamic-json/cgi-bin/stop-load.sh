#!/bin/bash

echo "Content-Type: application/json"
echo ""

# Stop all stress-ng processes
pkill stress-ng

echo "{\"status\":\"stopped\"}"