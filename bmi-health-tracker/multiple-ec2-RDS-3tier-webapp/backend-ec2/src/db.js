const { Pool } = require('pg');

// PostgreSQL connection pool configuration for AWS RDS
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000, // Close idle clients after 30 seconds
  connectionTimeoutMillis: 10000, // Increased timeout for RDS (10 seconds)
  // SSL configuration for AWS RDS
  ssl: process.env.NODE_ENV === 'production' ? {
    rejectUnauthorized: false // Required for AWS RDS SSL connections
  } : false
});

// Handle pool errors
pool.on('error', (err, client) => {
  console.error('Unexpected error on idle PostgreSQL client:', err);
  process.exit(-1);
});

// Test connection on startup
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('Database connection failed:', err.message);
    console.error('Check your DATABASE_URL in .env file');
    console.error('Ensure RDS instance is accessible from this EC2');
    process.exit(1);
  } else {
    console.log('Database connected successfully at:', res.rows[0].now);
    console.log('Connected to AWS RDS PostgreSQL');
  }
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
