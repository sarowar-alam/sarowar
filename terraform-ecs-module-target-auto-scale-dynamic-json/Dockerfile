FROM nginx:alpine

# Install stress-ng for CPU load testing
RUN apk add --no-cache stress-ng

# Copy custom HTML and configuration
COPY index.html /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]