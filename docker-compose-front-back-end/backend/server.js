const express = require('express');
const app = express();
const port = 3000;

app.get('/api/health', (req, res) => {
    res.json({ status: 'OK', message: 'Backend service is running' });
});

app.get('/api/data', (req, res) => {
    res.json({ data: 'Hello from backend API!', timestamp: new Date().toISOString() });
});

app.listen(port, '0.0.0.0', () => {
    console.log(`Backend service running on port ${port}`);
});
