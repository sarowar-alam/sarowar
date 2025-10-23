FROM nginx:alpine

# Install stress-ng for CPU load testing and create CGI scripts
RUN apk add --no-cache stress-ng bash fcgiwrap

# Copy custom HTML and configuration
COPY index.html /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
COPY cgi-bin/ /usr/share/nginx/html/cgi-bin/

# Make CGI scripts executable
RUN chmod +x /usr/share/nginx/html/cgi-bin/*.sh

# Create a simple health check file
RUN echo "healthy" > /usr/share/nginx/html/health.html

# Expose port 80
EXPOSE 80

# Start nginx and fcgiwrap for CGI support
CMD sh -c "fcgiwrap -s tcp:127.0.0.1:9000 & nginx -g 'daemon off;'"