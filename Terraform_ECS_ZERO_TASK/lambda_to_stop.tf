
# Lambda Function
resource "aws_lambda_function" "stop_ecs" {
  filename         = "${path.module}/stop_ecs.zip"
  function_name    = "${var.ecs_Name}-or-ecs-STOP"
  role             = "${var.iam_role_arn}"
  handler          = "stop_ecs.lambda_handler"
  runtime          = "python3.12"
  source_code_hash = filebase64sha256("${path.module}/stop_ecs.zip")
  timeout = 30

  environment {
    variables = {
      queue_url     = var.sqs_url    
      service_name  = var.ecs_Name
      cluster_name  = var.cluster_name 
    }
  }

  tags = {
  Client = var.tag_company
  CostUnit = var.tag_company
  Name = "${var.ecs_Name}-or-ecs-STOP"
  CostApp = var.cost_app
  Environment = var.tag_environment
  Owner = var.tag_owner
  System = var.tag_system
  }

}

# Output the Lambda Function ARN
output "lambda_function_stop_arn" {
  value = aws_lambda_function.stop_ecs.arn
}
