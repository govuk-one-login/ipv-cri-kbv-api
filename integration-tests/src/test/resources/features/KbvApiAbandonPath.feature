Feature: User goes through 3-out-of-4 question strategy and 2-out-of-3 question strategy.
  Tests are run against the KBV Stub. User chooses to abandon the question during KBV flow

  @abandon_medium_confidence
  Scenario: 3-out-of-4 question strategy user abandons first question
    Given user has a default signed JWT

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200

  @abandon_medium_confidence
  Scenario Outline: 3-out-of-4 question strategy user abandons second question
    Given user has a default signed JWT

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

    Examples:
      | user |
      | 197  |

  @abandon_low_confidence
  Scenario Outline: 2-out-of-3 question strategy user abandons first question
    Given user has an overridden signed JWT using "<confidenceProfile>"

    # Session
    When user sends a POST request to session end point
    Then user gets a session-id

    # First question
    When user sends a GET request to question endpoint
    Then user gets status code 200

    # Abandon
    When user chooses to abandon the question
    Then user gets status code 200
    Examples:
      | confidenceProfile   |
      | LOW_CONFIDENCE.json |

  @abandon_low_confidence
  Scenario Outline: 2-out-of-3 question strategy user abandons second question
    Given user has an overridden signed JWT using "<sharedClaims>"

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

    Examples:
      | user | sharedClaims    |
      | 256  | HILDA_HEAD.json |
