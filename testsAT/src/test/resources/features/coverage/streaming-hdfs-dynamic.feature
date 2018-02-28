@rest
Feature: [HDFS Streaming HDFS Dynamic Coverage] Streaming HDFS Dynamic Coverage tests

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    #Check dispatcher and spark-coverage are deployed
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'

  Scenario:[Spark Streaming HDFS Dynamic coverage][01] Deploy Streaming HDFS Dynamic spark job

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

    #Now launch the work
    Given I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    And I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'

    When I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/create' based on 'schemas/SparkCoverage/streaming_hdfs_dynamic_curl.json' as 'json' with:
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/streaming-hdfs-dynamic-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.jars']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/streaming-hdfs-dynamic-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.mesos.executor.docker.image']  |  UPDATE  | ${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION} | n/a     |
      |   $.sparkProperties['spark.mesos.driverEnv.SPARK_SECURITY_HDFS_CONF_URI']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/configs/${CLUSTER_ID} | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Save the driver launched id
    Then I save the value from field in service response 'submissionId' in variable 'driverDynamic'

    #Check output is correct (We wait until the driver says that will kill one executor)
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Registered executor NettyRpcEndpointRef"' contains 'executor'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Adding task set"' contains 'task'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Capping the total amount of executors to 0"' contains 'executors'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Job 0 finished"' contains 'Main'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Requesting to kill executor"' contains 'executor'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Existing executor [0-9] has been removed"' contains 'executor'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverDynamic} stdout --lines=2000 --completed | grep "Mesos task [0-9] is now TASK_KILLED"' contains 'TASK_KILLED'

    #We check that there are executors with KILLED status
    Then in less than '100' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select((.name | contains("AT-hdfs-dynamic")) and (.id | contains("!{driverDynamic}"))) | .completed_tasks | .[] | .state' | grep "TASK_KILLED" ' contains 'KILLED'

    #Now kill the process
    #(We send a JSON because the step from cucumber, doesn't support empty posts submissions)
    Then I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    Then I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'
    Then I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/kill/!{driverDynamic}' based on 'schemas/SparkCoverage/streaming_hdfs_dynamic_curl.json' as 'json' with:
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/streaming-hdfs-dynamic-${COVERAGE_VERSION}.jar | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Check exit is clean (may be tasks with TASK_FAILED due to capping executors before were launched totally)
    Then in less than '200' seconds, checking each '10' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-hdfs-dynamic"))) | map(select(.id == "!{driverDynamic}")) | .[] | .state' | grep "TASK_KILLED" | wc -l' contains '1'

    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-hdfs-dynamic"))) | map(select(.id == "!{driverDynamic}")) | .[] | .statuses' | grep "TASK_RUNNING"  | wc -l' contains '1'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-hdfs-dynamic"))) | map(select(.id == "!{driverDynamic}")) | .[] | .statuses' | grep "TASK_KILLED"  | wc -l' contains '1'
