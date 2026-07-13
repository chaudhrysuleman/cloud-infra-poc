#!/bin/bash
set -e

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '#' | xargs)
else
    echo "ERROR: Local .env file not found. Copy .env.example to .env and fill in the values."
    exit 1
fi

# Validate required variables
if [ -z "$EC2_IP" ] || [ -z "$RDS_HOST" ] || [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: Environment variables EC2_IP, RDS_HOST, and DB_PASSWORD must be defined in your .env file."
    exit 1
fi

# Query AWS Account ID using CLI to dynamically construct ARNs and Buckets
echo "Querying AWS Account ID..."
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)
if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo "ERROR: Could not query AWS Account ID. Ensure 'aws configure' is complete."
    exit 1
fi

SNS_TOPIC_ARN="arn:aws:sns:eu-north-1:$AWS_ACCOUNT_ID:order-placed-topic"
S3_BUCKET="suleman-parcels-invoices-$AWS_ACCOUNT_ID"

echo "Using SNS Topic ARN: $SNS_TOPIC_ARN"
echo "Using S3 Bucket Name: $S3_BUCKET"

# SSH Key Resolution
if [ -f ~/.ssh/id_ed25519_github ]; then
    SSH_KEY="~/.ssh/id_ed25519_github"
elif [ -f ~/.ssh/gitlab_ed25519 ]; then
    SSH_KEY="~/.ssh/gitlab_ed25519"
else
    echo "ERROR: No valid SSH private key found in ~/.ssh/"
    exit 1
fi

echo "Using SSH Key: $SSH_KEY"
echo "Step 1: Building Spring Boot application locally..."
mvn clean package -DskipTests

echo "Step 2: Preparing Dockerfile on the fly..."
cat << 'EOF' > target/Dockerfile.run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

echo "Step 3: Copying JAR and Dockerfile to EC2 server ($EC2_IP)..."
# Disable host key checking for ease of PoC connection
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no target/cloud-infra-poc-0.0.1-SNAPSHOT.jar ubuntu@"$EC2_IP":/home/ubuntu/app.jar
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no target/Dockerfile.run ubuntu@"$EC2_IP":/home/ubuntu/Dockerfile.run

echo "Step 4: Building Docker image on EC2 and starting the app container..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ubuntu@"$EC2_IP" << EOF
  # Stop and remove the old container if it exists
  sudo docker stop spring-boot-app || true
  sudo docker rm spring-boot-app || true

  # Build the image on the EC2 instance
  sudo docker build -t cloud-infra-poc:latest -f Dockerfile.run .

  # Run the Spring Boot container, linking it to the RDS Database
  sudo docker run -d \
    --name spring-boot-app \
    -p 80:8080 \
    -e DB_HOST="$RDS_HOST" \
    -e DB_PORT=5432 \
    -e DB_NAME="$DB_NAME" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -e SNS_TOPIC_ARN="$SNS_TOPIC_ARN" \
    -e S3_BUCKET="$S3_BUCKET" \
    -e AWS_REGION="eu-north-1" \
    --restart always \
    cloud-infra-poc:latest
EOF

echo "🎉 Deployment completed successfully!"
echo "Check the health: http://$EC2_IP/api/orders/healthcheck"
