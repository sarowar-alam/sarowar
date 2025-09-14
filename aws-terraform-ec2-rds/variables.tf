variable "aws_profile" {
  description = "AWS named profile to use"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
}

variable "common_tags" {
  description = "Common tags for all resources"
  type        = map(string)
  default = {
    Project   = "aws-infrastructure"
    ManagedBy = "terraform"
  }
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDR blocks"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "Private subnet CIDR blocks"
  type        = list(string)
}

variable "allowed_ips" {
  description = "List of IPs allowed to access bastion host"
  type        = list(string)
}

variable "bastion_instance_type" {
  description = "Bastion host instance type"
  type        = string
}

variable "key_name" {
  description = "Name of the key pair"
  type        = string
}

variable "windows_instance_type" {
  description = "Windows Server instance type"
  type        = string
}

variable "ubuntu_instance_type" {
  description = "Ubuntu Server instance type"
  type        = string
  default     = "t3.micro"  # Add a default value
}

variable "db_username" {
  description = "Database master username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}

variable "mariadb_database_name" {
  description = "MariaDB database name"
  type        = string
}

variable "sqlserver_database_name" {
  description = "SQL Server database name"
  type        = string
}
