@rest
Feature: [Install Spark Coverage] Installing Spark Coverage

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'

  Scenario:[Spark dispatcher Installation][01] Installing Spark Coverage (for testing purposes)
    Given I create file 'SparkCoverage.json' based on 'schemas/SparkCoverage/SparkCoverage.json' as 'json' with:
      |   $.container.docker.image  |  UPDATE  | ${SPARK_COVERAGE_IMAGE}:${COVERAGE_VERSION} | n/a     |

    When I outbound copy 'target/test-classes/SparkCoverage.json' through a ssh connection to '/dcos'

    And I run 'dcos marathon app add /dcos/SparkCoverage.json' in the ssh connection

    Then in less than '500' seconds, checking each '20' seconds, the command output 'dcos task | grep spark-coverage | grep R | wc -l' contains '1'
    And in less than '300' seconds, checking each '20' seconds, the command output 'dcos marathon task list spark-coverage | grep spark-coverage | awk '{print $2}'' contains 'True'
    And I run 'dcos marathon task list spark-coverage | awk '{print $5}' | grep spark-coverage | head -n 1' in the ssh connection and save the value in environment variable 'sparkTaskId'
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkTaskId} | grep TASK_RUNNING | wc -l' contains '1'
    Then in less than '300' seconds, checking each '10' seconds, the command output 'dcos marathon task show !{sparkTaskId} | grep TASK_RUNNING | wc -l' contains '1'