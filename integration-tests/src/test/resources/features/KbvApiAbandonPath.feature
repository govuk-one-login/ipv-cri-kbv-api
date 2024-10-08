Feature: User goes through 3-out-of-4 question strategy and 2-out-of-3 question strategy.
  Tests are run against the KBV Stub. User chooses to abandon the question during KBV flow

  @pre_merge_abandon_medium_confidence
  Scenario: 3-out-of-4 question strategy user abandons first question
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200
    And 5 events are deleted from the audit events SQS queue

  @pre_merge_abandon_medium_confidence
  Scenario Outline: 3-out-of-4 question strategy user abandons second question
    Given user has the test-identity <user> in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200
    And 5 events are deleted from the audit events SQS queue

    Examples:
      | user |
      | 197  |
  
  @pre_merge_abandon_low_confidence
  Scenario: 2-out-of-3 question strategy user abandons first question
    Given user has the test-identity 197 and verificationScore of 1 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200
    And 5 events are deleted from the audit events SQS queue

  @pre_merge_abandon_low_confidence
  Scenario Outline: 2-out-of-3 question strategy user abandons second question
    Given user has the test-identity <user> and verificationScore of 1 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200
    And 5 events are deleted from the audit events SQS queue

    Examples:
      | user |
      | 197  |
      | 256  |
