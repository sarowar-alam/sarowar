resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/${var.ecs_service_name}"
  retention_in_days = 30

  tags = {
    Name        = "/ecs/${var.ecs_service_name}"
    Environment = "${var.tag_environment}"
    System      = "${var.tag_system}"
    Owner       = "${var.tag_owner}"
    CostApp     = "${var.cost_app}"
    CostUnit    = "${var.tag_company}"
    Client      = "${var.tag_company}"
  }


}