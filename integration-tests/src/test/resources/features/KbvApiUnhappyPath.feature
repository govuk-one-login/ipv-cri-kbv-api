Feature: 3 out of 4 strategy. User has 2 KBV questions. Tests are run against the KBV Stub.
  User answers the two questions incorrectly and fails.

  @pre_merge_unhappy
  Scenario: User answers 2 questions incorrectly
    Given user has the test-identity 168 in the form of a signed JWT string

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

    #Authorization
    When user sends a GET request to authorization end point
    Then user gets status code 200
    And a valid authorization code is returned in the response

    #Access Token
    When user sends a POST request to token end point
    Then user gets status code 200
    And a valid access token code is returned in the response

    #Credential Issue
    When user sends a POST request to Credential Issue end point with a valid access token
    Then user gets status code 200
    And a verification score of 0 is returned in the response
