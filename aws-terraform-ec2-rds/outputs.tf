output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "bastion_public_ip" {
  description = "Bastion host public IP"
  value       = module.bastion_host.public_ip
}

output "ubuntu_private_ip" {  
  description = "Ubuntu Server private IP" 
  value       = module.ubuntu_server.private_ip  
}

output "windows_private_ip" {
  description = "Windows Server private IP"
  value       = module.windows_server.private_ip
}