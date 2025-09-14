resource "aws_instance" "this" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = var.security_group_ids
  key_name               = var.key_name
  user_data              = var.user_data
  
  # Explicitly disable public IP assignment for private subnet
  associate_public_ip_address = var.associate_public_ip

  tags = {
    Name        = "${var.environment}-${var.name}"
    Environment = var.environment
  }

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
    encrypted   = true
  }
}
