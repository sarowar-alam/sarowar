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
  alarm_actions       = [aws_appautoscaling_policy.scale_out.arn]
  alarm_description   = "Scale out when SQS queue depth exceeds threshold"

  dimensions = {
    QueueName = var.sqs_name
  }

  treat_missing_data = "notBreaching"

  tags = merge(var.common_tags, {
    Name = "sqs-queue-depth-high-${var.ecs_service_name}"
  })

  depends_on = [aws_ecs_service.this]
}

resource "aws_cloudwatch_metric_alarm" "sqs_alarm_low" {
  alarm_name          = "sqs-queue-depth-low-${var.ecs_service_name}"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Average"
  threshold           = var.threshold_num_messages
  actions_enabled     = true
  alarm_actions       = [aws_appautoscaling_policy.scale_in.arn]
  alarm_description   = "Scale in when SQS queue depth is below threshold"

  dimensions = {
    QueueName = var.sqs_name
  }

  treat_missing_data = "notBreaching"

  tags = merge(var.common_tags, {
    Name = "sqs-queue-depth-low-${var.ecs_service_name}"
  })

  depends_on = [aws_ecs_service.this]
}

resource "aws_appautoscaling_policy" "scale_out" {
  name               = "${var.ecs_service_name}-scale-out"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension
  service_namespace  = aws_appautoscaling_target.this.service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 300
    metric_aggregation_type = "Average"
    
    step_adjustment {
      metric_interval_lower_bound = 0
      scaling_adjustment          = 1
    }
  }
}

resource "aws_appautoscaling_policy" "scale_in" {
  name               = "${var.ecs_service_name}-scale-in"
  policy_type        = "StepScaling"
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension
  service_namespace  = aws_appautoscaling_target.this.service_namespace

  step_scaling_policy_configuration {
    adjustment_type         = "ChangeInCapacity"
    cooldown                = 300
    metric_aggregation_type = "Average"
    
    step_adjustment {
      metric_interval_upper_bound = 0
      scaling_adjustment          = -1
    }
  }
}