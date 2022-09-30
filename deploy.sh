#!/usr/bin/env bash
set -eu
./gradlew
sam validate -t infrastructure/lambda/template.yaml
sam build -t infrastructure/lambda/template.yaml --config-env dev         
sam deploy -t infrastructure/lambda/template.yaml \
   --config-env dev \
   --config-file samconfig.toml \
   --no-fail-on-empty-changeset \
   --parameter-overrides   CodeSigningEnabled=false \
   AuditEventNamePrefix=/common-cri-parameters/KbvAuditEventNamePrefix \
   CriIdentifier=/common-cri-parameters/KbvCriIdentifier \
   CommonStackName=kbv-common-cri-api 
  
