FROM nginx:alpine

# Install stress-ng and other dependencies
RUN apk add --no-cache stress-ng curl bash

# Copy custom HTML and configuration
COPY index.html /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
COPY scripts/ /usr/share/nginx/html/scripts/

# Make scripts executable
RUN chmod +x /usr/share/nginx/html/scripts/*.sh

# Create health check file
RUN echo "healthy" > /usr/share/nginx/html/health.html

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]