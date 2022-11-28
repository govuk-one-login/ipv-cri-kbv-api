@pre_merge_abandon
Feature: 3 out of 4 strategy. User has 2 KBV questions. Tests are run against the KBV Stub.
  User chooses to abandon the question during KBV flow

  Scenario: User abandons first question
    Given user has the test-identity 168 in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

    #First Question
    When user sends a GET request to question end point
    Then user gets status code 200

    #Abandon
    When user chooses to abandon the question
    Then user gets status code 200

  Scenario: User abandons second question
    Given user has the test-identity 168 in the form of a signed JWT string

    #Session
    When user sends a POST request to session end point
    Then user gets a session-id

    #First Question
    When user sends a GET request to question end point
    Then user gets status code 200
    And user answers the question correctly
    Then user gets status code 200

    #Second Question
    When user sends a GET request to question end point
    Then user gets status code 200

    #Abandon
    When user chooses to abandon the question
    Then user gets status code 200
