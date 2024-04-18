# IPV Knowledge Based Verification Credential Issuer

## Hooks

**important:** Once you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

## Deploy to dev environment

Ensure you have the `sam-cli` and `gds-cli` installed, and that you can assume an admin role on the `di-ipv-cri-dev` AWS account.
Alternatively you can [create a sso profile](https://govukverify.atlassian.net/wiki/spaces/LO/pages/3725591061/Getting+set+up+with+AWS+SSO+in+terminal+CLI+-+quickstart)

Deploy to the dev environment with:

`gds aws  di-ipv-cri-kbv-dev -- ./deploy.sh` would create a stack using defaults

or using sso profile for di-ipv-cri-kbv-dev

`AWS_PROFILE=profile-name-you-created di-ipv-cri-kbv-dev -- ./deploy.sh`

Override by supply a preferred stack name in place of `your-stack-name` below, the `CommonStackName` and `SecretPrefix`

`gds aws  di-ipv-cri-kbv-dev -- ./deploy.sh your-stack-name your-common-stack-name your-secret-prefix`

`AWS_PROFILE=profile-name-you-created ./deploy.sh your-stack-name your-common-stack-name your-secret-prefix`

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

To run:

Below runs by with stub client using `https://cri.core.build.stubs.account.gov.uk` in AWS with stub a client_id `ipv-core-stub-aws-stub`

`ENVIRONMENT=xxxx STACK_NAME=xxxx IPV_CORE_STUB_CRI_ID=kbv-cri-dev  API_GATEWAY_ID_PRIVATE=xxxx API_GATEWAY_ID_PUBLIC=xxxx IPV_CORE_STUB_BASIC_AUTH_USER=xxxx IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL="https://cri.core.build.stubs.account.gov.uk" DEFAULT_CLIENT_ID=ipv-core-stub-aws-build APIGW_API_KEY=xxxx gradle integration-tests:cucumber`

NB: The environment variable with value `kbv-cri-dev` allows the command above to use keys in `ipv-config` pointing to keys in `di-ipv-cri-kbv-dev` for the deployed stack.



