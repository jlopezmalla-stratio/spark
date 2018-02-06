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

import org.apache.spark.internal.Logging

object VaultHelper extends Logging {

  lazy val jsonTempTokenTemplate: String = "{ \"token\" : \"_replace_\" }"
  lazy val jsonRoleSecretTemplate: String = "{ \"role_id\" : \"_replace_role_\"," +
    " \"secret_id\" : \"_replace_secret_\"}"

  def getTokenFromAppRole(roleId: String,
                          secretId: String): String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/login"
    logDebug(s"Requesting login from app and role: $requestUrl")
    val replace: String = jsonRoleSecretTemplate.replace("_replace_role_", roleId)
      .replace("_replace_secret_", secretId)
    logDebug(s"getting secret: $secretId and role: $roleId")
    logDebug(s"generated JSON: $replace")
    val jsonAppRole = replace
    HTTPHelper.executePost(requestUrl, "auth",
      None, Some(jsonAppRole))("client_token").asInstanceOf[String]
  }

  def loadCas: Unit = {
    if (ConfigSecurity.vaultURI.isDefined) {
      val (caFileName, caPass) = getAllCaAndPassword
      HTTPHelper.secureClient =
        Some(HTTPHelper.generateSecureClient(caFileName, caPass))
    }
  }

  private def getAllCaAndPassword: (String, String) = {
    val cas = getAllCas
    val caPass = getCAPass
    (SSLConfig.generateTrustStore("ca-trust", cas, caPass), caPass)
  }

  private[security] def getCAPass: String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/passwords/?list=true"
    logDebug(s"Requesting ca-trust certificates passwords list from Vault: $requestUrl")
    val passPath = HTTPHelper.executeGet(requestUrl, "data",
      Some(Seq(("X-Vault-Token",
        ConfigSecurity.vaultToken.get))))("keys").asInstanceOf[List[String]].head
    logInfo("*************************")
    logInfo(s"$passPath passPath")
    logInfo("*************************")
    val requestPassUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/" +
      s"passwords/${passPath.replaceAll("/", "")}/keystore"
    logDebug(s"Requesting ca Pass from Vault: $requestPassUrl")
    HTTPHelper.executeGet(requestPassUrl, "data",
      Some(Seq(("X-Vault-Token",
        ConfigSecurity.vaultToken.get))))(s"pass").asInstanceOf[String]
  }

  def getRoleIdFromVault(role: String): String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/role/$role/role-id"

    logDebug(s"Requesting Role ID from Vault: $requestUrl")
    HTTPHelper.executeGet(requestUrl, "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get))))("role_id").asInstanceOf[String]
  }

  def getSecretIdFromVault(role: String): String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/auth/approle/role/$role/secret-id"

    logDebug(s"Requesting Secret ID from Vault: $requestUrl")
    HTTPHelper.executePost(requestUrl, "data",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get))))("secret_id").asInstanceOf[String]
  }

  def getTemporalToken: String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/sys/wrapping/wrap"
    logDebug(s"Requesting temporal token: $requestUrl")

    val jsonToken = jsonTempTokenTemplate.replace("_replace_", ConfigSecurity.vaultToken.get)

    HTTPHelper.executePost(requestUrl, "wrap_info",
      Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get),
        ("X-Vault-Wrap-TTL", sys.env.get("VAULT_WRAP_TTL")
        .getOrElse("2000")))), Some(jsonToken))("token").asInstanceOf[String]
  }

  def getKeytabPrincipalFromVault(vaultPath: String): (String, String) = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"
    logDebug(s"Requesting Keytab and principal: $requestUrl")
    val data = HTTPHelper.executeGet(requestUrl, "data", Some(Seq(("X-Vault-Token",
      ConfigSecurity.vaultToken.get))))
    val keytab64 = data.find(_._1.contains("keytab")).get._2.asInstanceOf[String]
    val principal = data.find(_._1.contains("principal")).get._2.asInstanceOf[String]
    (keytab64, principal)
  }

  def getPassPrincipalFromVault(vaultPath: String): (String, String) = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"
    logDebug(s"Requesting user and pass: $requestUrl")
    val data = HTTPHelper.executeGet(requestUrl, "data", Some(Seq(("X-Vault-Token",
      ConfigSecurity.vaultToken.get))))
    val pass = data.find(_._1.contains("pass")).get._2.asInstanceOf[String]
    val principal = data.find(_._1.contains("user")).get._2.asInstanceOf[String]
    (pass, principal)
  }

  def getTrustStore(certVaultPath: String): String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$certVaultPath"
    val truststoreVaultPath = s"$requestUrl"

    logDebug(s"Requesting truststore: $truststoreVaultPath")
    val data = HTTPHelper.executeGet(requestUrl,
      "data", Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get))))
    val trustStore = data.find(_._1.endsWith("_crt")).get._2.asInstanceOf[String]
    trustStore
  }

  def getCertPassForAppFromVault(appPassVaulPath: String): String = {
    logDebug(s"Requesting Cert Pass For App: $appPassVaulPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$appPassVaulPath"
    HTTPHelper.executeGet(requestUrl,
      "data", Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get)))
    )("pass").asInstanceOf[String]
  }

  def getCertKeyForAppFromVault(vaultPath: String): (String, String) = {
    logDebug(s"Requesting Cert Key For App: $vaultPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$vaultPath"
    val data = HTTPHelper.executeGet(requestUrl,
      "data", Some(Seq(("X-Vault-Token", ConfigSecurity.vaultToken.get))))
    val certs = data.find(_._1.endsWith("_crt")).get._2.asInstanceOf[String]
    val key = data.find(_._1.endsWith("_key")).get._2.asInstanceOf[String]
    (key, certs)
  }

  def getRealToken(vaultTempToken: Option[String]): String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/sys/wrapping/unwrap"
    logDebug(s"Requesting real Token: $requestUrl")
    HTTPHelper.executePost(requestUrl,
      "data", Some(Seq(("X-Vault-Token", vaultTempToken.get))))("token").asInstanceOf[String]
  }

  def retrieveSecret(secretVaultPath: String, idJSonSecret: String): String = {
    logDebug(s"Retriving Secret: $secretVaultPath")
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/$secretVaultPath"

    HTTPHelper.executeGet(requestUrl,
      "data", Some(Seq(("X-Vault-Token",
      ConfigSecurity.vaultToken.get))))(idJSonSecret).asInstanceOf[String]
  }

  private [security] def getAllCas: String = {
    val requestUrl = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/certificates/?list=true"
    val requestCA = s"${ConfigSecurity.vaultURI.get}/v1/ca-trust/certificates/"
    logDebug(s"Requesting ca-trust certificates list from Vault: $requestUrl")
    val keys = HTTPHelper.executeGet(requestUrl, "data",
      Some(Seq(("X-Vault-Token",
        ConfigSecurity.vaultToken.get))))("keys").asInstanceOf[List[String]]

    keys.flatMap(key => {
      logInfo(s"Requesting CAS for $requestCA$key")
      HTTPHelper.executeGet(s"$requestCA$key",
        "data", Some(Seq(("X-Vault-Token",
          ConfigSecurity.vaultToken.get))))
      (s"${key}_crt")
    }).mkString
  }

}
