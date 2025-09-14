variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "allowed_ips" {
  description = "List of allowed IPs for bastion"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "List of public subnet CIDRs"
  type        = list(string)
}

variable "environment" {
  description = "Environment name"
  type        = string
}