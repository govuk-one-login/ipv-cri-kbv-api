Feature: 3 out of 4 strategy. User Sheila Hil has total of 3 KBV questions.
  She answers the first  two questions correctly and the third question incorrectly.
  She gets Status Code 204 when she asks for the fourth question.

  @pre_merge_happy
  Scenario: Sheila Hill Insufficient KBV questions mid flight (2 correct, 1 incorrect)
    Given user has the user identity in the form of a signed JWT string
    When user sends a POST request to session end point
    Then user gets a session-id
    When user sends a GET request to question end point
    Then user gets the first question with status code 200
    When user answers the first question correctly
    Then user gets status code 200
    When user sends a GET request to question end point for the second question
    Then user gets the second question with status code 200
    When user answers the second question correctly
    Then user gets status code 200 for question two POST
    When user sends a GET request to question end point for the third question
    Then user gets status code 204 for the third call
    When user sends a GET request to authorization end point
    Then user gets status code 200 with authorization code
    When user sends a POST request to token end point
    Then user gets status code 200 with a valid access token code
    When user sends a POST request to Credential Issue end point with a valid access token
    Then user gets status code 200 and a JWT

  @Sheila_Hil_insufficient_Test_2
  Scenario: Sheila Hill Insufficient KBV questions mid flight (1 correct, 1 incorrect)
    Given user has the user identity in the form of a signed JWT string
    When user sends a POST request to session end point
    Then user gets a session-id
    When user sends a GET request to question end point
    Then user gets the first question with status code 200
    When user answers the first question correctly
    Then user gets status code 200
    When user sends a GET request to question end point for the second question
    Then user gets the second question with status code 200
    When user answers the second question incorrectly
    Then user gets status code 200 for question two POST
    When user sends a GET request to question end point for the third question
    Then user gets status code 204 for the third call





