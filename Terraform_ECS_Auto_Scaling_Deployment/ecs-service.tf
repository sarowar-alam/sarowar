
resource "aws_ecs_service" "ecs_service" {

  name = var.ecs_service_name

  cluster                            = var.ecs_cluster_id
  desired_count                      = 1
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = 0

  network_configuration {
    security_groups = ["${var.sg_name}"]
    subnets         = split(",", var.subnet_names)
  }

  platform_version = "LATEST"

  enable_execute_command = true
  scheduling_strategy    = "REPLICA"

  task_definition = aws_ecs_task_definition.ecs_task_def.arn
  capacity_provider_strategy {
    base              = 0
    capacity_provider = "FARGATE"
    weight            = 1
  }

  deployment_circuit_breaker {
    enable   = false
    rollback = false
  }

  deployment_controller {
    type = "ECS"
  }

  tags = {
    Name        = "${var.ecs_service_name}"
    Environment = "${var.tag_environment}"
    System      = "${var.tag_system}"
    Owner       = "${var.tag_owner}"
    CostApp     = "${var.cost_app}"
    CostUnit    = "${var.tag_company}"
    Client      = "${var.tag_company}"
  }
  enable_ecs_managed_tags = true
  propagate_tags = "SERVICE"
  depends_on = [aws_ecs_task_definition.ecs_task_def]
}