# Changelog

## 2.2.0.4 (upcoming)

* Added Tenant Name variable being able to have Spark Dispatcher with different name
* Enable Debug mode
* Support Dynamic allocation with Calico (External shuffle working in HOST mode)

## 2.2.0.3 (December 27, 2017)

* Unify Vault variables
* Secret Broadcast variables (Experimental)

## 2.2.0.2 (December 26, 2017)

* Added mesos constraints management to spark driver
* Added a secure way to retrieve user and passwords information from vault
* History Server could read from HDFS in HA using environment variables
* Added validation to curl parameters


## 2.2.0.1 (November 06, 2017)

* Removed Mesos secret and principal from curls
* Added configurable HDFS timeout

## 2.2.0.0 (September 27, 2017)

* Connection to Elastic with TLS
* Connection to Postgres with TLS, unified in datastore identity
* Removed Kafka identity, unified in datastore identity
* Removed script connection to Postgres 

## 2.1.0.4 (August 17, 2017)

* Spark Dispatcher retrieves Mesos Principal and Secret from Vault

## 2.1.0.3 (July 26, 2017)

* Fix History Server env vars


## 2.1.0.2 (July 25, 2017)

* Dynamic Authentication for History Server
* SDN compatibility and isolation for History Server


## 2.1.0.1 (July 18, 2017)

* Refactor vault variables


## 2.1.0.0 (July 13, 2017)

* Spark-vault interactions
* SDN compatibility and isolation
* Kerberized access to hdfs
* Postgress TLS connection
* Dynamic Authentication
* Stratio Mesos security compatibility
* Initial Stratio Version of Spark
* Forked from Apache Spark 2.1.0