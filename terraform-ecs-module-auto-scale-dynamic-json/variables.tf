# Application Configuration
variable "ecs_service_name" {
  type        = string
  description = "Name of the ECS service"
}

variable "ecs_cluster_name" {
  type        = string
  description = "Name of the ECS cluster"
}

variable "ecs_cluster_id" {
  type        = string
  description = "ID of the ECS cluster"
}

# Resource Sizing
variable "cpu_size" {
  type        = string
  description = "CPU units for the ECS task"
}

variable "memory_size" {
  type        = string
  description = "Memory for the ECS task"
}

variable "ephemeral_size" {
  type        = number
  description = "Ephemeral storage size in GiB"
  default     = 21
}

# Container Configuration
variable "image_uri" {
  type        = string
  description = "ECR image URI for the container"
}

variable "env_file" {
  type        = string
  description = "S3 URI for environment file"
}

# Networking
variable "sg_name" {
  type        = string
  description = "Security group ID for ECS service"
}

variable "subnet_names" {
  type        = string
  description = "Comma-separated list of subnet IDs"
}

# IAM
variable "iam_task_role_arn" {
  type        = string
  description = "IAM task role ARN"
}

# Auto Scaling
variable "max_number_instances" {
  type        = number
  description = "Maximum number of ECS tasks"
  default     = 5
}

variable "min_number_instances" {
  type        = number
  description = "Minimum number of ECS tasks"
  default     = 1
}

variable "threshold_num_messages" {
  type        = number
  description = "SQS queue depth threshold for scaling"
}

# SQS
variable "sqs_name" {
  type        = string
  description = "SQS queue name"
}

# Environment & Region
variable "region" {
  type        = string
  description = "AWS region"
  default     = "us-west-2"
}

# Tags
variable "tag_environment" {
  type        = string
  description = "Environment tag (dev, staging, prod)"
}

variable "tag_system" {
  type        = string
  description = "System tag"
}

variable "tag_owner" {
  type        = string
  description = "Owner tag"
}

variable "tag_company" {
  type        = string
  description = "Company tag"
}

variable "cost_app" {
  type        = string
  description = "Cost application tag"
}