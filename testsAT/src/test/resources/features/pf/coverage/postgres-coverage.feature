@rest
Feature: [Spark Postgres Coverage] Postgres Coverage tests

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    #Check dispatcher and spark-coverage are deployed
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep "${SPARK_FW_NAME}\." | grep R | wc -l' contains '1'
    Then in less than '20' seconds, checking each '10' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'

  Scenario:[Spark Postgres coverage][01] Deploy Postgres spark job

    #Obtain mesos master
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    Given I run 'getent hosts leader.mesos | awk '{print $1}'' in the ssh connection and save the value in environment variable 'MESOS_MASTER'

    #Obtain vault token
    Given I open a ssh connection to '${BOOTSTRAP_IP}' with user 'root' and password 'stratio'
    Then I run 'jq -r .root_token /stratio_volume/vault_response' in the ssh connection with exit status '0' and save the value in environment variable 'vaultToken'

    #Deploy secret in vault
    Given I open a ssh connection to '${DCOS_IP}' with user 'root' and password 'stratio'
    And I outbound copy 'target/test-classes/schemas/pf/SparkCoverage/spark_postgres_cert.json' through a ssh connection to '/tmp'
    And I run 'curl -k -H "X-Vault-Token:!{vaultToken}" -H "Content-Type:application/json" -X POST -d "@/tmp/spark_postgres_cert.json" https://vault.service.paas.labs.stratio.com:8200/v1/userland/certificates/spark-postgrestest' in the ssh connection with exit status '0'


    #Now launch the work
    Given I set sso token using host '${CLUSTER_ID}.labs.stratio.com' with user 'admin' and password '1234'
    And I securely send requests to '${CLUSTER_ID}.labs.stratio.com:443'

    When I send a 'POST' request to '/service/${SPARK_FW_NAME}/v1/submissions/create' based on 'schemas/pf/SparkCoverage/postgres_curl.json' as 'json' with:
      |   $.appResource  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/postgres-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.jars']  |  UPDATE  | http://spark-coverage.marathon.mesos:9000/jobs/postgres-${COVERAGE_VERSION}.jar | n/a     |
      |   $.sparkProperties['spark.mesos.executor.docker.image']  |  UPDATE  | ${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION} | n/a     |
      |   $.appArgs[0]  |  UPDATE  | ${POSTGRES_INSTANCE} | n/a     |

    Then the service response status must be '200' and its response must contain the text '"success" : true'

    #Save the driver launched id
    Then I save the value from field in service response 'submissionId' in variable 'driverPostgres'

    #Check output is correct
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverPostgres} stdout --lines=1000 --completed | grep "WRITING" | wc -l' contains '1'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverPostgres} stdout --lines=1000 --completed | grep "READING DATA" | wc -l' contains '1'
    Then in less than '200' seconds, checking each '10' seconds, the command output 'dcos task log !{driverPostgres} stdout --lines=1000 --completed | grep "|numbers|" | wc -l' contains '1'

    #Wait until finished
    Then in less than '200' seconds, checking each '10' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-postgres"))) | map(select(.id == "!{driverPostgres}")) | .[] | .state' | grep "TASK_FINISHED" | wc -l' contains '1'

    #Check has finished correctly
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-postgres"))) | map(select(.id == "!{driverPostgres}")) | .[] | .statuses' | grep "TASK_FAILED"  | wc -l' contains '0'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-postgres"))) | map(select(.id == "!{driverPostgres}")) | .[] | .statuses' | grep "TASK_RUNNING"  | wc -l' contains '1'
    Then in less than '10' seconds, checking each '5' seconds, the command output 'curl -s !{MESOS_MASTER}:5050/frameworks | jq '.frameworks[] | select(.name == "${SPARK_FW_NAME}") | .completed_tasks |  map(select(.name | contains ("AT-postgres"))) | map(select(.id == "!{driverPostgres}")) | .[] | .statuses' | grep "TASK_FINISHED"  | wc -l' contains '1'