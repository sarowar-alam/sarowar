FROM node:18-alpine

WORKDIR /app

# Copy package files first (for better caching)
COPY package*.json ./

# Verify package.json exists and install dependencies
RUN if [ ! -f package.json ]; then echo "package.json not found!" && exit 1; fi && \
    npm install --omit=dev

# Copy source code
COPY . .

# Verify server.js exists
RUN if [ ! -f server.js ]; then echo "server.js not found!" && exit 1; fi

EXPOSE 3000

CMD ["node", "server.js"]
