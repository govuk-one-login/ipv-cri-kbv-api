Feature: User goes through 3-out-of-4 question strategy. User answers 3 questions correctly and gets a valid JWT from credential issuer with a score of 2
  User goes through 2-out-of-3 question strategy. User answers 2 questions correctly and gets a valid JWT credential issuer with a score of 1.
  Tests are run against the KBV Stub.

  @happy_with_device_information_header
  Scenario Outline: User answers 3 questions correctly in 3-out-of-4 question strategy with device information header
    Given user has an overridden signed JWT using "<sharedClaims>"

    # Session
    When user sends a POST request to session end point with txma header
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_KBV_CRI_START"
    And a valid START event is returned in the response with txma header
    Then START TxMA event is validated against schema

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200
    When user sends a GET request to events end point for "IPV_KBV_CRI_RESPONSE_RECEIVED"
    Then a RESPONSE_RECEIVED event is returned with repeatAttemptAlert present <alert>

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
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
    And a valid JWT is returned in the response for "<sharedClaims>"
    And a verification score of 2 is returned in the response
    And the check details array has 3 objects returned in the response

    Examples:
      | user | sharedClaims          | alert |
      | 197  | DEFAULT.json          | false |
      | 1188 | CAROLINE_COULSON.json | true  |

  @happy_medium_confidence
  Scenario Outline: User answers 3 questions correctly in 3-out-of-4 question strategy
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_KBV_CRI_START"
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
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
    And a valid JWT is returned in the response for ""
    And a verification score of 2 is returned in the response
    And the check details array has 3 objects returned in the response

    Examples:
      | user |
      | 197  |

  @happy_low_confidence
  Scenario Outline: User answers 2 questions correctly in 2-out-of-3 question strategy
    Given user has an overridden signed JWT using "<confidenceProfile>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_KBV_CRI_START"
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
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
    And a valid JWT is returned in the response for "<confidenceProfile>"
    And a verification score of 1 is returned in the response
    And the check details array has 2 objects returned in the response

    Examples:
      | user | confidenceProfile   |
      | 197  | LOW_CONFIDENCE.json |

  @happy_low_confidence
  Scenario Outline: User answers 2 out of 3 questions correctly with in 2-out-of-3 question strategy
    Given user has an overridden signed JWT using "<confidenceProfile>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # TXMA event
    When user sends a GET request to events end point for "IPV_KBV_CRI_START"
    And a valid START event is returned in the response without txma header
    Then START TxMA event is validated against schema

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
    Then user gets status code 200

    # Second question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question incorrectly
    Then user gets status code 200

    # Third question
    When user sends a GET request to question endpoint
    Then user gets status code 200
    And <user> answers the question correctly
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
    And a valid JWT is returned in the response for "<confidenceProfile>"
    And a verification score of 1 is returned in the response
    And the check details array has 2 objects returned in the response
    And the failed details array has 1 objects returned in the response

    Examples:
      | user | confidenceProfile   |
      | 197  | LOW_CONFIDENCE.json |
      | 256  | HILDA_HEAD.json     |
