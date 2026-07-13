# --- Fetch latest Ubuntu 22.04 AMI ---
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical (Ubuntu)

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# --- SSH Key Pair (Creates a key pair for SSH access, using a local public key if it exists) ---
# For PoC simplicity, we define a key pair. You can generate a key locally via ssh-keygen
resource "aws_key_pair" "poc_key" {
  key_name   = "poc-ssh-key"
  public_key = fileexists("~/.ssh/id_ed25519_github.pub") ? file("~/.ssh/id_ed25519_github.pub") : (fileexists("~/.ssh/id_rsa.pub") ? file("~/.ssh/id_rsa.pub") : file("~/.ssh/gitlab_ed25519.pub"))
}

# --- EC2 Instance ---
resource "aws_instance" "web" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = "t3.micro" # Free tier eligible in eu-north-1
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.ec2.id]
  associate_public_ip_address = true
  key_name                    = aws_key_pair.poc_key.key_name
  iam_instance_profile        = aws_iam_instance_profile.ec2.name

  # Boot-time script to install Docker, Docker Compose, and start the application
  user_data = <<-EOF
              #!/bin/bash
              # Update packages
              apt-get update -y
              apt-get install -y apt-transport-https ca-certificates curl software-properties-common

              # Install Docker
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
              add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
              apt-get update -y
              apt-get install -y docker-ce

              # Start and enable Docker service
              systemctl start docker
              systemctl enable docker

              # Wait for RDS database to be ready (optional check, RDS is managed by TF dependency)
              sleep 10

              # Run the Docker container
              # We map host port 80 to container port 8080.
              # Adjust image name to your Docker Hub repository (e.g., chaudhrysuleman/cloud-infra-poc:latest)
              docker run -d \
                --name spring-boot-app \
                -p 80:8080 \
                -e DB_HOST="${aws_db_instance.postgres.address}" \
                -e DB_PORT=5432 \
                -e DB_NAME="${var.db_name}" \
                -e DB_USER="${var.db_username}" \
                -e DB_PASSWORD="${var.db_password}" \
                --restart always \
                nginx:alpine # Using a placeholder web server. You will replace this with your spring boot image once pushed.
              EOF

  # Ensure RDS is created before booting the EC2 to prevent connection failures
  depends_on = [aws_db_instance.postgres]

  tags = {
    Name = "poc-ec2-instance"
  }
}
