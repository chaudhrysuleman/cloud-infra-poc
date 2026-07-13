# --- EC2 Instance Security Group ---
resource "aws_security_group" "ec2" {
  name        = "poc-ec2-sg"
  description = "Allow HTTP and SSH access to EC2 app server"
  vpc_id      = aws_vpc.main.id

  # Allow inbound HTTP from anywhere
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow inbound Spring Boot port 8080 directly (optional but useful for PoC testing)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow inbound SSH restricted by variable (default anywhere for testing, but can lock down)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip]
  }

  # Allow all outbound traffic (so EC2 can install packages, download Docker images)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "poc-ec2-sg"
  }
}

# --- RDS Security Group ---
resource "aws_security_group" "rds" {
  name        = "poc-rds-sg"
  description = "Allow PostgreSQL database access from EC2 instance only"
  vpc_id      = aws_vpc.main.id

  # Allow PostgreSQL traffic from the EC2 security group ONLY
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  # Allow all outbound traffic (default, but can lock down if required)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "poc-rds-sg"
  }
}
