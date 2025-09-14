terraform {
  backend "s3" {
    bucket   = "terraform-state-ostad"
    key      = "batch-04/terraform-class.tfstate"
    region   = "us-east-1"
    encrypt  = true
    profile  = "sarowar-ostad"  # Added profile for consistency
  }
}