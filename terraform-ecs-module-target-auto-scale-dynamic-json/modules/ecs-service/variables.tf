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

variable "image_uri" {
  type        = string
  description = "ECR image URI for the container"
}

variable "env_file" {
  type        = string
  description = "S3 URI for environment file"
  default     = ""
}

variable "iam_task_role_arn" {
  type        = string
  description = "IAM task role ARN"
}

variable "max_number_instances" {
  type        = number
  description = "Maximum number of ECS tasks"
  default     = 10
}

variable "min_number_instances" {
  type        = number
  description = "Minimum number of ECS tasks"
  default     = 2
}

variable "threshold_num_messages" {
  type        = number
  description = "SQS queue depth threshold for scaling"
  default     = 100
}

variable "sqs_name" {
  type        = string
  description = "SQS queue name"
  default     = ""
}

variable "region" {
  type        = string
  description = "AWS region"
  default     = "us-west-2"
}

variable "common_tags" {
  type        = map(string)
  description = "Common tags for all resources"
  default     = {}
}

variable "container_port" {
  type        = number
  description = "Container port to expose"
  default     = 80
}

variable "health_check_path" {
  type        = string
  description = "Health check path for the application"
  default     = "/"
}

variable "health_check_grace_period" {
  type        = number
  description = "Health check grace period in seconds"
  default     = 60
}

variable "vpc_id" {
  type        = string
  description = "VPC ID"
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "List of public subnet IDs for ALB"
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "List of private subnet IDs for ECS tasks"
}

variable "acm_certificate_arn" {
  type        = string
  description = "ACM certificate ARN for HTTPS"
}

variable "ecs_task_execution_role_arn" {
  type        = string
  description = "ECS task execution role ARN"
}
