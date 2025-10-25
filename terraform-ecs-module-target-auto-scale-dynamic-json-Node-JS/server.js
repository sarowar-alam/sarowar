const express = require('express');
const cors = require('cors');
const { exec, spawn } = require('child_process');
const os = require('os');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('public'));

// Store active stress processes
let activeStressProcesses = [];

// Utility function to format uptime
function formatUptime(seconds) {
    const days = Math.floor(seconds / (24 * 60 * 60));
    const hours = Math.floor((seconds % (24 * 60 * 60)) / (60 * 60));
    const mins = Math.floor((seconds % (60 * 60)) / 60);

    if (days > 0) return `${days} day${days > 1 ? 's' : ''} ${hours} hour${hours > 1 ? 's' : ''} ${mins} minute${mins > 1 ? 's' : ''}`;
    if (hours > 0) return `${hours} hour${hours > 1 ? 's' : ''} ${mins} minute${mins > 1 ? 's' : ''}`;
    return `${mins} minute${mins > 1 ? 's' : ''}`;
}

// Utility function to get CPU usage
function getCPUUsage() {
    return new Promise((resolve) => {
        const cpus = os.cpus();
        let totalIdle = 0, totalTick = 0;

        cpus.forEach(cpu => {
            for (let type in cpu.times) {
                totalTick += cpu.times[type];
            }
            totalIdle += cpu.times.idle;
        });

        resolve({
            usage: ((1 - totalIdle / totalTick) * 100).toFixed(2),
            load: os.loadavg()[0].toFixed(2)
        });
    });
}

// Check if stress-ng is available
function checkStressNG() {
    return new Promise((resolve) => {
        exec('which stress-ng || echo "not-found"', (error, stdout) => {
            resolve(stdout.toString().trim() !== 'not-found');
        });
    });
}

// API Routes

// Start real CPU load
app.post('/real-start-load', async (req, res) => {
    try {
        console.log('🚀 Starting real CPU load...');

        // First, stop any existing stress processes
        await stopAllStressProcesses();

        const cpuCount = os.cpus().length;
        const duration = req.body.duration || 300; // Default 5 minutes

        console.log(`Starting stress-ng on ${cpuCount} cores for ${duration} seconds`);

        // Start stress-ng process
        const stressProcess = spawn('stress-ng', [
            '--cpu', cpuCount.toString(),
            '--timeout', duration.toString(),
            '--metrics-brief'
        ], {
            detached: true,
            stdio: ['ignore', 'pipe', 'pipe']
        });

        // Store process reference
        activeStressProcesses.push(stressProcess);

        // Log output
        stressProcess.stdout.on('data', (data) => {
            console.log(`stress-ng stdout: ${data}`);
        });

        stressProcess.stderr.on('data', (data) => {
            console.log(`stress-ng stderr: ${data}`);
        });

        stressProcess.on('close', (code) => {
            console.log(`stress-ng process exited with code ${code}`);
            // Remove from active processes
            activeStressProcesses = activeStressProcesses.filter(p => p !== stressProcess);
        });

        res.json({
            status: 'started',
            pid: stressProcess.pid,
            cores: cpuCount,
            duration: duration,
            message: `Real CPU load generation started - 100% CPU utilization on ${cpuCount} cores for ${duration} seconds`
        });

    } catch (error) {
        console.error('Error starting CPU load:', error);
        res.status(500).json({
            status: 'error',
            message: 'Failed to start CPU load: ' + error.message
        });
    }
});

// Alternative method using exec (more reliable)
app.post('/real-start-load-exec', async (req, res) => {
    try {
        console.log('🚀 Starting real CPU load using exec...');

        // Stop any existing processes first
        await stopAllStressProcesses();

        const cpuCount = os.cpus().length;
        const duration = req.body.duration || 300;

        const command = `stress-ng --cpu ${cpuCount} --timeout ${duration} --metrics-brief`;

        exec(command, (error, stdout, stderr) => {
            if (error) {
                console.error('Error executing stress-ng:', error);
                return res.status(500).json({
                    status: 'error',
                    message: 'Failed to start CPU load: ' + error.message
                });
            }

            console.log('stress-ng output:', stdout);
            if (stderr) console.error('stress-ng errors:', stderr);

            res.json({
                status: 'started',
                cores: cpuCount,
                duration: duration,
                message: `Real CPU load generation completed - 100% CPU utilization on ${cpuCount} cores for ${duration} seconds`
            });
        });

    } catch (error) {
        console.error('Error starting CPU load:', error);
        res.status(500).json({
            status: 'error',
            message: 'Failed to start CPU load: ' + error.message
        });
    }
});

// Stop real CPU load
app.post('/real-stop-load', async (req, res) => {
    try {
        console.log('🛑 Stopping real CPU load...');

        await stopAllStressProcesses();

        res.json({
            status: 'stopped',
            message: 'CPU load generation stopped',
            stoppedProcesses: activeStressProcesses.length
        });

    } catch (error) {
        console.error('Error stopping CPU load:', error);
        res.status(500).json({
            status: 'error',
            message: 'Failed to stop CPU load: ' + error.message
        });
    }
});

