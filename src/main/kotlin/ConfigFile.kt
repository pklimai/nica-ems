package com.example

class ConfigFile(
    val db_connection: DBConnectionParams,
    val title: String,
    val pages: List<Page>
)

class DBConnectionParams(
    val host: String,
    val port: Int,
    val db_name: String,
    val user: String,
    val password: String
)

class Page(
    val name: String,
    val api_url: String,
    val web_url: String,
    val db_table_name: String,
    val parameters: List<Parameter>
)

class Parameter(
    val name: String,
    val type: String,  // TODO
    val intervals: Boolean,
    val web_name: String
)
