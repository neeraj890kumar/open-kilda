@Failsafe
Feature: Failsafe Suite

  Verify correct system behavior under unexpected conditions like isl going down etc.

  Background:
    Given the reference topology

  Scenario: ISL goes down, system is able to react in expected way and reroute
    Given Create 1 flow with A Switch used and at least 1 alternate path between source and destination switch and 500 bandwidth

    When ISL between switches goes down
    And Remains in this state for 30 seconds

    Then ISL status is DISCOVERED
    And ISL status changes to FAILED
    Then flow is in UP state
    And flow is valid per Northbound validation
    And all active switches have correct rules installed per Northbound validation
    And flow has traffic going with bandwidth not less than 450 and not greater than 550

    When Changed ISL goes up

    Then ISL status changes to DISCOVERED
    And flow is in UP state
    And flow is valid per Northbound validation
    And all active switches have correct rules installed per Northbound validation
    And flow has traffic going with bandwidth not less than 450 and not greater than 550
    And each flow can be deleted

  Scenario: Replugging cable to some other switch/port should cause old ISL status to be changed to MOVED
    Given select a random ISL with A-Switch and alias it as 'aswitchIsl'
    And select a reverse path ISL for 'aswitchIsl' and alias it as 'aswitchIslReverse'
    And select a random not connected A-Switch link and alias it as 'notConnectedLink'
    And a potential ISL from 'aswitchIsl' source to 'notConnectedLink' source aliased as 'expectedNewIsl'
    And a potential ISL from 'notConnectedLink' source to 'aswitchIsl' source aliased as 'expectedNewIslReverse'

    When replug 'aswitchIsl' destination to 'notConnectedLink' source
    Then ISL status changes to MOVED for ISLs: aswitchIsl, aswitchIslReverse
    And ISL status changes to DISCOVERED for ISLs: expectedNewIsl, expectedNewIslReverse

    When replug 'expectedNewIsl' source to 'aswitchIsl' destination
    Then ISL status changes to MOVED for ISLs: expectedNewIsl, expectedNewIslReverse
    And ISL status changes to DISCOVERED for ISLs: aswitchIsl, aswitchIslReverse
