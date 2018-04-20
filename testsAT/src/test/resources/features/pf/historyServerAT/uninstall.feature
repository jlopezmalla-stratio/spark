@rest
Feature: [Uninstall Spark History Server] Uninstalling Spark History Server

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'


  Scenario: [Spark history server Uninstallation][01] Uninstall Spark History Server
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Given I run 'dcos marathon app remove history-server' in the ssh connection
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos task | grep history-server | wc -l' contains '0'