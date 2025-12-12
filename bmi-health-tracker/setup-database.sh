#!/bin/bash

# Quick Database Setup Script for BMI Health Tracker

set -e

echo "🗄️  Database Setup for BMI Health Tracker"
echo "========================================"

# Database credentials
DB_USER="bmi_user"
DB_NAME="bmidb"

echo ""
echo "This script will:"
echo "1. Create PostgreSQL user: $DB_USER"
echo "2. Create database: $DB_NAME"
echo "3. Run migrations"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

# Get password
read -sp "Enter password for database user '$DB_USER': " DB_PASS
echo ""

# Create user
echo "Creating database user..."
sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';" 2>/dev/null || echo "User may already exist"

# Create database
echo "Creating database..."
sudo -u postgres psql -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;" 2>/dev/null || echo "Database may already exist"

# Grant privileges
echo "Granting privileges..."
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"

# Run migrations
echo "Running migrations..."
cd "$(dirname "$0")"
export PGPASSWORD=$DB_PASS
psql -U $DB_USER -d $DB_NAME -h localhost -f backend/migrations/001_create_measurements.sql

echo ""
echo "Database setup complete!"
echo ""
echo "Your DATABASE_URL should be:"
echo "postgresql://$DB_USER:$DB_PASS@localhost:5432/$DB_NAME"
echo ""
echo "Add this to your backend/.env file"
