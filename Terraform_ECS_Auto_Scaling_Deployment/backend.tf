terraform {
  backend "s3" {
    bucket = "your-bucket-name"
    key    = "ecs/client/env/your-service-name.tfstate"
    region = "us-west-2"
  }
}