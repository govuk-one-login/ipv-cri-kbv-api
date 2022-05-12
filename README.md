# di-ipv-cri-kbv-api
IPV Knowledge Based Verification Credential Issuer

## Check out submodules (First Time)
> The first time you check out or clone the repository, you will need to run the following commands:

`git submodule update --init --recursive`

## Update submodules (Subsequent times)
> Subsequent times you will need to run the following commands:

`git submodule update --recursive`

## Updating submodules to the latest "main" branch
> You can also update the submodules to the latest "main" branch, but this is not done automatically
> in case there have been changes made to the shared libraries you do not yet want to track

cd into each submodule (folders are `/lib` and `/common-lambdas`) and run the following commands:

`git checkout main && git pull`

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

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
