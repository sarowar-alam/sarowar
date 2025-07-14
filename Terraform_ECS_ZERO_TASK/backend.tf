terraform {
  backend "s3" {    
    bucket = "your-tf-state-bucket-name"
    workspace_key_prefix = "your-workspace-name"
    key = "env/ecs-zero.tfstate"
    region = "us-west-2"       
  }
}
