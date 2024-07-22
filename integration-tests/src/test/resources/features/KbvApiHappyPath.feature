Feature: User goes through 3-out-of-4 question strategy. User answers 3 questions correctly and gets a valid JWT from credential issuer with a score of 2
  User goes through 2-out-of-3 question strategy. User answers 2 questions correctly and gets a valid JWT credential issuer with a score of 1.
  Tests are run against the KBV Stub.

  @pre_merge_happy_with_device_information_header
  Scenario: User answers 3 questions correctly in 3-out-of-4 question strategy with device information header
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    Then TXMA event is added to the SQS queue containing device information header

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
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

    # Credential issued
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a valid JWT is returned in the response
    And a verification score of 2 is returned in the response
    And the check details array has 3 objects returned in the response
    And 10 events are deleted from the audit events SQS queue

  @pre_merge_happy_medium_confidence
  Scenario: User answers 3 questions correctly in 3-out-of-4 question strategy
    Given user has the test-identity 197 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    Then TXMA event is added to the SQS queue not containing device information header

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
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

    # Credential issued
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a valid JWT is returned in the response
    And a verification score of 2 is returned in the response
    And the check details array has 3 objects returned in the response
    And 10 events are deleted from the audit events SQS queue

  @pre_merge_happy_low_confidence
  Scenario: User answers 2 questions correctly in 2-out-of-3 question strategy
    Given user has the test-identity 197 and verificationScore of 1 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    Then TXMA event is added to the SQS queue containing evidence requested

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
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

    # Credential issued
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a valid JWT is returned in the response
    And a verification score of 1 is returned in the response
    And the check details array has 2 objects returned in the response
    And 8 events are deleted from the audit events SQS queue

  @pre_merge_happy_low_confidence
  Scenario: User answers 2 out of 3 questions correctly with in 2-out-of-3 question strategy
    Given user has the test-identity 197 and verificationScore of 1 in the form of a signed JWT string

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    Then TXMA event is added to the SQS queue containing evidence requested

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And user answers the question correctly
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

    # Credential issued
    When user sends a POST request to credential issue endpoint with a valid access token
    Then user gets status code 200
    And a valid JWT is returned in the response
    And a verification score of 1 is returned in the response
    And the check details array has 2 objects returned in the response
    And the failed details array has 1 objects returned in the response
    And 10 events are deleted from the audit events SQS queue
