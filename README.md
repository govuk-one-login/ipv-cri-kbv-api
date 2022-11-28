# IPV Knowledge Based Verification Credential Issuer

## Hooks

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

## Deploy to dev environment

Ensure you have the `sam-cli` and `gds-cli` installed, and that you can assume an admin role on the `di-ipv-cri-dev` AWS account.


Copy `infrastructure/lambda/samconfig.toml.example` to `infrastructure/lambda/samconfig.toml`, and change the stack name in the new file.

Deploy to the dev environment with:

`gds aws di-ipv-cri-dev -- ./deploy.sh`


## Deploy to AWS lambda

Automated GitHub actions deployments have been enabled on this repository.

The automated deployments are triggered on a push to main after PR approval.

**Required GitHub secrets:**

Pre merge integration tests

| Secret            | Description                                                     |
|-------------------|-----------------------------------------------------------------|
| AWS_CONFIG_BUCKET | Bucket where integration code is pushed for deployment          |
| AWS_PROFILE_PATH  | Parameter Store path to the signing profile versioned ARN       |
| AWS_ROLE_ARN      | Assumed role IAM ARN                                            |
| AWS_ROLE_SESSION  | Assumed Role Session ID                                         |

Deployment to Dev:

| Secret                          | Description                                       |
|---------------------------------|---------------------------------------------------|
| DEV_ARTIFACT_SOURCE_BUCKET_NAME | Bucket where lambda code is pushed for deployment |
| DEV_SIGNING_PROFILE_NAME        | The AWS signer signing profile name               |
| DEV_GH_ACTIONS_ROLE_ARN         | Assumed role IAM ARN                              |                       |

Deployment to Build:

| Secret                            | Description                                           |
|-----------------------------------|-------------------------------------------------------|
| BUILD_ARTIFACT_SOURCE_BUCKET_NAME | Bucket where lambda code is pushed for deployment     |
| BUILD_SIGNING_PROFILE_NAME        | The AWS signer signing profile name                   |
| BUILD_GH_ACTIONS_ROLE_ARN         | Assumed role IAM ARN                                  |

## Required SSM Parameters

| Parameter                  | Description                      |
|----------------------------|----------------------------------|
| `/alerting/email-address`  | email address to receive alerts  |

## Run Integration Tests

To run all tests:
`IPV_CORE_STUB_CRI_ID=kbv-cri-dev ENVIRONMENT=dev STACK_NAME=di-ipv-cri-kbv-api-xxxx API_GATEWAY_ID_PRIVATE=xxxx API_GATEWAY_ID_PUBLIC=xxxx IPV_CORE_STUB_BASIC_AUTH_USER=user IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL="https://di-ipv-core-stub.london.cloudapps.digital" APIGW_API_KEY=xxxx gradle integration-tests:cucumber`

To run a particular test tag: 
`IPV_CORE_STUB_CRI_ID=kbv-cri-dev ENVIRONMENT=dev STACK_NAME=di-ipv-cri-kbv-api-xxxx API_GATEWAY_ID_PRIVATE=xxxx API_GATEWAY_ID_PUBLIC=xxxx IPV_CORE_STUB_BASIC_AUTH_USER=user IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL="https://di-ipv-core-stub.london.cloudapps.digital" APIGW_API_KEY=xxxx gradle cucumber -P tags=@tag name`
