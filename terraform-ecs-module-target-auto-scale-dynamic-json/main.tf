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
    key    = "ecs/your-environment-name/rc/${var.ecs_service_name}.tfstate"
    region = "us-west-2"
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
  ecs_service_name = var.ecs_service_name
  ecs_cluster_name = var.ecs_cluster_name
  ecs_cluster_id   = var.ecs_cluster_id
  cpu_size         = var.cpu_size
  memory_size      = var.memory_size
  ephemeral_size   = var.ephemeral_size
  image_uri        = var.image_uri
  env_file         = var.env_file

  # Container Configuration
  container_port            = var.container_port
  health_check_path         = var.health_check_path
  health_check_grace_period = var.health_check_grace_period

  # IAM
  iam_task_role_arn           = var.iam_task_role_arn
  ecs_task_execution_role_arn = var.ecs_task_execution_role_arn

  # Auto Scaling
  max_number_instances   = var.max_number_instances
  min_number_instances   = var.min_number_instances
  threshold_num_messages = var.threshold_num_messages

  # SQS
  sqs_name = var.sqs_name

  # Networking
  vpc_id              = var.vpc_id
  public_subnet_ids   = var.public_subnet_ids
  private_subnet_ids  = var.private_subnet_ids
  acm_certificate_arn = var.acm_certificate_arn

  # Region
  region = var.region

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
    Service     = var.ecs_service_name
  }
}
