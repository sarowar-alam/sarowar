# backend.tf
terraform {
  backend "s3" {
    bucket = "terraform-state-ostad"
    key    = "ecs-cpu-load-test/terraform.tfstate"
    region = "us-east-1"  # Change this to match your bucket's region
    encrypt = true
  }
}