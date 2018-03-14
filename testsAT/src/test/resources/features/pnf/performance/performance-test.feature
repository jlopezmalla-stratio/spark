@rest
Feature: [Spark Performance Tests] Spark performance tests

  Background:
    #Show parameters of execution
    Given I run 'echo Cores: !{CORES}' locally
    Given I run 'echo Cores per executor: !{CORES_PER_EXECUTOR}' locally
    Given I run 'echo Dataset: !{DATASET_PATH}' locally
    Given I run 'echo Executor memory: !{EXECUTOR_MEMORY}' locally
    Given I run 'echo Queries: ${QUERIES}' locally
    Given I run 'echo Iterations: ${ITERATIONS}' locally

    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    #Check dispatcher and spark-coverage are deployed
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

    #Clean all drivers from spark-dispatcher
    Then I clean all the drivers in the dispatcher with name '${SPARK_FW_NAME}' in dcos host '${CLUSTER_ID}.labs.stratio.com' with mesos master '!{MESOS_MASTER}:5050' with user 'admin' and password '1234'


  Scenario:[Spark Performance][01] Launch Spark TPCDS JOB to measure performance
    Given I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    And I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

    #Now launch the work
    Given I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    And I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'

    When I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/create' based on 'schemas/pnf/performance/tpcds_curl.json' as 'json' with:
      |   $.appArgs[0]  |  UPDATE  | !{DATASET_PATH} | n/a     |
      |   $.appArgs[1]  |  UPDATE  | ${QUERIES} | n/a     |
      |   $.appArgs[2]  |  UPDATE  | ${ITERATIONS} | n/a     |
      |   $.appArgs[3]  |  UPDATE  | /tmp | n/a     |
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/tpcds-queries-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.jars']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/tpcds-queries-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.mesos.executor.docker.image']  |  UPDATE  | ${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION} | n/a     |
      |   $.sparkProperties['spark.mesos.driverEnv.SPARK_SECURITY_HDFS_CONF_URI']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/configs/${CLUSTER_ID} | n/a     |
      |   $.sparkProperties['spark.executor.cores']  |  UPDATE  | !{CORES_PER_EXECUTOR} | n/a     |
      |   $.sparkProperties['spark.cores.max']  |  UPDATE  | !{CORES} | n/a     |
      |   $.sparkProperties['spark.executor.memory']  |  UPDATE  | !{EXECUTOR_MEMORY} | n/a     |
      |   $.sparkProperties['spark.eventLog.enabled']  |  UPDATE  | !{HS_ENABLED} | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Save the driver launched id
    Then I save the value from field in service response 'submissionId' in variable 'driverJobQuery'

    #Wait until finished
    Then in less than '3600' seconds, checking each '10' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("Spark-TCP-DS"))) | map(select(.id == "!{driverJobQuery}")) | .[] | .id' | grep "!{driverJobQuery}"' contains '!{driverJobQuery}'

    #Check finished correctly
    Then in less than '3600' seconds, checking each '40' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("Spark-TCP-DS"))) | map(select(.id == "!{driverJobQuery}")) | .[] | .state' | grep "TASK_FINISHED" | wc -l' contains '1'

    #Check output is correct
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'dcos task log !{driverJobQuery} stdout --lines=3000 --completed | grep "End of process" | wc -l' contains '1'

    #Check has finished correctly
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("Spark-TCP-DS"))) | map(select(.id == "!{driverJobQuery}")) | .[] | .statuses' | grep "TASK_FAILED"  | wc -l' contains '0'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("Spark-TCP-DS"))) | map(select(.id == "!{driverJobQuery}")) | .[] | .statuses' | grep "TASK_RUNNING"  | wc -l' contains '1'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("Spark-TCP-DS"))) | map(select(.id == "!{driverJobQuery}")) | .[] | .statuses' | grep "TASK_FINISHED"  | wc -l' contains '1'