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
}

variable "image_uri" {
  type        = string
  description = "ECR image URI for the container"
}

variable "env_file" {
  type        = string
  description = "S3 URI for environment file"
}

variable "sg_name" {
  type        = string
  description = "Security group ID for ECS service"
}

variable "subnet_names" {
  type        = string
  description = "Comma-separated list of subnet IDs"
}

variable "iam_task_role_arn" {
  type        = string
  description = "IAM task role ARN"
}

variable "max_number_instances" {
  type        = number
  description = "Maximum number of ECS tasks"
}

variable "min_number_instances" {
  type        = number
  description = "Minimum number of ECS tasks"
}

variable "threshold_num_messages" {
  type        = number
  description = "SQS queue depth threshold for scaling"
}

variable "sqs_name" {
  type        = string
  description = "SQS queue name"
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