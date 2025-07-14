resource "aws_cloudwatch_metric_alarm" "ecs_cpu_alarm" {
  alarm_name          = "${var.ecs_Name}-ecs-alarm-to-STOP"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 30
  statistic           = "Average"
  threshold           = 0
  alarm_description   = "Alarm when the number of visible messages in Service_Production SQS queue is less than or equal to 0"
  dimensions = {
    QueueName = local.queue_name
  }

  treat_missing_data  = "notBreaching"

  alarm_actions = [
    aws_lambda_function.stop_ecs.arn
  ]

  depends_on = [
    aws_lambda_function.stop_ecs
  ]

  tags = {
    Client = var.tag_company
    CostUnit = var.tag_company
    Name = "${var.ecs_Name}-ecs-alarm-to-STOP"
    CostApp = var.cost_app
    Environment = var.tag_environment
    Owner = var.tag_owner
    System = var.tag_system
  }

}

# Lambda Permission
resource "aws_lambda_permission" "allow_cloudwatch_to_invoke" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.stop_ecs.function_name
  principal     = "lambda.alarms.cloudwatch.amazonaws.com"
  source_arn    = aws_cloudwatch_metric_alarm.ecs_cpu_alarm.arn
}

output "cloud_watch_alarm_arn" {
  value = aws_cloudwatch_metric_alarm.ecs_cpu_alarm.arn
}
