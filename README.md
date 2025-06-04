# IPV Experian Knowledge Based Verification Credential Issuer


The Experian Knowledge Based Verification (KBV) Credential Issuer (CRI) gives users a way to prove their identity by answering a series of questions which only they should know. The RFC can be found [here](https://github.com/govuk-one-login/architecture/blob/main/rfc/0019-ipv-kbv-experian-interaction.md). In the case of the Experian KBV CRI, this involves questions about the user’s financial and credit history. For example, "What was the amount of you last mortgage payment". 

Information about working with Experian KBV CRI API can be found [here](https://govukverify.atlassian.net/wiki/spaces/OJ/pages/3837362945/Working+with+the+Experian+KBV+credential+issuer+API).

## Hooks

**important:** Once you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

## Deploy to dev environment

Ensure you have the `sam-cli` installed, [create a sso profile](https://govukverify.atlassian.net/wiki/spaces/LO/pages/3725591061/Getting+set+up+with+AWS+SSO+in+terminal+CLI+-+quickstart) for the role AdministratorAccessPermission based on `di-ipv-cri-dev` AWS account. Which can be found by searching the [AWS start page](https://uk-digital-identity.awsapps.com/start#/). 

Deploy to the dev environment with:

`AWS_PROFILE=profile-name-you-created di-ipv-cri-kbv-dev -- ./deploy.sh`

Override by supply a preferred stack name in place of `your-stack-name` below, the `CommonStackName` and `SecretPrefix`

`AWS_PROFILE=profile-name-you-created ./deploy.sh your-stack-name your-common-stack-name your-secret-prefix`

## Canaries
When deploying using sam deploy, canary deployment strategy will be used which is set in LambdaDeploymentPreference in template.yaml file. 

When deploying using the pipeline, canary deployment strategy set in the pipeline will be used and override the default set in template.yaml. 

Canary deployments will cause a rollback if any canary alarms associated with a lambda are triggered. 

To skip canaries such as when releasing urgent changes to production, set the last commit message to contain either of these phrases: [skip canary], [canary skip], or [no canary] as specified in the [Canary Escape Hatch guide](https://govukverify.atlassian.net/wiki/spaces/PLAT/pages/3836051600/Rollback+Recovery+Guidance#Escape-Hatch%3A-how-to-skip-canary-deployments-when-needed). 
`git commit -m "some message [skip canary]"`

Note: To update LambdaDeploymentPreference, update the LambdaCanaryDeployment pipeline parameter in the [identity-common-infra repository](https://github.com/govuk-one-login/identity-common-infra/tree/main/terraform/orange/address). To update the LambdaDeploymentPreference for a stack in dev using sam deploy, parameter override needs to be set in the [deploy script](./deploy.sh). 
`--parameter-overrides LambdaDeploymentPreference=<define-strategy> \`

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

## Run integration tests

To run these tests the following environment variables are needed:

- STACK_NAME
- AWS_REGION
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- AWS_SESSION_TOKEN

Temporary credentials can be found by going to the [AWS start page](https://uk-digital-identity.awsapps.com/start#/), selecting the account and clicking
"Command line or programmatic access" export these before you run the integration-tests.

To initiate journeys for the tests we use the `Headless Core Stub`, which runs in AWS and at the following endpoint `https://test-resources.review-K.dev.account.gov.uk`.

Optionally set a value for `TEST_RESOURCES_STACK_NAME` if you have deployed a local test resources stack and want to override the default stack named `test-resources`.

`ENVIRONMENT=localdev STACK_NAME=<your-stack> API_GATEWAY_ID_PRIVATE=<from-your-stack> API_GATEWAY_ID_PUBLIC=<from-your-stack> APIGW_API_KEY=xxxx TEST_RESOURCES_STACK_NAME= gradle integration-tests:cucumber`
