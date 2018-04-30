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

package org.apache.spark.deploy.security

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.mapred.Master
import org.apache.hadoop.security.{Credentials, UserGroupInformation}
import org.apache.hadoop.security.token.delegation.AbstractDelegationTokenIdentifier
import org.apache.spark.{SparkConf, SparkException}
import org.apache.spark.internal.Logging
import org.apache.spark.internal.config._
import org.apache.spark.scheduler.KerberosUser
import org.apache.spark.security.{HDFSConfig, MultiHDFSConfig}

private[deploy] class HadoopFSDelegationTokenProvider(fileSystems: Configuration => Set[FileSystem])
  extends HadoopDelegationTokenProvider with Logging {

  // This tokenRenewalInterval will be set in the first call to obtainDelegationTokens.
  // If None, no token renewer is specified or no token can be renewed,
  // so we cannot get the token renewal interval.
  private var tokenRenewalInterval: Option[Long] = null

  override val serviceName: String = "hadoopfs"

  override def obtainDelegationTokens(
                                       hadoopConf: Configuration,
                                       sparkConf: SparkConf,
                                       creds: Credentials): Option[Long] = {

    val fsToGetTokens = fileSystems(hadoopConf)
    val fetchCreds = fetchDelegationTokens(getTokenRenewer(hadoopConf), fsToGetTokens, creds)

    // Get the token renewal interval if it is not set. It will only be called once.
    if (tokenRenewalInterval == null) {
      tokenRenewalInterval = getTokenRenewalInterval(hadoopConf, sparkConf, fsToGetTokens)
    }

    // Get the time of next renewal.
    val nextRenewalDate = tokenRenewalInterval.flatMap { interval =>
      val nextRenewalDates = fetchCreds.getAllTokens.asScala
        .filter(_.decodeIdentifier().isInstanceOf[AbstractDelegationTokenIdentifier])
        .map { token =>
          val identifier = token
            .decodeIdentifier()
            .asInstanceOf[AbstractDelegationTokenIdentifier]
          identifier.getIssueDate + interval
        }
      if (nextRenewalDates.isEmpty) None else Some(nextRenewalDates.min)
    }

    logInfo(s"Next renewal date: $nextRenewalDate")

    nextRenewalDate
  }

  override def delegationTokensRequired(
                                         sparkConf: SparkConf,
                                         hadoopConf: Configuration): Boolean = {
    UserGroupInformation.isSecurityEnabled
  }

  private def getTokenRenewer(hadoopConf: Configuration): String = {

    val host = MultiHDFSConfig.extractHDFSHostFromConf(hadoopConf)

    // Is the same as the base ugi, so set the renewer to the current user
    if(host == HDFSConfig.getBaseHost) {

      val renewer = KerberosUser.baseUgiAndConf.get._1.getUserName
      logInfo(s"Renewer for base HDFS: $renewer")
      renewer

    } else {

      // Check multiHDFS configurations. Or leave default behaviour from Spark
      MultiHDFSConfig.getUgiForHost(
        MultiHDFSConfig.extractHDFSHostFromConf(hadoopConf)
      ).map { ugi =>
        val renewer = ugi.getUserName
        logInfo(s"Found ugi for multi hdfs, returning renewer: $ugi")
        renewer
      }.getOrElse {

        val tokenRenewer = Master.getMasterPrincipal(hadoopConf)
        logDebug("Delegation token renewer is: " + tokenRenewer)

        if (tokenRenewer == null || tokenRenewer.length() == 0) {
          val errorMessage = "Can't get Master Kerberos principal for use as renewer."
          logError(errorMessage)
          throw new SparkException(errorMessage)
        }

        tokenRenewer
      }
    }

  }

  private def fetchDelegationTokens(
                                     renewer: String,
                                     filesystems: Set[FileSystem],
                                     creds: Credentials): Credentials = {

    filesystems.foreach { fs =>
      logDebug("getting token for: " + fs)
      fs.addDelegationTokens(renewer, creds)
    }

    creds
  }

  private def getTokenRenewalInterval(
                                       hadoopConf: Configuration,
                                       sparkConf: SparkConf,
                                       filesystems: Set[FileSystem]): Option[Long] = {

    val renewer = getTokenRenewer(hadoopConf)
    logDebug(s"Found renewer: $renewer")

    val creds = new Credentials()
    fetchDelegationTokens(renewer, filesystems, creds)

    val renewIntervals = creds.getAllTokens.asScala.filter {
      _.decodeIdentifier().isInstanceOf[AbstractDelegationTokenIdentifier]
    }.flatMap { token =>

      logInfo(s"Token : ${token.getKind.toString} to check renewal")

      val tryRenew = Try {
        logInfo("Getting new expiration")
        val newExpiration = token.renew(hadoopConf)
        logInfo(s"New expiration is: $newExpiration")
        val identifier = token.decodeIdentifier().asInstanceOf[AbstractDelegationTokenIdentifier]
        val interval = newExpiration - identifier.getIssueDate
        logInfo(s"Renewal interval is $interval for token ${token.getKind.toString}")
        interval
      }

      tryRenew match {
        case Failure(e) =>
          logError("Error getting expiration", e)
        case _ =>
      }

      tryRenew.toOption
    }
    if (renewIntervals.isEmpty) None else Some(renewIntervals.min)

  }
}