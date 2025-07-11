resource "aws_cloudwatch_metric_alarm" "sqs_alarm_high" {
  alarm_name          = "sqs-queue-depth-high-${var.ecs_service_name}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Average"
  threshold           = var.threshold_num_messages
  actions_enabled     = true
  alarm_actions       = [aws_appautoscaling_policy.ecs_service_scaling_OUT_policy.arn]
  alarm_description   = "sqs-queue-depth-high-${var.ecs_service_name}"
  dimensions = {
    QueueName = "${var.sqs_name}"
  }
  treat_missing_data  = "notBreaching"
  tags = {
    Name        = "sqs-queue-depth-high-${var.ecs_service_name}"
    Environment = "${var.tag_environment}"
    System      = "${var.tag_system}"
    Owner       = "${var.tag_owner}"
    CostApp     = "${var.cost_app}"
    CostUnit    = "${var.tag_company}"
    Client      = "${var.tag_company}"
  }

  depends_on = [
    aws_ecs_service.ecs_service
  ]
}

resource "aws_appautoscaling_policy" "ecs_service_scaling_OUT_policy" {
  name               = "ScaleOutPolicy"
  policy_type        = "StepScaling"
  resource_id        = "service/${var.ecs_cluster_name}/${aws_ecs_service.ecs_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 300
    metric_aggregation_type = "Average"
    step_adjustment {
      metric_interval_lower_bound = 0
      scaling_adjustment          = 1
    }
  }
  depends_on = [aws_appautoscaling_target.ecs_service_register]
}

