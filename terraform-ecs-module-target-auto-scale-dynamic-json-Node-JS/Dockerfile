FROM node:18-alpine

# Install stress-ng for CPU load generation
RUN apk add --no-cache stress-ng

# Create app directory
WORKDIR /app

# Copy package files
COPY package*.json ./
COPY server.js ./

# Create public directory
RUN mkdir -p public
COPY public/index.html public/

# Install dependencies
RUN npm install

# Create non-root user
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nodejs -u 1001

# Change ownership of the app directory
RUN chown -R nodejs:nodejs /app

# Switch to non-root user
USER nodejs

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000/health', (r) => process.exit(r.statusCode === 200 ? 0 : 1))"

# Start the application
CMD ["npm", "start"]