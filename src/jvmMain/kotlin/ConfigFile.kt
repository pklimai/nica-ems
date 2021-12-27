package ru.mipt.npm.nica.emd

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

class DBConnectionConfig(
    val host: String,
    val port: Int,
    val db_name: String,
    val user: String,
    val password: String
)

class PageConfig(
    val name: String,
    val api_url: String,
    val web_url: String,
    val db_table_name: String,
    val parameters: List<ParameterConfig>
)

class LDAPAuthConfig(
    val ldap_server: String,
    val ldap_port: Int,
    val user_dn_format: String,
    val ldap_username: String,
    val ldap_password: String,
    val writer_group_dn: String,
    val admin_group_dn: String
)

class ParameterConfig(
    val name: String,
    val type: String,  // TODO
    val intervals: Boolean,
    val web_name: String
)
