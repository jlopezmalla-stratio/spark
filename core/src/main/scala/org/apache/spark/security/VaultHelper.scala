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



object VaultHelper extends Logging {

  lazy val jsonTempTokenTemplate: String = "{ \"token\" : \"_replace_\" }"
  lazy val jsonRoleSecretTemplate: String = "{ \"role_id\" : \"_replace_role_\"," +
    " \"secret_id\" : \"_replace_secret_\"}"

  def getTokenFromAppRole(roleId: String,
                          secretId: String): Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/login"
    logDebug(s"Requesting login from app and role: $requestUrl")

    val replace: String = jsonRoleSecretTemplate.replace("_replace_role_", roleId)
      .replace("_replace_secret_", secretId)
    logDebug(s"getting secret: $secretId and role: $roleId")

    logDebug(s"generated JSON: $replace")
    val jsonAppRole = replace

    HTTPHelper.executePost(
      requestUrl,
      "auth",
      None,
      Some(jsonAppRole)
    ).map(_("client_token").asInstanceOf[String])
  }

  def loadCas: Unit = {
    if (ConfigSecurity.vaultURI.isDefined) {

      val (caFileName, caPass) = getAllCaAndPassword.get
      log.debug("Retrieved correctly CAs and passwords from vault")

      HTTPHelper.secureClient =
        Some(HTTPHelper.generateSecureClient(caFileName, caPass))

    }
  }

  private def getAllCaAndPassword: Try[(String, String)] =
    (
      for {
        cas <- getAllCas
        caPass <- getCAPass
      } yield {

        (SSLConfig.generateTrustStore("ca-trust", cas, caPass), caPass)

      }) recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving CAs/passwords from vault", e))
    }


  private[security] def getCAPass: Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/passwords/?list=true"
    logDebug(s"Requesting ca-trust certificates passwords list from Vault: $requestUrl")

      HTTPHelper.executeGet(
        requestUrl,
        "data",
        Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
      ).flatMap{ listResponse =>

        val passPath = listResponse("keys").asInstanceOf[List[String]].head
        val requestPassUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/" +
          s"passwords/${passPath.replaceAll("/", "")}/keystore"

        logDebug(s"Requesting ca Pass from Vault: $requestPassUrl")

        HTTPHelper.executeGet(
          requestPassUrl,
          "data", Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
        ).map { passResponse =>
          passResponse(s"pass").asInstanceOf[String]
        }
      } recoverWith {
        case e: Exception =>
          Failure(new Exception("Error retrieving CA password from Vault", e))
      }
  }

  def getRoleIdFromVault(role: String): Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/role/$role/role-id"
    logDebug(s"Requesting Role ID from Vault: $requestUrl")

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving Role ID from Vault", e))

    }.map(_("role_id").asInstanceOf[String])
  }

  def getSecretIdFromVault(role: String): Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/role/$role/secret-id"
    logDebug(s"Requesting Secret ID from Vault: $requestUrl")

    HTTPHelper.executePost(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving Secret ID from Vault", e))

    }.map(_("secret_id").asInstanceOf[String])
  }

  def getTemporalToken: Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/sys/wrapping/wrap"
    logDebug(s"Requesting temporal token: $requestUrl")

    val jsonToken = jsonTempTokenTemplate.replace("_replace_", ConfigSecurity.vaultToken.get)

    HTTPHelper.executePost(
      requestUrl,
      "wrap_info",
      Some(
        Seq(
          ("X-Vault-Token", ConfigSecurity.vaultToken.get),
          ("X-Vault-Wrap-TTL", sys.env.getOrElse("VAULT_WRAP_TTL", "2000"))
        )
      ),
      Some(jsonToken)
    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving temporal token from Vault", e))

    }.map(_ ("token").asInstanceOf[String])
  }

  def getKeytabPrincipalFromVault(vaultPath: String): Try[(String, String)] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"
    logDebug(s"Requesting Keytab and principal: $requestUrl")

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving keytab and principal from Vault", e))

    }.map { vaultResponse =>

      val keytab64 = vaultResponse.find(_._1.contains("keytab")).get._2.asInstanceOf[String]
      val principal = vaultResponse.find(_._1.contains("principal")).get._2.asInstanceOf[String]
      (keytab64, principal)

    }
  }

  def getPassPrincipalFromVault(vaultPath: String): Try[(String, String)] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"
    logDebug(s"Requesting user and pass: $requestUrl")

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving user and password from vault", e))

    }.map { vaultResponse =>

      val pass = vaultResponse.find(_._1.contains("pass")).get._2.asInstanceOf[String]
      val principal = vaultResponse.find(_._1.contains("user")).get._2.asInstanceOf[String]
      (pass, principal)

    }
  }

  def getTrustStore(certVaultPath: String): Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$certVaultPath"
    val truststoreVaultPath = s"$requestUrl"
    logDebug(s"Requesting truststore: $truststoreVaultPath")

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving truststore from Vault", e))

    }.map{ vaultResponse =>
      vaultResponse.find(_._1.endsWith("_crt")).get._2.asInstanceOf[String]
    }
  }

  def getCertPassForAppFromVault(appPassVaulPath: String): Try[String] = {

    logDebug(s"Requesting Cert Pass For App: $appPassVaulPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$appPassVaulPath"

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))

    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving certificate password from vault", e))

    }.map(_("pass").asInstanceOf[String])
  }

  def getCertKeyForAppFromVault(
                                 vaultPath: String,
                                 certName: Option[String] = None
                               ): Try[(String, String)] = {

    logDebug(s"Retrieving certificate from path: $vaultPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving certificate from Vault", e))

    }.map{ vaultResponse =>

      certName map { certToSelect =>
        logDebug(s"Selecting certificate selected by user: $certToSelect")
        val certs = vaultResponse(s"${certName}_crt").asInstanceOf[String]
        val key = vaultResponse(s"${certName}_key").asInstanceOf[String]
        (key, certs)
      } getOrElse {
        val certs = vaultResponse.find(_._1.endsWith("_crt")).get._2.asInstanceOf[String]
        val key = vaultResponse.find(_._1.endsWith("_key")).get._2.asInstanceOf[String]
        (key, certs)
      }

    }

  }

  def getRealToken(vaultTempToken: Option[String]): Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/sys/wrapping/unwrap"
    logDebug(s"Requesting real Token: $requestUrl")

    HTTPHelper.executePost(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", vaultTempToken.get)))
    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving real token from Vault", e))

    }.map(_("token").asInstanceOf[String])
  }

  def retrieveSecret(secretVaultPath: String, idJSonSecret: String): Try[String] = {

    logDebug(s"Retriving Secret: $secretVaultPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$secretVaultPath"

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
    ).recoverWith {

      case e: Exception =>
        Failure(new Exception("Error retrieving secret from Vault"))

    }.map(_(idJSonSecret).asInstanceOf[String])
  }

  private[security] def getAllCas: Try[String] = {

    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/certificates/?list=true"
    val requestCA = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/certificates/"
    logDebug(s"Requesting ca-trust certificates list from Vault: $requestUrl")

    HTTPHelper.executeGet(
      requestUrl,
      "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
    ).map { listCAsResponse =>

      val keys = listCAsResponse("keys").asInstanceOf[List[String]]

      val casRetrieved: List[String] = keys.map { key =>
        logDebug(s"Requesting CAS for $requestCA$key")
        HTTPHelper.executeGet(
          s"$requestCA$key",
          "data",
          Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
        ).map(_ (s"${key}_crt").asInstanceOf[String]).get
      }

      casRetrieved.mkString

    }.recoverWith {
      case e: Exception =>
        Failure(new Exception("Error retrieving All CAs from vault", e))
    }
  }
}
