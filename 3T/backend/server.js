const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const redis = require('redis');
const { pool, initDB } = require('./models/db');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Redis client
const redisClient = redis.createClient({
  socket: {
    host: process.env.REDIS_HOST || 'localhost',
    port: process.env.REDIS_PORT || 6379
  }
});

// Connect to Redis
const connectRedis = async () => {
  try {
    await redisClient.connect();
    console.log('Connected to Redis');
  } catch (error) {
    console.error('Redis connection error:', error);
  }
};

// Cache middleware
const cache = (req, res, next) => {
  const key = req.originalUrl;
  
  redisClient.get(key)
    .then(data => {
      if (data !== null) {
        console.log('Cache hit for:', key);
        res.json(JSON.parse(data));
      } else {
        console.log('Cache miss for:', key);
        next();
      }
    })
    .catch(err => {
      console.error('Cache error:', err);
      next();
    });
};

// Routes
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', message: 'Backend service is healthy' });
});

// Get all users (with caching)
app.get('/api/users', cache, async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM users ORDER BY created_at DESC');
    const users = result.rows;
    
    // Cache for 30 seconds
    redisClient.setEx(req.originalUrl, 30, JSON.stringify(users));
    
    res.json(users);
  } catch (error) {
    console.error('Error fetching users:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Add new user
app.post('/api/users', async (req, res) => {
  try {
    const { name } = req.body;
    if (!name) {
      return res.status(400).json({ error: 'Name is required' });
    }

    const result = await pool.query(
      'INSERT INTO users (name) VALUES ($1) RETURNING *',
      [name]
    );
    
    // Invalidate users cache
    await redisClient.del('/api/users');
    
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error adding user:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Initialize database and start server
const startServer = async () => {
  await initDB();
  await connectRedis();
  
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Backend server running on port ${PORT}`);
  });
};

startServer();

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('Shutting down gracefully...');
  await redisClient.quit();
  await pool.end();
  process.exit(0);
});