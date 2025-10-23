resource "aws_ecs_service" "this" {
  name            = var.ecs_service_name
  cluster         = var.ecs_cluster_id
  task_definition = aws_ecs_task_definition.this.arn

  desired_count                      = var.min_number_instances
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds  = var.health_check_grace_period

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = var.ecs_service_name
    container_port   = var.container_port
  }

  network_configuration {
    security_groups  = [aws_security_group.ecs_service.id]
    subnets          = var.private_subnet_ids
    assign_public_ip = false
  }

  platform_version = "LATEST"

  enable_execute_command = true
  scheduling_strategy    = "REPLICA"

  capacity_provider_strategy {
    base              = 0
    capacity_provider = "FARGATE_SPOT"
    weight            = 1
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_controller {
    type = "ECS"
  }

  enable_ecs_managed_tags = true
  propagate_tags          = "SERVICE"

  tags = merge(var.common_tags, {
    Name = var.ecs_service_name
  })

  depends_on = [aws_lb_listener.https, aws_ecs_task_definition.this]
}

resource "aws_ecs_task_definition" "this" {
  family                   = var.ecs_service_name
  requires_compatibilities = ["FARGATE"]
  task_role_arn            = var.iam_task_role_arn
  execution_role_arn       = var.ecs_task_execution_role_arn
  network_mode             = "awsvpc"
  cpu                      = var.cpu_size
  memory                   = var.memory_size

  container_definitions = jsonencode([{
    name   = var.ecs_service_name
    image  = var.image_uri
    cpu    = tonumber(var.cpu_size)
    memory = tonumber(var.memory_size)
    portMappings = [{
      containerPort = var.container_port
      hostPort      = var.container_port
      protocol      = "tcp"
    }]
    environmentFiles = var.env_file != "" ? [{
      value = var.env_file
      type  = "s3"
    }] : []
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.this.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.container_port}${var.health_check_path} || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    essential = true
  }])

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  ephemeral_storage {
    size_in_gib = var.ephemeral_size
  }

  tags = merge(var.common_tags, {
    Name = var.ecs_service_name
  })
}

# Load Balancer Resources
resource "aws_lb" "this" {
  name               = "${var.ecs_service_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-alb"
  })
}

resource "aws_lb_target_group" "this" {
  name        = "${var.ecs_service_name}-tg"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
    path                = var.health_check_path
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "200-299"
  }

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-tg"
  })

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-https"
  })
}

resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-http-redirect"
  })
}

# Security Groups
resource "aws_security_group" "alb" {
  name        = "${var.ecs_service_name}-alb-sg"
  description = "Security group for ALB"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-alb-sg"
  })
}

resource "aws_security_group" "ecs_service" {
  name        = "${var.ecs_service_name}-ecs-sg"
  description = "Security group for ECS service"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.common_tags, {
    Name = "${var.ecs_service_name}-ecs-sg"
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
