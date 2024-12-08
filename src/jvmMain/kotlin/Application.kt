package ru.mipt.npm.nica.ems

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
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.postgresql.util.PSQLException
import java.io.File
import java.sql.DriverManager

lateinit var config: ConfigFile

fun Application.main() {

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    install(Compression)

    if (config.keycloak_auth != null) {
        if (config.database_auth == true) {
            error("If database_auth is set, no KeyCloak parameters are expected!")
        }
        install(Authentication) {
            basic("auth-keycloak-userpass") {
                // Obtain Token via KeyCloak
                validate { credentials ->
                    println("auth-keycloak-userpass called for username ${credentials.name}")
                    // Note: validate must return Principal in case of successful authentication or null otherwise
                    getKCPrincipalOrNull(config, credentials.name, credentials.password) //.also { println(it) }
                }
            }
            bearer("auth-keycloak-bearer") {
                authenticate { bearerTokenCredential: BearerTokenCredential ->
                    println("auth-keycloak-bearer called with token = ${bearerTokenCredential.token}")
                    val groups = getKCgroups(config, bearerTokenCredential.token) ?: return@authenticate null
                    TokenGroupsPrincipal(bearerTokenCredential.token, groups, rolesFromGroups(config, groups))
                }
            }
        }
    } else if (config.database_auth == true) {
        install(Authentication) {
            basic("auth-via-database") {
                validate { credentials ->
                    // all roles allowed here, enforcement is on the database side
                    UserIdPwPrincipal(credentials.name, credentials.password, UserRoles(true, true, true))
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
            try {
                val connEMD = newEMDConnection(config, this@get.context, forStatsGetting = true)!!
                connEMD.createStatement()
                    .executeQuery("SELECT json_stats FROM statistics ORDER BY id DESC LIMIT 1")
                    .let { resultSet ->
                        if (resultSet.next()) {
                            val r = resultSet.getString("json_stats")
                            connEMD.close()
                            call.response.header("Content-Type", "application/json")
                            call.respondText(r)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "No data in EMS statistics table")
                        }
                    }
            } catch (err: Exception) {
                println("EMS database error: $err")
                call.respond(HttpStatusCode.NotFound, "EMS database error: $err")
            }
        }

        fun Route.optionallyAuthenticate(build: Route.() -> Unit): Route {
            if (config.keycloak_auth != null) {
                return authenticate("auth-keycloak-userpass", "auth-keycloak-bearer", build = build)
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
                val roles = call.principal<WithRoles>()?.roles!!
                if (!(roles.isWriter or roles.isAdmin)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post  // not really needed here but important in other places
                } else {
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
                    }
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
                val roles = call.principal<WithRoles>()?.roles!!
                if (!(roles.isWriter or roles.isAdmin)) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
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
                    }
                }
            }
        }

        config.pages.forEach { page ->

            optionallyAuthenticate {

                legacyPage(page, config, connCondition)

                route(page.api_url) {

                    get("/${EVENT_ENTITY_API_NAME}") {
                        // no role checking here - any user allowed
                        val parameterBundle = ParameterBundle.buildFromCall(call, page)
                        if (parameterBundle.hasInvalidParameters()) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@get
                        }
                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            // val softwareMap = getSoftwareMap(connEMD)  // not used for now
                            val res = queryEMD(parameterBundle, page, connCondition, connEMD, null)

                            val lstEvents = ArrayList<EventRepr>()
                            while (res?.next() == true) {
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

                        val roles = call.principal<WithRoles>()?.roles!!
                        if (!(roles.isWriter or roles.isAdmin)) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }

                        val events = call.receive<Array<EventRepr>>()

                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
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
                                // print(query)
                                connEMD.createStatement().executeUpdate(query)
                            }
                            connEMD.close()
                            call.response.status(HttpStatusCode.OK)
                            call.respond("Events were created")
                        }
                    }

                    delete("/${EVENT_ENTITY_API_NAME}") {
                        val roles = call.principal<WithRoles>()?.roles!!
                        if (!roles.isAdmin) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@delete
                        } else {
                            call.respond(HttpStatusCode.NotImplemented)
                            TODO("To be implemented")
                        }
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

// See resources/application.conf for ktor configuration
fun main(args: Array<String>) {
    config = readConfig() ?: return
    io.ktor.server.netty.EngineMain.main(args)
}
