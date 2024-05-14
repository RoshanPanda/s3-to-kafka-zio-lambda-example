#!/bin/bash
custom_tag=1
custom_image=wmos-to-pe
echo "==============================================================================="
# Step 0: Get login password for AWS ECR
echo "Building Jar..."
sbt package
echo "Built Jar..."
echo "==============================================================================="
# Step 1: Get login password for AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 675303442877.dkr.ecr.us-east-1.amazonaws.com
echo "==============================================================================="

# Step 2: Docker build
echo "Building Docker Image..."
docker build -t $custom_image .
echo "Built Docker Image..."
echo "==============================================================================="

# Step 3: Tag the image as latest
echo "Tagging image as latest..."
docker tag $custom_image:latest 675303442877.dkr.ecr.us-east-1.amazonaws.com/$custom_image:latest
echo "Tagged image as latest."
echo "==============================================================================="

# Step 4: Tag the image with custom tag from environment variable
echo "Tagging image with custom tag: $custom_tag..."
docker tag $custom_image:latest 675303442877.dkr.ecr.us-east-1.amazonaws.com/$custom_image:"$custom_tag"
docker image ls --format '{{.Repository}}:{{.Tag}}' | grep 'preaudit-transform-records'
echo "Tagged image with custom tag: $custom_tag."
echo "==============================================================================="

# Step 5: Docker push
echo "Pushing images to ECR..."
docker push 675303442877.dkr.ecr.us-east-1.amazonaws.com/$custom_image:latest
docker push 675303442877.dkr.ecr.us-east-1.amazonaws.com/$custom_image:"$custom_tag"
echo "Images pushed to ECR."
echo "==============================================================================="
