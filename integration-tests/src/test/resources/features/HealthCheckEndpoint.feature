Feature: User visits the "/healthcheck/thirdparty/info" endpoint to see system information

  Scenario: The API is only accessible via the GDS VPN

    Given user makes a request to "/healthcheck/thirdparty" without being on the VPN
    Then they should see an access denied error
    And the response status should be 403

    Given user makes a request to "/healthcheck/thirdparty/info" without being on the VPN
    Then they should see an access denied error
    And the response status should be 403
