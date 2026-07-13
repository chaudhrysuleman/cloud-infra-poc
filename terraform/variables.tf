variable "aws_region" {
  type        = string
  description = "AWS region for deployment"
  default     = "eu-north-1" # Stockholm
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR block for the custom VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type        = string
  description = "CIDR block for the public subnet (EC2 host)"
  default     = "10.0.1.0/24"
}

variable "private_subnet_1_cidr" {
  type        = string
  description = "CIDR block for private subnet 1 (RDS DB)"
  default     = "10.0.2.0/24"
}

variable "private_subnet_2_cidr" {
  type        = string
  description = "CIDR block for private subnet 2 (RDS DB - multi-AZ requirement)"
  default     = "10.0.3.0/24"
}

variable "db_name" {
  type        = string
  description = "Name of the PostgreSQL database"
  default     = "postgres"
}

variable "db_username" {
  type        = string
  description = "Username for the RDS database administrator"
  default     = "dbadmin"
}

variable "db_password" {
  type        = string
  description = "Password for the RDS database administrator"
  sensitive   = true
  default     = "SecurePass123!"
}

variable "my_ip" {
  type        = string
  description = "Your public IP CIDR to restrict SSH access (default allows all, but override for safety)"
  default     = "0.0.0.0/0"
}
