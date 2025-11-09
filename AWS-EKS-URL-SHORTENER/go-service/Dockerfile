FROM golang:1.25-alpine

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache gcc musl-dev sqlite-dev

# Copy module files
COPY go.mod go.sum ./

# Download and verify dependencies
RUN go mod download && go mod verify

# Copy source code
COPY *.go ./

# Build the application
RUN CGO_ENABLED=1 go build -tags "libsqlite3" -v -o urlshortener .

# Verify the binary exists (remove the 'file' command)
RUN ls -la urlshortener

EXPOSE 8000

CMD ["./urlshortener"]