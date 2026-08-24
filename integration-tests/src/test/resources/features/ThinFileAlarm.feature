@manual @thin_file_alarm
Feature: Thin file rate alarm

  Scenario: Thin file journeys emit the metrics the alarm is built on
    Given the thin file rate alarm exists and is not already firing
    When 10 thin file journeys are completed
    Then the alarm's metrics show at least 10 thin files out of at least 10 responses

  Scenario: A sustained run of thin file journeys triggers the alarm
    Given the thin file rate alarm exists and is not already firing
    When 12 thin file journeys are completed in each of 2 consecutive alarm periods
    Then the thin file rate alarm enters the ALARM state
