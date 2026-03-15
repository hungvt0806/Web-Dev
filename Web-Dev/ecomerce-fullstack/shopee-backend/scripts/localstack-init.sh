#!/bin/bash
# ── LocalStack init — creates S3 bucket on startup ──────────
set -e

AWS_DEFAULT_REGION=${DEFAULT_REGION:-ap-northeast-1}
BUCKET_NAME=${AWS_S3_BUCKET:-shopee-media-dev}

echo "[localstack-init] Creating S3 bucket: $BUCKET_NAME"

awslocal s3api create-bucket \
  --bucket "$BUCKET_NAME" \
  --region "$AWS_DEFAULT_REGION" \
  --create-bucket-configuration LocationConstraint="$AWS_DEFAULT_REGION" \
  || echo "[localstack-init] Bucket $BUCKET_NAME already exists"

# Make bucket publicly readable for dev (products images)
awslocal s3api put-bucket-policy \
  --bucket "$BUCKET_NAME" \
  --policy '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Principal":"*",
      "Action":["s3:GetObject"],
      "Resource":"arn:aws:s3:::'"$BUCKET_NAME"'/*"
    }]
  }'

echo "[localstack-init] Done. Bucket URL: http://localhost:4566/$BUCKET_NAME"
