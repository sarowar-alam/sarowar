# Use official NGINX as base image
FROM nginx:alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy custom NGINX configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy website files
COPY src/ /usr/share/nginx/html/

# Create health check file
RUN echo "healthy" > /usr/share/nginx/html/health

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# Start NGINX
CMD ["nginx", "-g", "daemon off;"]