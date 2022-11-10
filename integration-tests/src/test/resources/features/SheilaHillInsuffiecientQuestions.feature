Feature: 3 out of 4 strategy. User Sheila Hil has total of 3 KBV questions.
  She answers the first  two questions correctly and the third question incorrectly.
  She gets Status Code 204 when she asks for the fourth question.

  @Sheila_Hil_insufficient_Test_1
  Scenario: Sheila Hill Insufficient KBV questions mid flight (2 correct 1 incorrect)
    Given user has the user identity in the form of a signed JWT string

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
    And user selects Insufficient additional questions





