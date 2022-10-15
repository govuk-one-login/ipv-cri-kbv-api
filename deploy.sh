#!/usr/bin/env bash
set -e

stack_name="$1"

if [ -z "$stack_name" ]
then
echo "😱 stack name expected as first argument, e.g. ./deploy kbv-my-stackname"
exit 1
fi

./gradlew clean
sam validate -t infrastructure/lambda/template.yaml
sam build -t infrastructure/lambda/template.yaml
sam deploy --stack-name "$stack_name" \
   --no-fail-on-empty-changeset \
   --no-confirm-changeset \
   --resolve-s3 \
   --region eu-west-2 \
   --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
   --parameter-overrides \
   CodeSigningEnabled=false \
   Environment=dev \
   AuditEventNamePrefix=/common-cri-parameters/KbvAuditEventNamePrefix \
   CriIdentifier=/common-cri-parameters/KbvCriIdentifier \
   CommonStackName=kbv-common-cri-api-local \
   SecretPrefix=kbv-cri-api
