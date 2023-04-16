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
    // in this case ldap_auth is not used
    // If both are not set, authentication is disabled completely
    val database_auth: Boolean?,
    val ldap_auth: LDAPAuthConfig?,

    val title: String,
    val pages: List<PageConfig>
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
class LDAPAuthConfig(
    val ldap_server: String,
    val ldap_port: Int,
    val user_dn_format: String,
    val ldap_username: String,
    val ldap_password: String,
    val writer_group_dn: String,
    val admin_group_dn: String
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
        ldap_auth = if (ldap_auth == null) null else LDAPAuthConfig("", 0, "", "", "", "", ""),
        title = this.title,
        pages = this.pages
    )
}

fun ConfigFile.authRequired(): Boolean = (database_auth == true) || (ldap_auth != null)
