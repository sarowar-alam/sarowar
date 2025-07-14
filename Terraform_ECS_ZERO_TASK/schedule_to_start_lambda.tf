resource "aws_scheduler_schedule" "example" {
  name       = "${var.ecs_Name}-schedule-start"
  group_name = "default"

  schedule_expression = "rate(1 minute)"
  flexible_time_window {
    mode = "OFF"
  }
  schedule_expression_timezone = "Asia/Dhaka" # UTC+6

  target {
    arn      = aws_lambda_function.state_ecs.arn # Reference from another module
    role_arn = var.iam_role_arn                      # Use the provided role ARN

    input = jsonencode({}) # Empty JSON payload

    retry_policy {
      maximum_event_age_in_seconds = 86400  # Default 24 hours
      maximum_retry_attempts       = 0     # No retries
    }
  }

  state = "ENABLED"
}

resource "aws_lambda_permission" "eventbridge_invoke" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.state_ecs.function_name
  principal     = "scheduler.amazonaws.com"
}