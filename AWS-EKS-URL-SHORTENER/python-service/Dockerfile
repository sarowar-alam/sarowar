FROM python:3.14-slim

WORKDIR /app

# Copy requirements
COPY requirements.txt ./

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Create data directory for volume mount
RUN mkdir -p /app/data

# Copy application files
COPY app.py ./
COPY templates/ ./templates/

# Expose port
EXPOSE 5000

# Run Flask
CMD ["python", "app.py"]