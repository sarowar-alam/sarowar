resource "aws_db_subnet_group" "this" {
  name       = "${var.environment}-${var.name}-subnet-group"
  subnet_ids = var.subnet_ids
  depends_on = [var.subnet_ids]

  tags = {
    Name        = "${var.environment}-${var.name}-subnet-group"
    Environment = var.environment
  }
}

resource "aws_db_instance" "this" {
  identifier              = "${var.environment}-${var.name}"
  engine                  = var.engine
  engine_version          = var.engine_version
  instance_class          = var.instance_class
  allocated_storage       = 20
  max_allocated_storage   = 100
  storage_type            = "gp3"
  storage_encrypted       = true
  username                = var.username
  password                = var.password
  db_name                 = var.engine == "sqlserver-ex" ? null : var.database_name
  db_subnet_group_name    = aws_db_subnet_group.this.name
  vpc_security_group_ids  = var.security_group_ids
  multi_az                = false
  backup_retention_period = 7
  skip_final_snapshot     = true
  publicly_accessible     = false
  license_model           = var.license_model

  tags = {
    Name        = "${var.environment}-${var.name}"
    Environment = var.environment
  }
}