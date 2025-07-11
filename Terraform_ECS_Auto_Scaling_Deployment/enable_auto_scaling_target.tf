resource "aws_appautoscaling_target" "ecs_service_register" {
  max_capacity       = var.max_number_Instances
  min_capacity       = var.min_number_Instances
  resource_id        = "service/${var.ecs_cluster_name}/${aws_ecs_service.ecs_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"

  depends_on = [
    aws_ecs_service.ecs_service
  ]
}