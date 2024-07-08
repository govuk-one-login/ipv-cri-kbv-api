Feature: User goes through 3-out-of-4 question strategy. User answers 2 questions incorrectly and fails with a vc score of 0
  User goes through 2-out-of-3 question strategy. User answers 2 questions incorrectly and fails with a vc score of 0.
  Tests are run against the KBV Stub.

  @pre_merge_unhappy_medium_confidence
  Scenario: 3-out-of-4 question strategy user answers 2 questions incorrectly
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    When user sends a GET request to question endpoint when there are no questions left
    Then user gets status code 204

    # Authorization
    When user sends a GET request to authorization end point
    Then user gets status code 200
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point
    Then user gets status code 200
    And a valid access token code is returned in the response

    # Credential issue
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a verification score of 0 is returned in the response
    And 8 events are deleted from the audit events SQS queue

  @pre_merge_unhappy_low_confidence
  Scenario: 2-out-of-3 question strategy user answers 2 questions incorrectly
    Given user has the test-identity 197 and verificationScore of 1 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    When user sends a GET request to question endpoint when there are no questions left
    Then user gets status code 204

    # Authorization
    When user sends a GET request to authorization end point
    Then user gets status code 200
    And a valid authorization code is returned in the response

    # Access token
    When user sends a POST request to token end point
    Then user gets status code 200
    And a valid access token code is returned in the response

    # Credential issue
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a verification score of 0 is returned in the response
    And 8 events are deleted from the audit events SQS queue
