#!/bin/bash

set -e

# Variables
CLUSTER_NAME="argo-cd-class-12th"
REGION="ap-south-1"
ROLE_NAME="secure-app-role"
NAMESPACE="secure-app-demo"
SERVICE_ACCOUNT="secure-app-sa"

# Get account ID and OIDC provider
ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --profile "sarowar-ostad" --output text)
OIDC_PROVIDER=$(aws eks describe-cluster --name $CLUSTER_NAME --region $REGION --query "cluster.identity.oidc.issuer" --profile "sarowar-ostad" --output text | sed -e "s/^https:\/\///")

echo "Account ID: $ACCOUNT_ID"
echo "OIDC Provider: $OIDC_PROVIDER"

# Create trust policy
cat > trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${OIDC_PROVIDER}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${OIDC_PROVIDER}:aud": "sts.amazonaws.com",
          "${OIDC_PROVIDER}:sub": "system:serviceaccount:${NAMESPACE}:${SERVICE_ACCOUNT}"
        }
      }
    }
  ]
}
EOF

# Create IAM role
echo "Creating IAM role..."
aws iam create-role \
  --role-name $ROLE_NAME \
  --assume-role-policy-document file://trust-policy.json \
  --description "IAM role for secure-app service account in EKS" --profile "sarowar-ostad" \
  --output text

# Attach basic policies (modify as needed)
echo "Attaching policies..."
aws iam attach-role-policy \
  --role-name $ROLE_NAME \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess --profile "sarowar-ostad"

# Output the role ARN
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo "✅ IAM Role created successfully!"
echo "Role ARN: $ROLE_ARN"

# Update the Kubernetes manifest
sed -i.bak "s|arn:aws:iam::YOUR_AWS_ACCOUNT:role/secure-app-role|$ROLE_ARN|g" secure-app/rbac-setup.yaml

echo "✅ Updated Kubernetes manifest with role ARN: $ROLE_ARN"
echo "✅ You can now deploy your application using: kubectl apply -f secure-app/"