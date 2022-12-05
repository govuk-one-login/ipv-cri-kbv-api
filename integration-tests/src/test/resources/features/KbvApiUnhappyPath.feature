Feature: 3 out of 4 strategy. User has 2 KBV questions. Tests are run against the KBV Stub.
  User answers the two questions incorrectly and fails.

  @pre_merge_unhappy
  Scenario: User answers 2 questions incorrectly
    Given user has the user identity in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

     #First Question
    When user sends a GET request to question end point
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    #Second Question
    When user sends a GET request to question end point
    Then user gets status code 200
    And user answers the question incorrectly
    Then user gets status code 200

    When user sends a GET request to question end point when there are no questions left
    Then user gets status code 204

