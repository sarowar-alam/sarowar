output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.this.name
}

output "ecs_service_arn" {
  description = "ECS service ARN"
  value       = aws_ecs_service.this.id
}

output "ecs_task_definition_arn" {
  description = "ECS task definition ARN"
  value       = aws_ecs_task_definition.this.arn
}

output "cloudwatch_log_group_name" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.this.name
}

output "scale_out_policy_arn" {
  description = "Scale out policy ARN"
  value       = aws_appautoscaling_policy.scale_out.arn
}

output "scale_in_policy_arn" {
  description = "Scale in policy ARN"
  value       = aws_appautoscaling_policy.scale_in.arn
}

output "load_balancer_dns_name" {
  description = "Load Balancer DNS name"
  value       = aws_lb.this.dns_name
}

output "target_group_arn" {
  description = "Target group ARN"
  value       = aws_lb_target_group.this.arn
}

output "load_balancer_arn" {
  description = "Load Balancer ARN"
  value       = aws_lb.this.arn
}
