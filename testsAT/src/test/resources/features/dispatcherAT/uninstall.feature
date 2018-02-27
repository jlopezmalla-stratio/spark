@rest
Feature: [Uninstall Spark Dispatcher] Uninstalling Spark Dispatcher

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'


  Scenario: [Spark dispatcher Uninstallation][01] Uninstall Spark Dispatcher
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Given I run 'dcos marathon app remove spark-fw' in the ssh connection
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos task | grep "spark-fw\." | wc -l' contains '0'