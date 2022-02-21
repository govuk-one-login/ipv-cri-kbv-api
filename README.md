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

Automated GitHub actions deployments to di-ipv-cri-dev have been enabled for this repository.

The automated deployments are triggered on a push to main after PR approval.

GitHub secrets are required which must be configured in an environment for security reasons.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| AWS_ROLE_ARN | Assumed role IAM ARN |
| AWS_PROFILE_PATH | Parameter Store path to the signing profile versioned ARN |
| AWS_ROLE_SESSION | Assumed Role Session ID
