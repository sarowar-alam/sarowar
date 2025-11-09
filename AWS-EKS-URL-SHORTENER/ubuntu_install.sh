#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Update system
print_status "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Git
print_status "Installing Git..."
if command_exists git; then
    CURRENT_GIT=$(git --version)
    print_warning "Git is already installed: $CURRENT_GIT"
else
    sudo apt install -y git
    print_success "Git installed successfully: $(git --version)"
fi

# Install essential packages
print_status "Installing essential packages..."
sudo apt install -y curl wget build-essential libssl-dev ca-certificates software-properties-common

# Install Go (latest from official Go site AND golang-go from apt)
print_status "Installing Go from official Go site (latest)..."
if command_exists go; then
    CURRENT_GO=$(go version)
    print_warning "Go is already installed: $CURRENT_GO"
else
    # Download latest Go
    GO_VERSION=$(curl -s https://go.dev/VERSION?m=text | head -1)
    GO_URL="https://go.dev/dl/${GO_VERSION}.linux-amd64.tar.gz"
    
    wget -q "$GO_URL" -O /tmp/go.tar.gz
    sudo tar -C /usr/local -xzf /tmp/go.tar.gz
    rm /tmp/go.tar.gz
    
    # Add Go to PATH
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
    echo 'export PATH=$PATH:$(go env GOPATH)/bin' >> ~/.bashrc
    export PATH=$PATH:/usr/local/go/bin
    
    print_success "Go installed successfully: $(go version)"
fi

# Also install golang-go from apt for additional tools
print_status "Installing golang-go from apt..."
sudo apt install -y golang-go
print_success "golang-go installed from apt"

# Install Python (latest)
print_status "Installing Python latest..."
sudo apt install -y python3 python3-pip python3-venv python3-dev

# Create symlinks for python and pip
sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 1
sudo update-alternatives --install /usr/bin/pip pip /usr/bin/pip3 1

print_success "Python installed successfully: $(python3 --version)"
print_success "Pip installed successfully: $(pip3 --version)"

# Install Node.js with npm (latest)
print_status "Installing Node.js and npm..."
if command_exists node; then
    CURRENT_NODE=$(node --version)
    print_warning "Node.js is already installed: $CURRENT_NODE"
else
    # Using NodeSource repository for latest version
    curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
    sudo apt install -y nodejs
    
    print_success "Node.js installed successfully: $(node --version)"
    print_success "npm installed successfully: $(npm --version)"
fi

# Install Redis 7+
print_status "Installing Redis..."
if command_exists redis-server; then
    CURRENT_REDIS=$(redis-server --version)
    print_warning "Redis is already installed: $CURRENT_REDIS"
else
    # Add Redis repository for latest version
    curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/redis.list
    
    sudo apt update
    sudo apt install -y redis-server
    
    # Configure Redis to run on port 6380
    print_status "Configuring Redis for port 6380..."
    sudo sed -i 's/^port 6379/port 6380/' /etc/redis/redis.conf
    sudo sed -i 's/^bind 127.0.0.1 ::1/bind 127.0.0.1/' /etc/redis/redis.conf
    
    # Enable and start Redis
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
    
    print_success "Redis installed and configured on port 6380"
fi

# SQLite is usually pre-installed, but we'll ensure it's there
print_status "Checking SQLite..."
sudo apt install -y sqlite3 libsqlite3-dev
print_success "SQLite installed: $(sqlite3 --version 2>&1 | head -1)"

# Install Docker
print_status "Installing Docker..."
if command_exists docker; then
    print_warning "Docker is already installed: $(docker --version)"
else
    # Add Docker's official GPG key
    sudo apt update
    sudo apt install -y ca-certificates curl
    sudo install -m 0755 -d /etc/apt/keyrings
    sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    sudo chmod a+r /etc/apt/keyrings/docker.asc

    # Add the repository to Apt sources
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Add current user to docker group
    sudo usermod -aG docker $USER
    
    print_success "Docker installed successfully: $(docker --version)"
fi

# Install Docker Compose (standalone)
print_status "Installing Docker Compose..."
if command_exists docker-compose; then
    print_warning "Docker Compose is already installed: $(docker-compose --version)"
else
    # Install Docker Compose standalone
    DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
    sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    
    print_success "Docker Compose installed successfully: $(docker-compose --version)"
fi

# Install AWS CLI v2
print_status "Installing AWS CLI v2..."
if command_exists aws; then
    CURRENT_AWS=$(aws --version)
    print_warning "AWS CLI is already installed: $CURRENT_AWS"
else
    # Download and install AWS CLI v2
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
    unzip -q /tmp/awscliv2.zip -d /tmp
    sudo /tmp/aws/install --update
    rm -rf /tmp/awscliv2.zip /tmp/aws
    
    print_success "AWS CLI installed successfully: $(aws --version 2>/dev/null)"
fi

# Install kubectl
print_status "Installing kubectl..."
if command_exists kubectl; then
    CURRENT_KUBECTL=$(kubectl version --client --short 2>/dev/null | head -1)
    print_warning "kubectl is already installed: $CURRENT_KUBECTL"
else
    # Download latest kubectl
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    
    # Validate kubectl binary (optional but recommended)
    curl -LO "https://dl.k8s.io/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
    echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check
    
    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
    rm kubectl kubectl.sha256
    
    print_success "kubectl installed successfully: $(kubectl version --client --short 2>/dev/null)"
fi

# Install eksctl
print_status "Installing eksctl..."
if command_exists eksctl; then
    CURRENT_EKSCTL=$(eksctl version)
    print_warning "eksctl is already installed: $CURRENT_EKSCTL"
else
    # Download and install eksctl
    curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
    sudo mv /tmp/eksctl /usr/local/bin
    
    print_success "eksctl installed successfully: $(eksctl version)"
fi

# Configure Git (optional - set your own credentials)
print_status "Git Configuration Note:"
echo "You may want to configure Git with your credentials:"
echo "  git config --global user.name 'Your Name'"
echo "  git config --global user.email 'your.email@example.com'"
echo "  git config --global init.defaultBranch main"

# Configure AWS CLI (optional - set your own credentials)
print_status "AWS CLI Configuration Note:"
echo "You may want to configure AWS CLI with your credentials:"
echo "  aws configure"
echo "Or configure named profiles:"
echo "  aws configure --profile your-profile-name"

# Verify installations
print_status "Verifying installations..."

echo ""
print_success "=== Installation Summary ==="
echo "Git: $(git --version 2>/dev/null || echo "Not installed")"
echo "Go (official): $(go version 2>/dev/null || echo "Not installed")"
echo "Go (apt): $(dpkg -l | grep golang-go | awk '{print $3}' 2>/dev/null || echo "Not installed")"
echo "Python: $(python3 --version 2>/dev/null || echo "Not installed")"
echo "Node.js: $(node --version 2>/dev/null || echo "Not installed")"
echo "npm: $(npm --version 2>/dev/null || echo "Not installed")"
echo "Redis: $(redis-server --version 2>/dev/null || echo "Not installed")"
echo "SQLite: $(sqlite3 --version 2>/dev/null || echo "Not installed")"
echo "Docker: $(docker --version 2>/dev/null || echo "Not installed")"
echo "Docker Compose: $(docker-compose --version 2>/dev/null || echo "Not installed")"
echo "AWS CLI: $(aws --version 2>/dev/null || echo "Not installed")"
echo "kubectl: $(kubectl version --client --short 2>/dev/null | head -1 || echo "Not installed")"
echo "eksctl: $(eksctl version 2>/dev/null || echo "Not installed")"

# Display Redis connection info
echo ""
print_success "Redis is configured to run on: localhost:6380"
print_warning "Note: You may need to restart your shell or run 'source ~/.bashrc' for Go to be available in your PATH"
print_warning "Note: You may need to log out and log back in for Docker group permissions to take effect"

# Test Redis connection
print_status "Testing Redis connection..."
if command_exists redis-cli; then
    if redis-cli -p 6380 ping | grep -q "PONG"; then
        print_success "Redis is running successfully on port 6380"
    else
        print_error "Redis is not responding on port 6380"
        print_status "Try starting Redis with: sudo systemctl start redis-server"
    fi
fi

# Test AWS tools
print_status "Testing AWS tools..."
if command_exists aws; then
    print_success "AWS CLI is working: $(aws --version 2>/dev/null)"
else
    print_error "AWS CLI installation failed"
fi

if command_exists kubectl; then
    print_success "kubectl is working: $(kubectl version --client --short 2>/dev/null | head -1)"
else
    print_error "kubectl installation failed"
fi

if command_exists eksctl; then
    print_success "eksctl is working: $(eksctl version 2>/dev/null)"
else
    print_error "eksctl installation failed"
fi

echo ""
print_success "All installations completed!"