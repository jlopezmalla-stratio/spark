@rest
Feature: [Install Spark History Server] Installing Spark History Server

    Background:
        Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'

    Scenario: [Install Spark History Server][01] Install History Server

        Given I create file 'SparkHistoryServerInstallation.json' based on 'schemas/pf/SparkHistoryServer/BasicSparkHistoryServer.json' as 'json' with:
            |   $.service.name                                  |  UPDATE      | history-server                        | n/a     |

        #Copy DEPLOY JSON to DCOS-CLI
        When I outbound copy 'target/test-classes/SparkHistoryServerInstallation.json' through a ssh connection to '/dcos'

        #Start image from JSON
        And I run 'dcos package describe --app --options=/dcos/SparkHistoryServerInstallation.json spark-history-server > /dcos/SparkHistoryServerInstallationMarathon.json' in the ssh connection
        And I run 'sed -i -e 's|"image":.*|"image": "qa.stratio.com/stratio/spark-stratio-history-server:${STRATIO_SPARK_VERSION}",|g' /dcos/SparkHistoryServerInstallationMarathon.json' in the ssh connection
        And I run 'dcos marathon app add /dcos/SparkHistoryServerInstallationMarathon.json' in the ssh connection

        #Check Spark-history-server is Running
        Then in less than '500' seconds, checking each '20' seconds, the command output 'dcos task | grep "history-server" | grep R | wc -l' contains '1'

        #Find task-id if from DCOS-CLI
        And in less than '300' seconds, checking each '20' seconds, the command output 'dcos marathon task list history-server | grep history-server | awk '{print $2}'' contains 'True'
        And I run 'dcos marathon task list history-server | awk '{print $5}' | grep history-server | head -n 1' in the ssh connection and save the value in environment variable 'sparkHSId'

        #DCOS dcos marathon task show check healtcheck status
        Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkHSId} | grep TASK_RUNNING | wc -l' contains '1'
        Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkHSId} | grep healthCheckResults | wc -l' contains '1'
        Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkHSId} | grep  '"alive": true' | wc -l' contains '1'
