FROM ubuntu:rolling

# Set environment variables to avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Combine commands to reduce layers and clean up in same RUN
RUN apt-get update && \
    apt-get install -y nginx curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy your HTML file
COPY index.nginx-debian.html /var/www/html/

# Copy custom nginx config (optional - you can use volume mount instead)
# COPY index.nginx-debian.html /etc/nginx/nginx.conf

# Expose the nginx port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]