/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.scheduler

import java.security.PrivilegedExceptionAction

import org.apache.hadoop.security.UserGroupInformation

import org.apache.spark.deploy.SparkHadoopUtil
import org.apache.spark.internal.Logging
import org.apache.spark.security.MultiHDFSConfig
import org.apache.spark.util.SerializableConfiguration

object KerberosFunction extends Logging {

  def executeSecure[U, T](tokens: Option[Array[Byte]], funct: (U => T), inputParameters: U): T = {
    if (tokens.isDefined) {
      KerberosUtil.useTokenAuth(tokens.get)
    }
    funct(inputParameters)
  }

  def executeWithUgi[T](ugi: Option[UserGroupInformation])(funct: => T): T = {
    ugi.map { ug =>
      logDebug(s"Executing function with ugi: ${ug.getUserName}")
      ug.doAs(new PrivilegedExceptionAction[T] {
        override def run() =
          funct
      })
    }.getOrElse {

      if (UserGroupInformation.isSecurityEnabled) {
        logDebug(s"Not found ugi, so executing with " +
          s"default ugi: ${UserGroupInformation.getCurrentUser.getUserName}")
      } else {
        logDebug("HDFS without security. Executing normally")
      }

      funct
    }
  }

  def executeSecure[U, T](
                           conf: SerializableConfiguration
                         )(funct: U => T): (U => T) = (params: U) => {

    val hadoopConf = conf.value

    val hdfsHost = MultiHDFSConfig.extractHDFSHostFromConf(hadoopConf)

    logDebug(s"Executing secure HDFS in host: $hdfsHost")

    val ugi = SparkHadoopUtil.get.getCredentials(hdfsHost)


    ugi.map { ug =>

      logDebug(s"Using delegation tokens for user ${ug.getUserName}")
      KerberosUtil.useUgi(ug, conf.value)
      logDebug("And using an ugi for if its needed")
      ug.doAs(new PrivilegedExceptionAction[T] {
        override def run(): T = funct(params)
      })

    }.getOrElse {

      if (UserGroupInformation.isSecurityEnabled) {
        logDebug(s"Not found delegation tokens for host $hdfsHost. " +
          s"Executing with default ugi: ${UserGroupInformation.getCurrentUser.getUserName}")
      } else {
        logDebug("HDFS without security. Executing normally")
      }

      funct(params)
    }

  }

}