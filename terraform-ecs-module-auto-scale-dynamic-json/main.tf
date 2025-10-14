terraform {
  required_version = "~> 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.53.0"
    }
  }

  backend "s3" {
    bucket = "your-terraform-state-bucket-name"
    region = "us-west-2"
    # terraform init -backend-config=backend.hcl
  }
}

provider "aws" {
  region = var.region
  
  default_tags {
    tags = {
      Environment = var.tag_environment
      System      = var.tag_system
      Owner       = var.tag_owner
      CostApp     = var.cost_app
      CostUnit    = var.tag_company
      Client      = var.tag_company
      Terraform   = "true"
    }
  }
}

module "ecs_service" {
  source = "./modules/ecs-service"

  # ECS Service Configuration
  ecs_service_name    = var.ecs_service_name
  ecs_cluster_name    = var.ecs_cluster_name
  ecs_cluster_id      = var.ecs_cluster_id
  cpu_size            = var.cpu_size
  memory_size         = var.memory_size
  ephemeral_size      = var.ephemeral_size
  image_uri           = var.image_uri
  env_file            = var.env_file
  
  # Networking
  sg_name      = var.sg_name
  subnet_names = var.subnet_names
  
  # IAM
  iam_task_role_arn = var.iam_task_role_arn
  
  # Auto Scaling
  max_number_instances    = var.max_number_instances
  min_number_instances    = var.min_number_instances
  threshold_num_messages  = var.threshold_num_messages
  
  # SQS
  sqs_name = var.sqs_name
  
  # Common Tags
  common_tags = local.common_tags
}

locals {
  common_tags = {
    Environment = var.tag_environment
    System      = var.tag_system
    Owner       = var.tag_owner
    CostApp     = var.cost_app
    CostUnit    = var.tag_company
    Client      = var.tag_company
  }
}