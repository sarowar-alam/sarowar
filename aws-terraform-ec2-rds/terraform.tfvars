aws_profile  = "your-profile"
region       = "ap-south-1"
environment  = "prod"

vpc_cidr     = "10.0.0.0/16"
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.3.0/24", "10.0.4.0/24"]

allowed_ips  = ["your-real-ip/32"]

bastion_instance_type  = "t3.medium"
ubuntu_instance_type    = "t3.medium"
windows_instance_type  = "t3.medium"
key_name = "your-key-name"

db_username = "admin"
db_password = "ChangeThisPassword123!"

mariadb_database_name = "mariadbdatabase"
sqlserver_database_name = "sqlserverdatabase"