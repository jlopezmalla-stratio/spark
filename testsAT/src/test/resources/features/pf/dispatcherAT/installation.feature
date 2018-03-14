@rest
Feature: [Install Spark Dispatcher] Installing Spark Dispatcher

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'

  Scenario:[Spark dispatcher Installation][01]Basic Installation Spark dispatcher
    Given I create file 'SparkDispatcherInstallation.json' based on 'schemas/pf/SparkDispatcher/BasicSparkDispatcher.json' as 'json' with:
      | $.service.name | UPDATE | ${SPARK_FW_NAME} | n/a |

    #Copy DEPLOY JSON to DCOS-CLI
    When I outbound copy 'target/test-classes/SparkDispatcherInstallation.json' through a ssh connection to '/dcos'

    #Start image from JSON
    And I run 'dcos package describe --app --options=/dcos/SparkDispatcherInstallation.json spark-dispatcher > /dcos/SparkDispatcherInstallationMarathon.json' in the ssh connection
    And I run 'sed -i -e 's|"image":.*|"image": "${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION}",|g' /dcos/SparkDispatcherInstallationMarathon.json' in the ssh connection
    And I run 'dcos marathon app add /dcos/SparkDispatcherInstallationMarathon.json' in the ssh connection

    #Check Spark-fw is Running
    Then in less than '500' seconds, checking each '20' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'

    #Find task-id if from DCOS-CLI
    And in less than '300' seconds, checking each '20' seconds, the command output 'dcos marathon task list ${SPARK_FW_NAME} | grep ${SPARK_FW_NAME} | awk '{print $2}'' contains 'True'
    And I run 'dcos marathon task list ${SPARK_FW_NAME} | awk '{print $5}' | grep ${SPARK_FW_NAME} | head -n 1' in the ssh connection and save the value in environment variable 'sparkTaskId'

    #DCOS dcos marathon task show check healtcheck status
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkTaskId} | grep TASK_RUNNING | wc -l' contains '1'
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkTaskId} | grep healthCheckResults | wc -l' contains '1'
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkTaskId} | grep  '"alive": true' | wc -l' contains '1'