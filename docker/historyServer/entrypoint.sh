#!/bin/bash

source commons.sh

# Create krb5.conf file
HADOOP_CONF_DIR=${HADOOP_CONF_DIR:=/tmp/hadoop}
SPARK_HISTORY_OPTS=${SPARK_HISTORY_OPTS:=""}

function set_log_level() {
    if [ ! -z "$SPARK_LOG_LEVEL" ]; then
        sed "s,log4j.rootCategory=INFO,log4j.rootCategory=${SPARK_LOG_LEVEL}," \
            /opt/sds/spark/conf/log4j.properties.template > /opt/sds/spark/conf/log4j.properties
    else
        cp /opt/sds/spark/conf/log4j.properties.template /opt/sds/spark/conf/log4j.properties
    fi
}

function main() {
   VAULT_PORT=${VAULT_PORT:=8200}
   VAULT_URI="$VAULT_PROTOCOL://$VAULT_HOSTS:$VAULT_PORT"

   SPARK_HOME=/opt/sds/spark

   mkdir -p $HADOOP_CONF_DIR

   set_log_level

   SPARK_HISTORY_OPTS="-Dspark.history.fs.logDirectory=${HISTORY_SERVER_LOG_DIR} ${SPARK_HISTORY_OPTS}"

   SPARK_HISTORY_OPTS="-Dspark.history.ui.port=${PORT0} ${SPARK_HISTORY_OPTS}" $SPARK_HOME/bin/spark-class org.apache.spark.deploy.history.HistoryServer
}

main