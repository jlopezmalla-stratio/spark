@rest
Feature: [Download Docker Images] Downloading images

  Background:
    Given I open a ssh connection to '${DCOS_CLI_HOST}' with user 'root' and password 'stratio'

  Scenario:[Download images][01]Downloading docker images
    Then I execute the command 'sudo docker pull ${SPARK_DOCKER_IMAGE}:${STRATIO_SPARK_VERSION}' in all the nodes of my cluster with user 'operador' and pem '${PEM_PATH}'
    Then I execute the command 'sudo docker pull qa.stratio.com/stratio/spark-stratio-history-server:${STRATIO_SPARK_VERSION}' in all the nodes of my cluster with user 'operador' and pem '${PEM_PATH}'

