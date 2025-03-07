package ru.mipt.npm.nica.ems

import JwtParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import jakarta.ws.rs.NotAuthorizedException
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.AccessTokenResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray

@Serializable
data class KCUserInfo(
    val sub: String,
    val email_verified: Boolean,
    val name: String,
    val groups: List<String>? = listOf(),   // in case `groups` is not present in JSON, pass empty list
    val preferred_username: String,
    val given_name: String,
    val family_name: String,
    val email: String
)

fun getKCtoken(config: ConfigFile, username: String, pass: String): String? {
    val keycloak = Keycloak.getInstance(
        config.keycloak_auth?.server_url,
        config.keycloak_auth?.realm,
        username,
        pass,
        config.keycloak_auth?.client_id,
        config.keycloak_auth?.client_secret,
        null,
        null,
        false,
        null,
        "openid"  // required for userinfo endpoint to work
    )

    var token: AccessTokenResponse? = null
    try {
        token = keycloak.tokenManager().grantToken()
        // println(token.token)
    } catch (e: NotAuthorizedException) {
        debug("Authentication failed, no token obtained!")
        return null
    }
    return token!!.token .also{ debug("getKCtoken: token was obtained") }  //.also { println(it) }
}

suspend fun getKCgroups(config: ConfigFile, token: String): List<String> {
    val parser = JwtParser()
    val jsonToken = parser.parseToJsonObject(token)
    // debug(jsonToken?.get("groups"))
    val res = mutableListOf<String>()
    jsonToken?.get("groups")?.jsonArray?.forEach {
        // debug(it)
        res.add((it.toString().replace("/", "").replace("\"", "")))
    }
    // debug(res)
    return res
}

suspend fun getKCPrincipalOrNull(config: ConfigFile, username: String, pass: String): Principal? {
    val token = getKCtoken(config, username, pass) ?: return null
    val groups = getKCgroups(config, token)
    return UserIdPwGroupsPrincipal(username, pass, groups, rolesFromGroups(config, groups))
}

fun rolesFromGroups(config: ConfigFile, groups: List<String>): UserRoles {
    val res = UserRoles(
        isReader = true,      // any authenticated user
        isWriter = groups.contains(config.keycloak_auth?.writer_group_name), // e.g. "bmneventwriter", can add records
        isAdmin = groups.contains(config.keycloak_auth?.admin_group_name)    // e.g. "bmneventadmin", can delete records
    )
    // println("res = $res")
    return res
}
