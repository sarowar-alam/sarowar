terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  profile = var.aws_profile
  region  = var.region
  default_tags {
    tags = {
      Environment = var.environment
      Project     = "aws-infrastructure"
      ManagedBy   = "terraform"
    }
  }
}

provider "tls" {}
provider "local" {}