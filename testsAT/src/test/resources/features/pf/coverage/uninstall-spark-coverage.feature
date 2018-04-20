@rest
Feature: [Uninstall Spark Coverage] Uninstalling Spark Coverage

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'


  Scenario: [Spark Coverage Uninstallation][01] Uninstall Spark Coverage
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Given I run 'dcos marathon app remove spark-coverage' in the ssh connection
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | wc -l' contains '0'