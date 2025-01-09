package ru.mipt.npm.nica.ems

import kotlinx.serialization.Serializable

val DEFAULT_LIMIT_FOR_WEB = 1000

/**
 * Determines configuration file structure
 *
 * Note: Backend part currently uses Jackson (not kotlinx) for (de)serialization;
 *       but frontend actually makes use of @Serializable stuff
 *
 */
@Serializable
class ConfigFile(
    val event_db: DBConnectionConfig,
    val condition_db: DBConnectionConfig?,

    // If database_auth is set, user:password from user request are used for database connection
    // in this case keycloak_auth is not used
    // If both are not set, authentication is disabled completely
    val database_auth: Boolean?,
    val keycloak_auth: KeyCloakAuthConfig?,

    val title: String,
    val pages: List<PageConfig>,

    // If true, API will log requests extensively
    val debug: Boolean? = false
)

@Serializable
class DBConnectionConfig(
    val host: String,
    val port: Int,
    val db_name: String,
    val user: String,
    val password: String
)

@Serializable
class PageConfig(
    val name: String,
    val api_url: String,
    val db_table_name: String,
    val parameters: List<ParameterConfig>,
    // By default, this number of records is returned in WebUI if limit is not specified
    // does not need to be specified in YAML (uses given default value if not specified)
    val default_limit_web: Int = DEFAULT_LIMIT_FOR_WEB
)

@Serializable
class KeyCloakAuthConfig(
    val server_url: String,
    val realm: String,
    val client_id: String,
    val client_secret: String,
    val writer_group_name: String,
    val admin_group_name: String
)

@Serializable
class ParameterConfig(
    val name: String,
    val type: String,  // string/int/float/bool // TODO
    val intervals: Boolean,
    val web_name: String
)


fun ConfigFile.removeSensitiveData(): ConfigFile {
    /**
     * Removes all sensitive data (use before sending config to frontend via API)
     */
    return ConfigFile(
        event_db = DBConnectionConfig("", 0, "", "", ""),
        condition_db = if (condition_db == null) null else DBConnectionConfig("", 0, "", "", ""),
        database_auth = database_auth, /* Boolean so not sensitive */
        keycloak_auth = if (keycloak_auth == null) null else KeyCloakAuthConfig("",  "", "", "", "", ""),
        title = this.title,
        pages = this.pages
    )
}

fun ConfigFile.authRequired(): Boolean = (database_auth == true) || (keycloak_auth != null)
