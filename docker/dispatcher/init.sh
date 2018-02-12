#!/bin/bash

set -e

export DISPATCHER_PORT=${PORT0}
export DISPATCHER_UI_PORT=${PORT1}

# determine scheme and derive WEB
SCHEME=http
OTHER_SCHEME=https
if [[ "${SPARK_SSL_ENABLED}" == true ]]; then
	SCHEME=https
	OTHER_SCHEME=http
fi

export DISPATCHER_UI_WEB_PROXY_BASE="/service/${DCOS_SERVICE_NAME}"


#Start spark
/etc/service/spark/run
