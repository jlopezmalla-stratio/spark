@rest
Feature: [Uninstall Spark Shuffle] Uninstalling Spark Shuffle

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'


  Scenario: [Spark Shuffle Uninstallation][01] Uninstall Spark Shuffle
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Given I run 'dcos marathon app remove spark-shuffle' in the ssh connection
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-shuffle | wc -l' contains '0'