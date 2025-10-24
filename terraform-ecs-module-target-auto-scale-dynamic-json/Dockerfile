FROM nginx:alpine

# Install stress-ng, curl, bash, and Lua for nginx
RUN apk add --no-cache stress-ng curl bash lua5.3

# Copy custom HTML and configuration
COPY index.html /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
COPY scripts/ /usr/share/nginx/html/scripts/

# Make scripts executable
RUN chmod +x /usr/share/nginx/html/scripts/*.sh

# Create health check file
RUN echo "healthy" > /usr/share/nginx/html/health.html

# Create a simple test page
RUN echo "<html><body><h1>CPU Load Test App</h1><p>Application is running!</p></body></html>" > /usr/share/nginx/html/test.html

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]