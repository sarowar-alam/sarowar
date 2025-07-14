terraform {
    required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.53.0"
    }
  }
}

provider "aws" {
  region = var.region
}

locals {
  queue_name = regex("([^/]+)$", var.sqs_url)[0]
}

data "aws_sqs_queue" "my_queue" {
  name = local.queue_name
}

data "aws_ecs_cluster" "my_cluster" {
  cluster_name = var.cluster_name
}


data "aws_ecs_service" "my_service" {
  cluster_arn = data.aws_ecs_cluster.my_cluster.arn
  service_name = var.ecs_Name
}

output "sqs_queue_arn" {
  value = data.aws_sqs_queue.my_queue.arn
}

output "ecs_service_arn" {
  value = data.aws_ecs_service.my_service.arn
}
