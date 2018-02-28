@rest
Feature: [Install Spark Shuffle] Installing Spark Shuffle

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'

  Scenario:[Spark Shuffle Installation][01]Basic Installation Spark Shuffle
    Given I create file 'SparkShuffleInstallation.json' based on 'schemas/SparkShuffle/BasicSparkShuffle.json' as 'json' with:
      | $.service.name | UPDATE | spark-shuffle | n/a |

    #Copy DEPLOY JSON to DCOS-CLI
    When I outbound copy 'target/test-classes/SparkShuffleInstallation.json' through a ssh connection to '/dcos'

    #Start image from JSON
    And I run 'dcos package describe --app --options=/dcos/SparkShuffleInstallation.json spark-shuffle > /dcos/SparkShuffleInstallationMarathon.json' in the ssh connection
    And I run 'sed -i -e 's|"image":.*|"image": "${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION}",|g' /dcos/SparkShuffleInstallationMarathon.json' in the ssh connection
    And I run 'dcos marathon app add /dcos/SparkShuffleInstallationMarathon.json' in the ssh connection

    #Wait 120 seconds to launch all the spark-shuffle instances (Download from docker image)
    And I run 'sleep 120' in the ssh connection

    #Get instances launched
    Then I run 'dcos marathon task list spark-shuffle | grep spark-shuffle | wc -l' in the ssh connection and save the value in environment variable 'sparkShuffleInstances'

    #Check Spark Shuffle is Running
    Then in less than '500' seconds, checking each '20' seconds, the command output 'dcos task | grep spark-shuffle | grep R | wc -l' contains '!{sparkShuffleInstances}'
    #Check are healthy
    Then in less than '500' seconds, checking each '20' seconds, the command output 'dcos marathon task list spark-shuffle | grep spark-shuffle | awk '{print $2}' | wc -l' contains '!{sparkShuffleInstances}'