terraform {
  backend "s3" {
    bucket = "terraform-state-ostad"
    key    = "ecs-cpu-load-test/terraform.tfstate"
    region = "ap-south-1"
  }
}
