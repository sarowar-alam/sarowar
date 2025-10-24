FROM nginx:alpine

# Install stress-ng, curl, bash and other dependencies
RUN apk add --no-cache stress-ng curl bash coreutils procps

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

# Create simple error pages
RUN echo "<html><body><h1>404 - Not Found</h1></body></html>" > /usr/share/nginx/html/404.html
RUN echo "<html><body><h1>500 - Server Error</h1></body></html>" > /usr/share/nginx/html/50x.html

# Fix shebang in scripts to use sh
RUN sed -i 's|#!/bin/bash|#!/bin/sh|' /usr/share/nginx/html/scripts/*.sh

# Create a startup script that runs nginx
RUN echo '#!/bin/sh' > /start.sh && \
    echo 'echo "Starting CPU Load Test Application..."' >> /start.sh && \
    echo 'nginx -g "daemon off;"' >> /start.sh && \
    chmod +x /start.sh

# Expose port 80
EXPOSE 80

# Start nginx using the startup script
CMD ["/start.sh"]