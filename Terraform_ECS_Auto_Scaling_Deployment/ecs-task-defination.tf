resource "aws_ecs_task_definition" "ecs_task_def" {
  family = "${var.ecs_service_name}"
  requires_compatibilities = ["FARGATE"]
  task_role_arn = "${var.iam_task_role_arn}"
  execution_role_arn = "arn:aws:iam::123456789123:role/ecsTaskExecutionRole"
  network_mode = "awsvpc"
  cpu = "${var.cpuSize}"
  memory = "${var.memorySize}"
  container_definitions    = jsonencode(local.task_definition)
     ephemeral_storage {
      size_in_gib = var.ephemeral_size
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
}
locals {
  task_definition = [
    {
      name  = var.ecs_service_name
      image = var.image_uri
      cpu   = 0
      environmentFiles = [
        {
          value = var.env_file
          type  = "s3"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_logs.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ]
}