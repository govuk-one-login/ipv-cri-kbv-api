Feature: Certificate Expiry step function test

  Scenario: Step function successfully completes
    Given no certificate overrides are passed into the step function
    When the step function is invoked
    Then the step function executes successfully

  Scenario: No certificate expiring
    Given no certificates are expiring soon
    When the step function is invoked
    Then the step function executes successfully
    And the step function will push 0 to the 90 day metric
    And the step function will push 0 to the 7 day metric

  Scenario: Certificate expiring within 90 days
    Given a certificate is expiring within 90 days
    When the step function is invoked
    Then the step function executes successfully
    And the step function will push 1 to the 90 day metric

  Scenario: Certificate expiring within 7 days
    Given a certificate is expiring within 7 days
    When the step function is invoked
    Then the step function executes successfully
    And the step function will push 1 to the 7 day metric

  Scenario: Multiple certificates are passed into the step function
    Given three certificates with various expiries are passed into the step function
    When the step function is invoked
    Then the step function executes successfully
    And the step function will push 0 to the 90 day metric
    And the step function will push 0 to the 7 day metric
    And the step function will push 1 to the 7 day metric
    And the step function will push 1 to the 90 day metric
