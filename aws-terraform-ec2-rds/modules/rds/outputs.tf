
output "endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "RDS address"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "RDS port"
  value       = aws_db_instance.this.port
}

output "username" {
  description = "RDS master username"
  value       = aws_db_instance.this.username
  sensitive   = true
}