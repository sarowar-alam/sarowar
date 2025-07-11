
variable "env" {}
variable "cpuSize" {
  type = string
}
variable "memorySize" {
  type = string
}
variable "region" {
  type = string
}
variable "build" {
  type = string
}
variable "image_uri" {
  type = string
}
variable "env_file" {
  type = string
}
variable "sqs_name" {
  type = string
}
variable "ecs_cluster_name" {
  type = string
}
variable "ecs_cluster_id" {
  type = string
}
variable "ecs_service_name" {
  type = string
}
variable "sg_name" {
  type = string
}
variable "subnet_names" {
  type = string
}
variable "tag_company" {
  type = string
}
variable "tag_owner" {
  type = string
}
variable "tag_system" {
  type = string
}
variable "tag_environment" {
  type = string
}
variable "cost_app" {
  type = string
}
variable "iam_task_role_arn" {
  type = string
}
variable "max_number_Instances" {
  type = number
}
variable "min_number_Instances" {
  type = number
}
variable "threshold_num_messages" {
  type = number
}
variable "ephemeral_size" {
    type = number
}
