# di-ipv-cri-kbv-api
IPV Knowledge Based Verification Credential Issuer

## Deploy to PaaS

Set environment variables with:
````
cf set-env di-ipv-cri-kbv-api EXPERIAN_API_WRAPPER_URL  <experian api wrapper api url>
cf set-env di-ipv-cri-kbv-api EXPERIAN_API_WRAPPER_SAA_RESOURCE  "/question-request"

````

Then build and push with:
````
./gradlew clean build
cf push
````

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

| Secret                      | Description                                           |
|-----------------------------|-------------------------------------------------------|
| ARTIFACT_SOURCE_BUCKET_NAME | Bucket where lambda code is pushed for deployment     |
| SIGNING_PROFILE_NAME        | The AWS signer signing profile name                   |
| GH_ACTIONS_ROLE_ARN         | Assumed role IAM ARN                                  |        

