output "instance_id" {
  description = "Instance ID"
  value       = aws_instance.this.id
}

output "public_ip" {
  description = "Public IP address"
  value       = aws_instance.this.public_ip
}

output "private_ip" {
  description = "Private IP address"
  value       = aws_instance.this.private_ip
}

output "key_name" {
  description = "Key pair name"
  value       = var.key_name
}
