resource "aws_ecs_service" "this" {
  name            = var.ecs_service_name
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.this.arn

  desired_count                      = var.min_number_instances
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = 0

  network_configuration {
    security_groups = [var.sg_name]
    subnets         = split(",", var.subnet_names)
  }

  platform_version = "LATEST"
  #launch_type      = "FARGATE"

  enable_execute_command = true
  scheduling_strategy    = "REPLICA"

  capacity_provider_strategy {
    base              = 0
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
  }

  deployment_circuit_breaker {
    enable   = false
    rollback = false
  }

  deployment_controller {
    type = "ECS"
  }

  enable_ecs_managed_tags = true
  propagate_tags          = "SERVICE"

  tags = merge(var.common_tags, {
    Name = var.ecs_service_name
  })

  depends_on = [aws_ecs_task_definition.this]
}

resource "aws_ecs_task_definition" "this" {
  family                   = var.ecs_service_name
  requires_compatibilities = ["FARGATE"]
  task_role_arn            = var.iam_task_role_arn
  execution_role_arn       = "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskExecutionRole"
  network_mode             = "awsvpc"
  cpu                      = var.cpu_size
  memory                   = var.memory_size

  container_definitions = jsonencode([{
    name  = var.ecs_service_name
    image = var.image_uri
    cpu   = 0
    environmentFiles = [{
      value = var.env_file
      type  = "s3"
    }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.this.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  ephemeral_storage {
    size_in_gib = var.ephemeral_size
  }

  tags = merge(var.common_tags, {
    Name = var.ecs_service_name
  })
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.ecs_service_name}"
  retention_in_days = 30

  tags = merge(var.common_tags, {
    Name = "/ecs/${var.ecs_service_name}"
  })
}

resource "aws_appautoscaling_target" "this" {
  max_capacity       = var.max_number_instances
  min_capacity       = var.min_number_instances
  resource_id        = "service/${var.ecs_cluster_name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"

  depends_on = [aws_ecs_service.this]
}