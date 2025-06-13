Feature: User visits the "/health/3rd-party/info" endpoint to see system information

  Scenario: All checks in the health check endpoint are passing

    Given user visits the verbose health check endpoint
    Then they should see a detailed report of all the checks that were performed
    And the statuses of those test should be pass
    And the web page status should be 200

    Given user visits the health check endpoint
    Then they should shall not see an output
    And the web page status should be 200
