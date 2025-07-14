
# Lambda Function
resource "aws_lambda_function" "state_ecs" {
  filename         = "${path.module}/start_ecs.zip"
  function_name    = "${var.ecs_Name}-or-ecs-START"
  role             = var.iam_role_arn
  handler          = "start_ecs.lambda_handler"
  runtime          = "python3.12"
  source_code_hash = filebase64sha256("${path.module}/start_ecs.zip")

  timeout = 60
  environment {
    variables = {
      queue_url     = var.sqs_url     # Replace with your actual queue URL
      service_name  = var.ecs_Name  # Replace with your actual service name
      cluster_name  = var.cluster_name  # Replace with your actual cluster name
    }
  }

  tags = {
  Client = var.tag_company
  CostUnit = var.tag_company
  Name = "${var.ecs_Name}-or-ecs-START"
  CostApp = var.cost_app
  Environment = var.tag_environment
  Owner = var.tag_owner
  System = var.tag_system
  }

}

# Output the Lambda Function ARN
output "lambda_function_arn" {
  value = aws_lambda_function.state_ecs.arn
}
