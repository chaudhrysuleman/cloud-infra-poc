output "ec2_public_ip" {
  value       = aws_instance.web.public_ip
  description = "Public IP address of the EC2 instance"
}

output "ec2_url" {
  value       = "http://${aws_instance.web.public_ip}"
  description = "Web address of the running Spring Boot service"
}

output "rds_endpoint" {
  value       = aws_db_instance.postgres.endpoint
  description = "Database endpoint connection string"
}
