# User Journey Acceptance Tests

Run a set of user journeys that test that the KBV credential issuer is working as expected.

These tests run as part of our continuous delivery pipeline, after the API has been deployed to environments.

The tests begin and end from an [IPV Core Stub](https://github.com/alphagov/di-ipv-stubs/tree/main/di-ipv-core-stub) instance.

Run the tests locally with

`docker run -e PIPELINE_START_URL=xxx -e PIPELINE_BASIC_AUTH_USERNAME=xxx -e PIPELINE_BASIC_AUTH_PASSWORD=xxx --rm -it $(docker build -q .)`

