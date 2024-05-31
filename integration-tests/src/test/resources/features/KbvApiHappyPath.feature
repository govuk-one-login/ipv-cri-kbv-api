Feature: 3 out of 4 strategy. User has 2 KBV questions. Tests are run against the KBV Stub.
  User answers the two questions correctly and gets valid JWT from the credential issuer

  @pre_merge_happy_with_device_information_header
  Scenario: User answers 2 questions correctly with device information header
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
    And 10 events are deleted from the audit events SQS queue

  @pre_merge_happy
  Scenario: User answers 2 questions correctly
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
    And 10 events are deleted from the audit events SQS queue
