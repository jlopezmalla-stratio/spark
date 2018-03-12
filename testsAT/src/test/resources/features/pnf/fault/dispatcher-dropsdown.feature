@rest
Feature: [Stability test] Dispatcher Dropdowns

  Background:

    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    #Check dispatcher and spark-coverage are deployed
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

    #Clean all drivers from spark-dispatcher
    Then I clean all the drivers in the dispatcher with name '${SPARK_FW_NAME}' in dcos host '${CLUSTER_ID}.labs.stratio.com' with mesos master '!{MESOS_MASTER}:5050' with user 'admin' and password '1234'

  Scenario:[Dispatcher Dropdowns][01] Launch Kafka job, delete dispatcher, and check if we launch a new dispatcher, retrieve the running drivers

    #Now launch the work
    Given I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    And I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'

    When I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/create' based on 'schemas/pf/SparkCoverage/kafka_curl.json' as 'json' with:
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/kafka-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.jars']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/kafka-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.mesos.executor.docker.image']  |  UPDATE  | ${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION} | n/a     |
      |   $.appArgs[0]  |  UPDATE  | gosec1.node.paas.labs.stratio.com:9092 | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Save the driver launched id
    Then I save the value from field in service response 'submissionId' in variable 'driverKafka'

    #Check the driver starts
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverKafka} stdout --lines=1000 | grep "###"' contains '###'

    #Now kill the running dispatcher
    Then I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then I run 'dcos marathon app remove ${SPARK_FW_NAME}' in the ssh connection

    #Check the dispatcher has been killed
    Then in less than '150' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | wc -l' contains '0'
    Then in less than '150' seconds, checking each '10' seconds, the command output 'dcos marathon task list ${SPARK_FW_NAME} | wc -l' contains '0'

    #Now we deploy another dispatcher (Copied from installation.feature... TODO Check a way better)
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


    #Now check that the new dispatcher, has adopted the previous driver as child
    Then in less than '200' seconds, checking each '10' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .tasks |  map(select(.name | contains ("AT-kafka"))) | map(select(.id == "!{driverKafka}")) | .[] | .state' | grep "TASK_RUNNING" | wc -l' contains '1'

    #Check the driver is working correctly, independent from dispatcher
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then I run 'dcos task log !{driverKafka} stdout --lines=1000000 | grep "###" | wc -l' in the ssh connection and save the value in environment variable 'PREVIOUS_WINDOW'
    Then in less than '100' seconds, checking each '10' seconds, the command output 'if [ $(dcos task log !{driverKafka} stdout --lines=1000000 | grep "###" | wc -l) -gt "!{PREVIOUS_WINDOW}" ]; then echo "true"; fi' contains 'true'

    #Now kill the kafka driver
    #(We send a JSON because the step from cucumber, doesn't support empty posts submissions)
    Then I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    Then I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'
    Then I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/kill/!{driverKafka}' based on 'schemas/pf/SparkCoverage/kafka_curl.json' as 'json' with:
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/kafka-${COVERAGE_VERSION}.jar | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Check exit is clean
    Then in less than '200' seconds, checking each '10' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-kafka"))) | map(select(.id == "!{driverKafka}")) | .[] | .state' | grep "TASK_KILLED" | wc -l' contains '1'

    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-kafka"))) | map(select(.id == "!{driverKafka}")) | .[] | .statuses' | grep "TASK_RUNNING"  | wc -l' contains '1'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-kafka"))) | map(select(.id == "!{driverKafka}")) | .[] | .statuses' | grep "TASK_FAILED"  | wc -l' contains '0'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-kafka"))) | map(select(.id == "!{driverKafka}")) | .[] | .statuses' | grep "TASK_KILLED"  | wc -l' contains '1'