// Stop all stress processes
async function stopAllStressProcesses() {
    return new Promise((resolve) => {
        console.log('Stopping all stress processes...');

        // Kill active Node.js child processes
        activeStressProcesses.forEach(process => {
            try {
                process.kill('SIGTERM');
            } catch (error) {
                console.error('Error killing process:', error);
            }
        });
        activeStressProcesses = [];

        // Kill any stress-ng processes that might be running
        exec('pkill -f stress-ng || true', (error) => {
            if (error) {
                console.log('No stress-ng processes found or error killing:', error);
            } else {
                console.log('Killed all stress-ng processes');
            }
            resolve();
        });
    });
}

// Get CPU information
app.get('/real-cpu-info', async (req, res) => {
    try {
        const cpuInfo = {
            cores: os.cpus().length,
            arch: os.arch(),
            platform: os.platform(),
            load: os.loadavg().map(load => load.toFixed(2)),
            uptime: formatUptime(os.uptime()),
            totalMemory: (os.totalmem() / 1024 / 1024 / 1024).toFixed(2) + ' GB',
            freeMemory: (os.freemem() / 1024 / 1024 / 1024).toFixed(2) + ' GB',
            hostname: os.hostname()
        };

        // Check if stress-ng is running
        const isStressRunning = await new Promise((resolve) => {
            exec('pgrep stress-ng', (error) => {
                resolve(!error);
            });
        });

        cpuInfo.current_status = isStressRunning ? 'High (stress-ng active)' : 'Normal';
        cpuInfo.stress_ng_available = await checkStressNG();

        res.json(cpuInfo);

    } catch (error) {
        console.error('Error getting CPU info:', error);
        res.status(500).json({
            status: 'error',
            message: 'Failed to get CPU information'
        });
    }
});

// Get current CPU usage (more detailed)
app.get('/cpu-usage', async (req, res) => {
    try {
        const cpuUsage = await getCPUUsage();
        const memoryUsage = {
            used: ((os.totalmem() - os.freemem()) / 1024 / 1024 / 1024).toFixed(2) + ' GB',
            total: (os.totalmem() / 1024 / 1024 / 1024).toFixed(2) + ' GB',
            percentage: (((os.totalmem() - os.freemem()) / os.totalmem()) * 100).toFixed(2) + '%'
        };

        res.json({
            cpu: cpuUsage,
            memory: memoryUsage,
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        console.error('Error getting CPU usage:', error);
        res.status(500).json({
            status: 'error',
            message: 'Failed to get CPU usage'
        });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

// Application status
app.get('/status', (req, res) => {
    res.json({
        status: 'running',
        name: 'CPU Load Test Application',
        version: '1.0.0',
        timestamp: new Date().toISOString(),
        activeStressProcesses: activeStressProcesses.length
    });
});

// Serve the main application
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Error handling middleware
app.use((error, req, res, next) => {
    console.error('Unhandled error:', error);
    res.status(500).json({
        status: 'error',
        message: 'Internal server error'
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        status: 'error',
        message: 'Endpoint not found'
    });
});

// Graceful shutdown
process.on('SIGTERM', async () => {
    console.log('SIGTERM received, shutting down gracefully...');
    await stopAllStressProcesses();
    process.exit(0);
});

process.on('SIGINT', async () => {
    console.log('SIGINT received, shutting down gracefully...');
    await stopAllStressProcesses();
    process.exit(0);
});

// Start server
app.listen(PORT, async () => {
    const stressAvailable = await checkStressNG();
    console.log('='.repeat(60));
    console.log('🚀 CPU Load Test Application Started');
    console.log('='.repeat(60));
    console.log(`📍 Server running on port: ${PORT}`);
    console.log(`🔧 Stress-ng available: ${stressAvailable ? '✅ Yes' : '❌ No'}`);
    console.log(`💻 CPU Cores: ${os.cpus().length}`);
    console.log(`🖥️  Architecture: ${os.arch()}`);
    console.log(`📊 Platform: ${os.platform()}`);
    console.log('='.repeat(60));
    console.log('Endpoints:');
    console.log('  GET  /              - Web interface');
    console.log('  POST /real-start-load - Start CPU load');
    console.log('  POST /real-stop-load  - Stop CPU load');
    console.log('  GET  /real-cpu-info   - CPU information');
    console.log('  GET  /cpu-usage       - Current CPU usage');
    console.log('  GET  /health          - Health check');
    console.log('  GET  /status          - Application status');
    console.log('='.repeat(60));

    if (!stressAvailable) {
        console.log('❌ WARNING: stress-ng is not installed!');
        console.log('   CPU load generation will not work properly.');
        console.log('   Make sure stress-ng is installed in the Docker container.');
    }
});

module.exports = app;