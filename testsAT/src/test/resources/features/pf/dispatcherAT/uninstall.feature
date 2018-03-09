@rest
Feature: [Uninstall Spark Dispatcher] Uninstalling Spark Dispatcher

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'


  Scenario: [Spark dispatcher Uninstallation][01] Uninstall Spark Dispatcher
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Given I run 'dcos marathon app remove ${SPARK_FW_NAME}' in the ssh connection
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | wc -l' contains '0'