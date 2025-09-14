module "vpc" {
  source = "./modules/vpc"

  vpc_cidr            = var.vpc_cidr
  public_subnet_cidrs = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  environment         = var.environment

}

module "security_groups" {
  source = "./modules/security-groups"

  vpc_id              = module.vpc.vpc_id
  allowed_ips         = var.allowed_ips
  public_subnet_cidrs = var.public_subnet_cidrs
  environment         = var.environment
}

module "bastion_host" {
  source = "./modules/ec2"

  name                = "bastion-host"
  instance_type       = var.bastion_instance_type
  subnet_id           = module.vpc.public_subnet_ids[0]
  security_group_ids  = [module.security_groups.bastion_sg_id]
  key_name            = var.key_name
  associate_public_ip = true
  user_data           = filebase64("${path.module}/scripts/bastion-userdata.ps1")
  ami_id              = "ami-07247b44a617713f0"  # Windows Server 2019 in ap-south-1
  environment         = var.environment
}

module "ubuntu_server" {
  source = "./modules/ec2"

  name                = "ubuntu-server"
  instance_type       = var.ubuntu_instance_type
  subnet_id           = module.vpc.private_subnet_ids[0]
  security_group_ids  = [module.security_groups.private_sg_id]
  key_name            = var.key_name
  associate_public_ip = false
  user_data           = filebase64("${path.module}/scripts/ubuntu-userdata.sh")
  ami_id              = "ami-07f07a6e1060cd2a8"  # Ubuntu 22.04 LTS in ap-south-1
  environment         = var.environment
}

module "windows_server" {
  source = "./modules/ec2"

  name                = "windows-server"
  instance_type       = var.windows_instance_type
  subnet_id           = module.vpc.private_subnet_ids[1]
  security_group_ids  = [module.security_groups.private_sg_id]
  key_name            = var.key_name
  associate_public_ip = false
  user_data           = filebase64("${path.module}/scripts/windows-userdata.ps1")
  ami_id              = "ami-07247b44a617713f0"  # Windows Server 2019 in ap-south-1
  environment         = var.environment
}

module "mariadb_rds" {
  source = "./modules/rds"

  name               = "mariadb"
  engine             = "mariadb"
  engine_version     = "10.6"
  instance_class     = "db.t3.micro"
  subnet_ids         = module.vpc.private_subnet_ids
  security_group_ids = [module.security_groups.rds_sg_id]
  username           = var.db_username
  password           = var.db_password
  database_name      = var.mariadb_database_name
  environment        = var.environment
  license_model      = "general-public-license"
}

module "sqlserver_rds" {
  source = "./modules/rds"

  name               = "sqlserver"
  engine             = "sqlserver-ex"
  engine_version     = "15.00"
  instance_class     = "db.t3.small"
  subnet_ids         = module.vpc.private_subnet_ids
  security_group_ids = [module.security_groups.rds_sg_id]
  username           = var.db_username
  password           = var.db_password
  database_name      = var.sqlserver_database_name
  environment        = var.environment
  license_model      = "license-included"
}