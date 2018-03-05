@rest
Feature: [Spark Kafka Coverage] Kafka Coverage tests

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    #Check dispatcher and spark-coverage are deployed
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'

  Scenario:[Spark Kafka coverage][01] Deploy Kafka spark job

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

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

    #Check kafka output is correct
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverKafka} stdout --lines=1000 | grep "###" | grep -v "0"' contains '###'

    #Now kill the process
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
