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

to do
