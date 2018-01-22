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
package org.apache.spark.security

import scala.util.{Failure, Success, Try}

import org.apache.spark.internal.Logging
import org.apache.spark.util.Utils

object ConfigSecurity extends Logging {

  val secretsFolder: String = sys.env.get("SPARK_DRIVER_SECRET_FOLDER") match {
    case Some(folder) =>
      logDebug(s"Creating secret folder using driver information in path $folder")
      Utils.createDirectoryByName(folder).getAbsolutePath
    case None =>
      logDebug(s"Creating secret folder for executor")
      Utils.createTempDir(
       s"${sys.env.getOrElse("SPARK_SECRETS_FOLDER", "/tmp")}", "spark").getAbsolutePath
  }

  lazy val vaultToken: Option[String] =

    if (sys.env.get("VAULT_TOKEN").isDefined) {
      logInfo("Obtaining vault token using VAULT_TOKEN")
      sys.env.get("VAULT_TOKEN")
    } else if (sys.env.get("VAULT_TEMP_TOKEN").isDefined) {
      logInfo("Obtaining vault token using VAULT_TEMP_TOKEN")
      scala.util.Try {
        VaultHelper.getRealToken(sys.env.get("VAULT_TEMP_TOKEN"))
      } match {
        case Success(token) => Option(token)
        case Failure(e) =>
          logWarning("An error ocurred while trying to obtain" +
            " Application Token from a temporal token", e)
          None
      }
    } else if (sys.env.get("VAULT_ROLE_ID").isDefined && sys.env.get("VAULT_SECRET_ID").isDefined) {
     logInfo("Obtaining vault token using ROLE_ID and SECRET_ID")
      Option(VaultHelper.getTokenFromAppRole(
        sys.env("VAULT_ROLE_ID"),
        sys.env("VAULT_SECRET_ID")))
    } else {
      logInfo("No Vault token variables provided. Skipping Vault token retrieving")
      None
    }

  lazy val vaultURI: Option[String] = {
    if (sys.env.get("VAULT_PROTOCOL").isDefined
        && sys.env.get("VAULT_HOSTS").isDefined
        && sys.env.get("VAULT_PORT").isDefined) {
      val vaultProtocol = sys.env.get("VAULT_PROTOCOL").get
      val vaultHost = sys.env.get("VAULT_HOSTS").get
      val vaultPort = sys.env.get("VAULT_PORT").get
      Option(s"$vaultProtocol://$vaultHost:$vaultPort")
    } else {
      logInfo("No Vault variables provided")
      None
    }
  }

  def prepareEnvironment: Map[String, String] = {
    logDebug(s"env VAR: ${sys.env.mkString("\n")}")
    val secretOptionsMap = ConfigSecurity.extractSecretFromEnv(sys.env)
    logDebug(s"secretOptionsMap: ${secretOptionsMap.mkString("\n")}")
    loadingConf(secretOptionsMap)
    prepareEnvironment(secretOptionsMap)

  }


  def extractSecretFromEnv(env: Map[String, String]): Map[String,
    Map[String, String]] = {
    val sparkSecurityPrefix = "spark_security_"

    val extract: ((String, String)) => String = (keyValue: (String, String)) => {
      val (key, _) = keyValue

      val securityProp = key.toLowerCase

      if (securityProp.startsWith(sparkSecurityPrefix)) {
        val result = Try(securityProp.split("_")(2))
        result match {
          case Success(value) => value
          case Failure(e) =>
            throw new IllegalArgumentException(
              s"Your SPARK_SECURITY property: $securityProp is malformed")
        }
      } else {
        ""
      }
    }

    env.groupBy(extract).filter(_._2.exists {
      case (key, value) => key.toLowerCase.contains("enable") && value.equals("true")
    })
      .flatMap{case (key, value) =>
        if (key.nonEmpty) Option((key, value.map{case (propKey, propValue) =>
          (propKey.split(sparkSecurityPrefix.toUpperCase).tail.head, propValue)
        }))
        else None
      }
  }

  private def loadingConf(secretOptions: Map[String, Map[String, String]]): Unit = {
    secretOptions.foreach { case (key, options) =>
      key match {
        case "hdfs" =>
          HDFSConfig.prepareEnviroment(options)
          logDebug("Downloaded HDFS conf")
        case _ =>
      }
    }
  }

  private def prepareEnvironment(secretOptions: Map[String,
                                   Map[String, String]]): Map[String, String] = {
    VaultHelper.loadCas
    secretOptions flatMap {
      case ("kerberos", options) =>
        KerberosConfig.prepareEnviroment(options)
      case ("datastore", options) =>
        SSLConfig.prepareEnvironment(SSLConfig.sslTypeDataStore, options)
      case ("db", options) =>
        DBConfig.prepareEnvironment(options)
      case ("mesos", options) =>
        MesosConfig.prepareEnvironment(options)
      case _ => Map.empty[String, String]
    }
  }
}
