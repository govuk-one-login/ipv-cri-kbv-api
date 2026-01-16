Feature: User goes through 3-out-of-4 question strategy. User answers 2 questions incorrectly and fails with a vc score of 0
  User goes through 2-out-of-3 question strategy. User answers 2 questions incorrectly and fails with a vc score of 0.
  Tests are run against the KBV Stub.

  @unhappy_medium_confidence
  Scenario Outline: 3-out-of-4 question strategy user answers 2 questions incorrectly
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question incorrectly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question incorrectly
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
    And the failed details array has 2 objects returned in the response

    Examples:
      | user |
      | 197  |

  @unhappy_low_confidence
  Scenario Outline: 2-out-of-3 question strategy user answers 2 questions incorrectly
    Given user has an overridden signed JWT using "<sharedClaims>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question incorrectly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question incorrectly
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
    And the failed details array has 2 objects returned in the response

    Examples:
      | user | sharedClaims        |
      | 197  | LOW_CONFIDENCE.json |
      | 256  | HILDA_HEAD.json     |

  @unhappy_timeout
  Scenario Outline: 3-out-of-4 question strategy and Experian response timed out
    Given user has an overridden signed JWT using "<sharedClaims>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint for timeout
    And verify timeout response is shown

    Examples:
      | sharedClaims      |
      | TEST_TIMEOUT.json |
