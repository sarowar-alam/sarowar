output "ecs_service_name" {
  description = "ECS service name"
  value       = module.ecs_service.ecs_service_name
}

output "ecs_service_arn" {
  description = "ECS service ARN"
  value       = module.ecs_service.ecs_service_arn
}

output "ecs_task_definition_arn" {
  description = "ECS task definition ARN"
  value       = module.ecs_service.ecs_task_definition_arn
}

output "cloudwatch_log_group_name" {
  description = "CloudWatch log group name"
  value       = module.ecs_service.cloudwatch_log_group_name
}

output "scale_out_policy_arn" {
  description = "Scale out policy ARN"
  value       = module.ecs_service.scale_out_policy_arn
}

output "scale_in_policy_arn" {
  description = "Scale in policy ARN"
  value       = module.ecs_service.scale_in_policy_arn
}

output "load_balancer_dns_name" {
  description = "Load Balancer DNS name"
  value       = module.ecs_service.load_balancer_dns_name
}

output "target_group_arn" {
  description = "Target group ARN"
  value       = module.ecs_service.target_group_arn
}

output "load_balancer_arn" {
  description = "Load Balancer ARN"
  value       = module.ecs_service.load_balancer_arn
}

output "service_url" {
  description = "Service URL"
  value       = "https://${module.ecs_service.load_balancer_dns_name}"
}
