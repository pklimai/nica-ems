package ru.mipt.npm.nica.ems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.openapi.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import jakarta.ws.rs.NotAuthorizedException
import kotlinx.coroutines.runBlocking
import org.keycloak.Config
import org.keycloak.representations.idm.authorization.AuthorizationRequest
import org.postgresql.util.PSQLException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.AccessTokenResponse


const val CONFIG_PATH = "./ems.config.yaml"

fun Application.main() {

    val mapper = ObjectMapper(YAMLFactory()).also { it.findAndRegisterModules() }

    val config: ConfigFile
    try {
        config = mapper.readValue(File(CONFIG_PATH), ConfigFile::class.java)
    } catch (e: java.lang.Exception) {
        println(
            "Could not read config file from $CONFIG_PATH. \n" +
                    "Make sure the file is there and has proper format (if in Docker, mount as volume)"
        )
        throw e
    }
    println("Done reading config from $CONFIG_PATH")

/*
    val keycloak = Keycloak.getInstance(
        config.keycloak_auth?.server_url,
        config.keycloak_auth?.realm,
        "pklimai",
        "******",
        config.keycloak_auth?.client_id,
        config.keycloak_auth?.client_secret,
        null,
        null,
        false,
        null,
        "openid"
    )

    var token: AccessTokenResponse? = null
    try {
        token = keycloak.tokenManager().grantToken()
        println(token.token)
        println(token.scope)

        //println(keycloak.realm(config.keycloak_auth?.realm).users().get("pklimai").groups())
        //keycloak.tokenManager().
    } catch (e: NotAuthorizedException) {
        println("No auth!")
    }
    val url = "${config.keycloak_auth?.server_url}/realms/${config.keycloak_auth?.realm}/protocol/openid-connect/userinfo"
    val client = HttpClient(CIO) {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(token!!.token, token!!.refreshToken)
                }
            }
        }
    }
    runBlocking {
        val response = client.get(url)
        println(response)
    }

*/

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    install(Compression)

    if (config.keycloak_auth != null) {
        if (config.database_auth == true) {
            error("If database_auth is set, no LDAP parameters are expected!")
        }
        install(Authentication) {
            basic("auth-keycloak") {
                skipWhen { it ->
                    // If there is Authorization: Bearer header, we must check token via KeyCloak
                    println(it.request.headers["Authorization"])
                    it.request.headers["Authorization"]?.startsWith("Bearer") == true
                }
                validate { credentials ->
//                    this.request.headers.filter{s, _ -> s == "Authorization"}.forEach { s, ls ->
//                        print(s)
//                        print(" --- ")
//                        ls.forEach() {
//                            print(it)
//                            print(" -- ")
//                        }
//                        println()
//                    }
                    println("Obtained credentials: ${credentials.name} ${credentials.password}")
                    // validate must return Principal in a case of successful authentication or null if authentication fails.
                    // so we must obtain Token via KC and also save user roles here


                    UserIdPwPrincipal(credentials.name, credentials.password)

                }
            }
        }
    } else if (config.database_auth == true) {
        install(Authentication) {
            basic("auth-via-database") {
                validate { credentials ->
                    UserIdPwPrincipal(credentials.name, credentials.password)
                }
            }
        }
    }

    // If null, do not use event preselection from the Condition Database
    val connCondition = config.condition_db?.let {
        val urlConditionDB =
            "jdbc:postgresql://${config.condition_db!!.host}:${config.condition_db!!.port}/${config.condition_db!!.db_name}"
        DriverManager.getConnection(urlConditionDB, config.condition_db!!.user, config.condition_db!!.password)
    }

    // TODO: Check if tables in Event Catalogue already exist, if not, create them in the database?

    // println("Working Directory = ${System.getProperty("user.dir")}")
    routing {

        // Allows all resources to be statically available (including generated nica-ems.js file)
        staticResources("", "")

        // OpenAPI (aka Swagger) page
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")

        // React Web UI available on root URL
        get("/") {
            call.respondText(
                this::class.java.classLoader.getResource("index.html")!!.readText(),
                ContentType.Text.Html
            )
        }

        // For health check
        get("/health") {
            call.respond(HttpStatusCode.OK)
        }

        // This way frontend knows about different pages, parameters, etc.
        get(CONFIG_URL) {
            call.respond(config.removeSensitiveData())
        }

        get(STATISTICS_URL) {
            val connEMD = newEMDConnection(config, this@get.context, forStatsGetting = true)
            if (connEMD == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                connEMD.createStatement().executeQuery("SELECT json_stats FROM statistics ORDER BY id DESC LIMIT 1")
                    .let { resultSet ->
                        if (resultSet.next()) {
                            val r = resultSet.getString("json_stats")
                            connEMD.close()
                            call.response.header("Content-Type", "application/json")
                            call.respondText(r)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
            }
        }

        fun Route.optionallyAuthenticate(build: Route.() -> Unit): Route {
            if (config.keycloak_auth != null) {
                return authenticate("auth-keycloak", build = build)
            } else if (config.database_auth == true) {
                return authenticate("auth-via-database", build = build)
            } else {
                build(this)
            }
            return this
        }

        legacyHomePage(config)
        optionallyAuthenticate {
            legacyDictionaries(config)
        }

        optionallyAuthenticate {

            get(SOFTWARE_URL) {
                val connEMD = newEMDConnection(config, this@get.context)
                if (connEMD == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val swList = mutableListOf<SoftwareVersion>().apply {
                        connEMD.createStatement().executeQuery("SELECT * FROM software_").let { resultSet ->
                            while (resultSet.next()) {
                                this@apply.add(
                                    SoftwareVersion(
                                        resultSet.getInt("software_id"),
                                        resultSet.getString("software_version")
                                    )
                                )
                            }
                        }
                    }
                    connEMD.close()
                    call.respond(swList)
                }
            }

            post(SOFTWARE_URL) {
                // e.g. POST { "software_id": 100, "software_version": "22.1" }
                // Note: software_id is assigned automatically by the database, regardless of what is passed in JSON
//                val roles = getUserRoles(config, call)
//                if (!(roles.isWriter or roles.isAdmin)) {
//                    call.respond(HttpStatusCode.Unauthorized)
//                } else {
                val sw = call.receive<SoftwareVersion>()
                val connEMD = newEMDConnection(config, this.context)
                if (connEMD == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val query = """
                        INSERT INTO software_ (software_version)
                        VALUES ('${sw.software_version}')
                    """.trimIndent()
                    // print(query)
                    try {
                        connEMD.createStatement().executeUpdate(query)
                        call.response.status(HttpStatusCode.OK)
                        call.respond("SW record was created")
                    } catch (ex: PSQLException) {
                        if (ex.serverErrorMessage.toString().startsWith("ERROR: permission denied for table")) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else if (ex.serverErrorMessage.toString().startsWith("ERROR: duplicate key value")) {
                            call.respond(HttpStatusCode.Conflict)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    } finally {
                        connEMD.close()
                    }
                    // }
                }
            }

            get(STORAGE_URL) {
                val connEMD = newEMDConnection(config, this@get.context)
                if (connEMD == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val storageList = mutableListOf<Storage>().apply {
                        connEMD.createStatement().executeQuery("SELECT * FROM storage_").let { resultSet ->
                            while (resultSet.next()) {
                                this@apply.add(
                                    Storage(
                                        resultSet.getInt("storage_id"),
                                        resultSet.getString("storage_name")
                                    )
                                )
                            }
                        }
                    }
                    connEMD.close()
                    call.respond(storageList)
                }
            }

            post(STORAGE_URL) {
                // Note: storage_id is assigned automatically by the database, regardless of what is passed in JSON
//                val roles = getUserRoles(config, call)
//                if (!(roles.isWriter or roles.isAdmin)) {
//                    call.respond(HttpStatusCode.Unauthorized)
//                } else
//                {
                val storage = call.receive<Storage>()
                val connEMD = newEMDConnection(config, this.context)
                if (connEMD == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val query = """
                        INSERT INTO storage_ (storage_name)
                        VALUES ('${storage.storage_name}')
                    """.trimIndent()
                    // print(query)
                    try {
                        connEMD.createStatement().executeUpdate(query)
                        call.response.status(HttpStatusCode.OK)
                        call.respond("Storage record was created")
                    } catch (ex: PSQLException) {  // e.g. this version already exists
                        if (ex.serverErrorMessage.toString().startsWith("ERROR: permission denied for table")) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else if (ex.serverErrorMessage.toString().startsWith("ERROR: duplicate key value")) {
                            call.respond(HttpStatusCode.Conflict)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    } finally {
                        connEMD.close()
                    }
                    //  }
                }
            }
        }

        config.pages.forEach { page ->

            optionallyAuthenticate {

                legacyPage(page, config, connCondition)

                route(page.api_url) {

                    get("/${EVENT_ENTITY_API_NAME}") {

                        val parameterBundle = ParameterBundle.buildFromCall(call, page)
                        if (parameterBundle.hasInvalidParameters()) {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            // val softwareMap = getSoftwareMap(connEMD)  // not used for now
                            val res = queryEMD(parameterBundle, page, connCondition, connEMD, null)

                            val lstEvents = ArrayList<EventRepr>()
                            while (res.next()) {
                                val paramMap = HashMap<String, Any>()

                                page.parameters.forEach {
                                    when (it.type.uppercase()) {
                                        "INT" -> paramMap[it.name] = res.getInt(it.name)
                                        "FLOAT" -> paramMap[it.name] = res.getFloat(it.name)
                                        "STRING" -> paramMap[it.name] = res.getString(it.name)
                                        "BOOL" -> paramMap[it.name] = res.getBoolean(it.name)
                                        else -> throw Exception("Unknown parameter type!")
                                    }
                                }
                                lstEvents.add(
                                    EventRepr(
                                        Reference(
                                            res.getString("storage_name"),
                                            res.getString("file_path"),
                                            res.getInt("event_number")
                                        ),
                                        res.getString("software_version"),
                                        res.getShort("period_number"),
                                        res.getInt("run_number"),
                                        paramMap
                                    )
                                )
                            }
                            connEMD.close()
                            call.respond(mapOf("events" to lstEvents))
                        }
                    }

                    post("/${EVENT_ENTITY_API_NAME}") {

//                        val roles = getUserRoles(config, call)
//                        if (!(roles.isWriter or roles.isAdmin)) {
//                            call.respond(HttpStatusCode.Unauthorized)
//                        }

                        val events = call.receive<Array<EventRepr>>()

                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            val softwareMap = getSoftwareMap(connEMD)
                            val storageMap = getStorageMap(connEMD)
                            events.forEach { event ->
                                println("Create event: $event")
                                val software_id = softwareMap.str_to_id[event.software_version]
                                val file_path = event.reference.file_path
                                val storage_name = event.reference.storage_name
                                val storage_id = storageMap.str_to_id[storage_name]

                                // get file_guid
                                val file_guid: Int
                                val res = connEMD.createStatement().executeQuery(
                                    """SELECT file_guid FROM file_ WHERE 
                             storage_id = $storage_id AND file_path = '$file_path'
                            """.trimMargin()
                                )
                                if (res.next()) {
                                    file_guid = res.getInt("file_guid")
                                    println("File GUID = $file_guid")
                                } else {
                                    // create file
                                    val fileQuery = """
                                INSERT INTO file_ (storage_id, file_path)
                                VALUES ($storage_id, '$file_path')
                            """.trimIndent()
                                    print(fileQuery)
                                    connEMD.createStatement().executeUpdate(fileQuery)
                                    // TODO remove duplicate code here...
                                    val res2 = connEMD.createStatement().executeQuery(
                                        """SELECT file_guid FROM file_ WHERE 
                             storage_id = $storage_id AND file_path = '$file_path'
                            """.trimMargin()
                                    )
                                    if (res2.next()) {
                                        file_guid = res2.getInt("file_guid")
                                        println("File GUID = $file_guid")
                                    } else {
                                        throw java.lang.Exception("File guid writing issue... ")
                                    }
                                }
                                val parameterValuesStr =
                                    page.parameters.joinToString(", ") {
                                        when (it.type.uppercase()) {
                                            "STRING" -> "'" + event.parameters[it.name].toString() + "'"
                                            else -> event.parameters[it.name].toString()
                                        }
                                    }
                                val query = """
                                INSERT INTO ${page.db_table_name} 
                                (file_guid, event_number, software_id, period_number, run_number,
                                 ${page.parameters.joinToString(", ") { it.name }})
                                VALUES ($file_guid, ${event.reference.event_number}, $software_id, ${event.period_number},
                                   ${event.run_number}, $parameterValuesStr)
                                """.trimIndent()
                                print(query)
                                connEMD.createStatement().executeUpdate(query)
                            }
                            connEMD.close()
                            call.response.status(HttpStatusCode.OK)
                            call.respond("Events were created")
                        }
                    }

                    delete("/${EVENT_ENTITY_API_NAME}") {
                        TODO("To be implemented")
                        /*
                        val roles = getUserRoles(config, call)
                        if (!roles.isAdmin) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            call.respond(HttpStatusCode.NotImplemented)
                            TODO("To be implemented")
                        }
                        */
                    }

                    // Synchronous - build a file with some ROOT macro and return it
                    get("/eventFile") {
                        // TODO Apply all filtering, build ROOT file
                        println("Serving dummy eventFile...")
                        val f =
                            Thread.currentThread().getContextClassLoader().getResource("static/downloadFile.bin")!!.file
                        call.respondFile(File(f))
                    }

                    // Asynchronous - to return reference to the file that WILL be created in some time
                    get("/eventFileRef") {
                        TODO()
                    }

                }
            }
        }
    }
}

fun newEMDConnection(config: ConfigFile, context: ApplicationCall, forStatsGetting: Boolean = false): Connection? {
    val urlEventDB =
        "jdbc:postgresql://${config.event_db.host}:${config.event_db.port}/${config.event_db.db_name}"
    // val connEMD = DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    if (config.database_auth == true && !forStatsGetting) {
        val user = context.principal<UserIdPwPrincipal>()!!.name
        val pass = context.principal<UserIdPwPrincipal>()!!.pw
        try {
            val connection = DriverManager.getConnection(urlEventDB, user, pass)
            return connection
        } catch (_: PSQLException) {
            return null
        }
    } else {
        // LDAP or no auth at all, or we are collecting stats for homepage
        return DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    }
}

/*
private fun getUserRoles(config: ConfigFile, call: ApplicationCall): UserRoles {
    if (config.database_auth == true) {
        // Allow all on our end -- database will permit/deny based on user
        return UserRoles(isReader = true, isWriter = true, isAdmin = true)
    } else if (config.keycloak_auth != null) {
        val username = call.principal<UserIdPrincipal>()?.name!!
        // println("AUTHENTICATED USER NAME IS: $username")
        val ldapConn = LDAPConnection()
        ldapConn.connect(config.keycloak_auth.ldap_server, config.keycloak_auth.ldap_port)
        // Perform actual authentication
        ldapConn.bind(
            config.keycloak_auth.user_dn_format.replace("%s", config.keycloak_auth.ldap_username),
            // For example, "uid=user,cn=users,cn=accounts,dc=jinr,dc=ru"
            config.keycloak_auth.ldap_password
        )

        fun belongsToGroup(group: String) = (ldapConn.search(
            SearchRequest(
                //"uid=$username,cn=users,cn=accounts,dc=jinr,dc=ru"
                config.keycloak_auth.user_dn_format.replace("%s", username),
                SearchScope.SUB,
                "(&(memberOf=$group))"
            )
        ).entryCount == 1)

        val isWriter = belongsToGroup(config.keycloak_auth.writer_group_dn)
        val isAdmin = belongsToGroup(config.keycloak_auth.admin_group_dn)
        // println("Writer: $isWriter, Admin: $isAdmin")
        ldapConn.close()
        return UserRoles(isReader = true, isWriter = isWriter, isAdmin = isAdmin)
    } else {
        return UserRoles(isReader = true, isWriter = false, isAdmin = false)
    }
}
*/


// See resources.application.conf for ktor configuration
fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
