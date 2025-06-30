package ru.mipt.npm.nica.ems

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
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
import java.sql.Connection
import java.sql.DriverManager

lateinit var config: ConfigFile

fun debug(message: String) {
    if (config.debug == true) {
        println(message)
    }
}

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
                    debug("auth-keycloak-userpass called for username ${credentials.name}")
                    // Note: validate must return Principal in case of successful authentication or null otherwise
                    getKCPrincipalOrNull(config, credentials.name, credentials.password) //.also { debug(it) }
                }
            }
            bearer("auth-keycloak-bearer") {
                authenticate { bearerTokenCredential: BearerTokenCredential ->
                    debug("auth-keycloak-bearer called with token = ${bearerTokenCredential.token}")
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

    // debug("Working Directory = ${System.getProperty("user.dir")}")
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
            var connEMD: Connection? = null
            try {
                connEMD = newEMDConnection(config, this@get.context, forStatsGetting = true)!!
                connEMD.createStatement()
                    .executeQuery("SELECT json_stats FROM statistics_ ORDER BY id DESC LIMIT 1")
                    .let { resultSet ->
                        if (resultSet.next()) {
                            val r = resultSet.getString("json_stats")
                            call.response.header("Content-Type", "application/json")
                            call.respondText(r)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "No data in EMS statistics_ table")
                        }
                    }
            } catch (err: Exception) {
                debug("EMS database error: $err")
                call.respond(HttpStatusCode.NotFound, "EMS database error: $err")
            } finally {
                connEMD?.close()
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
                var connEMD: Connection? = null
                try {
                    connEMD = newEMDConnection(config, this@get.context)!!
                    val swList = mutableListOf<SoftwareVersion>().apply {
                        connEMD!!.createStatement().executeQuery("SELECT * FROM software_").let { resultSet ->
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
                    call.respond(swList)
                } catch (err: PSQLException) {
                    if (err.toString().contains("The connection attempt failed.")) {
                        call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Database error: $err")
                    }
                } catch (err: BadRequestException) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                } catch (err: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error obtaining software table data: $err")
                } finally {
                    connEMD?.close()
                }
            }

            post(SOFTWARE_URL) {
                // e.g. POST { "software_id": 100, "software_version": "22.1" }
                // Note: software_id is assigned automatically by the database, regardless of what is passed in JSON
                val roles = call.principal<WithRoles>()?.roles!!
                // debug("Roles in post(SOFTWARE_URL): $roles")
                if (!(roles.isWriter or roles.isAdmin)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post  // not really needed here but important in other places
                }
                var connEMD: Connection? = null
                try {
                    val sw = call.receive<SoftwareVersion>()
                    connEMD = newEMDConnection(config, this.context)
                    val query = """INSERT INTO software_ (software_version) VALUES ('${sw.software_version}')"""
                    debug(query)
                    connEMD!!.createStatement().executeUpdate(query)
                    call.respond(HttpStatusCode.OK, "SW record was created")
                } catch (err: PSQLException) {
                    if (err.toString().contains("The connection attempt failed.")) {
                        call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Database error: $err")
                    }
                } catch (err: BadRequestException) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                } catch (err: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error obtaining software table data: $err")
                } finally {
                    connEMD?.close()
                }
            }

            get(STORAGE_URL) {
                var connEMD: Connection? = null
                try {
                    connEMD = newEMDConnection(config, this@get.context)!!
                    val storageList = mutableListOf<Storage>().apply {
                        connEMD!!.createStatement().executeQuery("SELECT * FROM storage_").let { resultSet ->
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
                    call.respond(storageList)
                } catch (err: PSQLException) {
                    if (err.toString().contains("The connection attempt failed.")) {
                        call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Database error: $err")
                    }
                } catch (err: BadRequestException) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                } catch (err: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error obtaining software table data: $err")
                } finally {
                    connEMD?.close()
                }
            }

            post(STORAGE_URL) {
                // Note: storage_id is assigned automatically by the database, regardless of what is passed in JSON
                val roles = call.principal<WithRoles>()?.roles!!
                if (!(roles.isWriter or roles.isAdmin)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                var connEMD: Connection? = null
                try {
                    val storage = call.receive<Storage>()
                    connEMD = newEMDConnection(config, this.context)
                    val query = """INSERT INTO storage_ (storage_name) VALUES ('${storage.storage_name}')"""
                    debug(query)

                    connEMD!!.createStatement().executeUpdate(query)
                    call.response.status(HttpStatusCode.OK)
                    call.respond("Storage record was created")
                } catch (err: PSQLException) {
                    if (err.toString().contains("The connection attempt failed.")) {
                        call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                    } else {
                        call.respond(HttpStatusCode.Conflict, "Database error: $err")
                    }
                } catch (err: BadRequestException) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                } catch (err: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error obtaining software table data: $err")
                } finally {
                    connEMD?.close()
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
                        if (parameterBundle.hasInvalidParameters(call, page)) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid parameters detected in request")
                            return@get
                        }
                        var connEMD: Connection? = null
                        try {
                            connEMD = newEMDConnection(config, this.context)!!
                            val res = queryEMD(parameterBundle, page, connCondition, connEMD!!, null)
                            val limit = getLimit(page, parameterBundle.limit?.stringValue)
                            val offset = parameterBundle.offset?.stringValue?.toLong() ?: 0
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
                            call.respond(mapOf(
                                "events" to lstEvents,
                                "limit" to limit,
                                "offset" to offset,
                                "count" to lstEvents.size
                            ))
                        } catch (err: PSQLException) {
                            if (err.toString().contains("The connection attempt failed.")) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Database error: $err")
                            }
                        } catch (err: BadRequestException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                        } catch (err: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Error obtaining event data: $err")
                        } finally {
                            connEMD?.close()
                        }
                    }

                    get("/${EVENT_COUNT_API_NAME}") {
                        val parameterBundle = ParameterBundle.buildFromCall(call, page)
                        if (parameterBundle.hasInvalidParameters(call, page)) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid parameters detected in request")
                            return@get
                        }
                        var connEMD: Connection? = null
                        try {
                            connEMD = newEMDConnection(config, this.context)!!
                            val res = queryEMD(parameterBundle, page, connCondition, connEMD!!, null, countOnly = true)
                            if (res?.next() == true) {
                                call.respond(HttpStatusCode.OK, mapOf("count" to res.getInt(1)))
                            } else {
                                call.respond(
                                    HttpStatusCode.ServiceUnavailable,
                                    "Could not obtain event count from database"
                                )
                            }
                        } catch (err: PSQLException) {
                            if (err.toString().contains("The connection attempt failed.")) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Database error: $err")
                            }
                        } catch (err: BadRequestException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                        } catch (err: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Error obtaining event data: $err")
                        } finally {
                            connEMD?.close()
                        }
                    }

                    post("/${EVENT_ENTITY_API_NAME}") {
                        val roles = call.principal<WithRoles>()?.roles!!
                        if (!(roles.isWriter or roles.isAdmin)) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@post
                        }
                        var connEMD: Connection? = null
                        try {
                            val events = call.receive<Array<EventRepr>>()
                            connEMD = newEMDConnection(config, this.context)
                            connEMD!!.autoCommit = false
                            val softwareMap = getSoftwareMap(connEMD!!)
                            val storageMap = getStorageMap(connEMD!!)
                            debug("Received ${events.size} events!")
                            val fileData = mutableMapOf<Pair<Byte, String>, Int>()
                            events.forEach { event ->
                                val software_id = softwareMap.str_to_id[event.software_version]
                                if (software_id == null) {
                                    call.respond(
                                        HttpStatusCode.Conflict,
                                        "Error: Software version ${event.software_version} not present in the dictionary"
                                    )
                                    return@post
                                }
                                val storage_id = storageMap.str_to_id[event.reference.storage_name]
                                if (storage_id == null) {
                                    call.respond(
                                        HttpStatusCode.Conflict,
                                        "Error: Storage ${event.reference.storage_name} not present in the dictionary"
                                    )
                                    return@post
                                }
                                val file_path = event.reference.file_path
                                val file_guid: Int
                                if (Pair(storage_id, file_path) !in fileData) {
                                    val res = connEMD!!.createStatement().executeQuery(
                                        """SELECT file_guid FROM file_ WHERE storage_id = $storage_id AND file_path = '$file_path'"""
                                    )
                                    if (res.next()) { // file record already exists
                                        file_guid = res.getInt("file_guid")
                                    } else { // create file record
                                        val fileQuery =
                                            """INSERT INTO file_ (storage_id, file_path) VALUES ($storage_id, '$file_path')"""
                                        debug(fileQuery)
                                        connEMD!!.createStatement().executeUpdate(fileQuery)
                                        val res2 = connEMD!!.createStatement().executeQuery(
                                            """SELECT file_guid FROM file_ WHERE storage_id = $storage_id AND file_path = '$file_path'"""
                                        )
                                        res2.next()
                                        file_guid = res2.getInt("file_guid")
                                    }
                                    fileData[Pair(storage_id, file_path)] = file_guid
                                    debug("File GUID for $file_path = $file_guid")
                                }
                            }
                            val stmt = connEMD!!.createStatement()
                            events.forEach { event ->
                                debug("Creating event: $event")
                                val software_id = softwareMap.str_to_id[event.software_version]
                                val storage_id = storageMap.str_to_id[event.reference.storage_name]
                                val file_guid = fileData[Pair(storage_id, event.reference.file_path)]!!
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
                                debug(query)
                                stmt.addBatch(query)
                            }
                            stmt.executeBatch()
                            connEMD!!.commit()
                            debug("Success: ${events.size} event(s) were created")
                            call.respond(HttpStatusCode.OK, "Success: ${events.size} event(s) were created")
                        } catch (err: PSQLException) {
                            if (err.toString().contains("The connection attempt failed.")) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Database error: $err")
                            }
                        } catch (err: java.sql.BatchUpdateException) {
                            call.respond(HttpStatusCode.Conflict, "Database error: $err")
                        } catch (err: BadRequestException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                        } catch (err: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Error writing event data: $err")
                        } finally {
                            connEMD?.close()
                        }
                    }

                    put("/${EVENT_ENTITY_API_NAME}") {
                        val roles = call.principal<WithRoles>()?.roles!!
                        if (!(roles.isWriter or roles.isAdmin)) {
                            call.respond(HttpStatusCode.Unauthorized)
                            return@put
                        }
                        var connEMD: Connection? = null
                        try {
                            val events = call.receive<Array<EventRepr>>()
                            connEMD = newEMDConnection(config, this.context)
                            connEMD!!.autoCommit = false
                            val softwareMap = getSoftwareMap(connEMD!!)
                            val storageMap = getStorageMap(connEMD!!)
                            debug("Received ${events.size} events!")
                            val fileData = mutableMapOf<Pair<Byte, String>, Int>()
                            events.forEach { event ->
                                val software_id = softwareMap.str_to_id[event.software_version]
                                if (software_id == null) {
                                    call.respond(
                                        HttpStatusCode.Conflict,
                                        "Error: Software version ${event.software_version} not present in the dictionary"
                                    )
                                    return@put
                                }
                                val storage_id = storageMap.str_to_id[event.reference.storage_name]
                                if (storage_id == null) {
                                    call.respond(
                                        HttpStatusCode.Conflict,
                                        "Error: Storage ${event.reference.storage_name} not present in the dictionary"
                                    )
                                    return@put
                                }
                                val file_path = event.reference.file_path
                                val file_guid: Int
                                if (Pair(storage_id, file_path) !in fileData) {
                                    val res = connEMD!!.createStatement().executeQuery(
                                        """SELECT file_guid FROM file_ WHERE storage_id = $storage_id AND file_path = '$file_path'"""
                                    )
                                    if (res.next()) { // file record already exists
                                        file_guid = res.getInt("file_guid")
                                    } else { // create file record
                                        val fileQuery =
                                            """INSERT INTO file_ (storage_id, file_path) VALUES ($storage_id, '$file_path')"""
                                        debug(fileQuery)
                                        connEMD!!.createStatement().executeUpdate(fileQuery)
                                        val res2 = connEMD!!.createStatement().executeQuery(
                                            """SELECT file_guid FROM file_ WHERE storage_id = $storage_id AND file_path = '$file_path'"""
                                        )
                                        res2.next()
                                        file_guid = res2.getInt("file_guid")
                                    }
                                    fileData[Pair(storage_id, file_path)] = file_guid
                                    debug("File GUID for $file_path = $file_guid")
                                }
                            }
                            val stmt = connEMD!!.createStatement()
                            events.forEach { event ->
                                debug("Creating event: $event")
                                val software_id = softwareMap.str_to_id[event.software_version]
                                val storage_id = storageMap.str_to_id[event.reference.storage_name]
                                val file_guid = fileData[Pair(storage_id, event.reference.file_path)]!!
                                val parameterValuesStr =
                                    page.parameters.joinToString(", ") {
                                        when (it.type.uppercase()) {
                                            "STRING" -> "'" + event.parameters[it.name].toString() + "'"
                                            else -> event.parameters[it.name].toString()
                                        }
                                    }
                                val parameterNamesEqValuesStr =
                                    page.parameters.joinToString(", ") {
                                        when (it.type.uppercase()) {
                                            "STRING" -> "${it.name}='${event.parameters[it.name].toString()}'"
                                            else -> "${it.name}=${event.parameters[it.name].toString()}"
                                        }
                                    }
                                val query = """
                                        INSERT INTO ${page.db_table_name} 
                                            (file_guid, event_number, software_id, period_number, run_number,
                                            ${page.parameters.joinToString(", ") { it.name }})
                                        VALUES ($file_guid, ${event.reference.event_number}, $software_id, ${event.period_number},
                                            ${event.run_number}, $parameterValuesStr)
                                        ON CONFLICT (file_guid, event_number) DO UPDATE SET
                                            software_id=$software_id, period_number=${event.period_number}, run_number=${event.run_number},
                                            $parameterNamesEqValuesStr
                                    """.trimIndent()
                                debug(query)
                                stmt.addBatch(query)
                            }
                            stmt.executeBatch()
                            connEMD!!.commit()
                            call.respond(HttpStatusCode.OK, "Success: ${events.size} event(s) were put")
                        } catch (err: PSQLException) {
                            if (err.toString().contains("The connection attempt failed.")) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Database error: $err")
                            }
                        } catch (err: BadRequestException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                        } catch (err: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Error writing event data: $err")
                        } finally {
                            connEMD?.close()
                        }
                    }

                    delete("/${EVENT_ENTITY_API_NAME}") {
                        val roles = call.principal<WithRoles>()?.roles!!
                        if (!roles.isAdmin) {
                            call.respond(HttpStatusCode.Unauthorized, "Only user with Admin role can delete events")
                            return@delete
                        }
                        var connEMD: Connection? = null
                        try {
                            // Here, we only care about reference to event, other event data is optional and is ignored, if passed
                            val events = call.receive<Array<EventReprForDelete>>()
                            connEMD = newEMDConnection(config, this.context)
                            connEMD!!.autoCommit = false
                            val storageMap = getStorageMap(connEMD!!)
                            val fileData = mutableMapOf<Pair<Byte, String>, Int>()
                            events.forEach { event ->
                                val storage_name = event.reference.storage_name
                                val storage_id = storageMap.str_to_id[storage_name]
                                if (storage_id == null) {
                                    call.respond(
                                        HttpStatusCode.Conflict,
                                        "Error: Storage ${event.reference.storage_name} not present in the dictionary"
                                    )
                                    return@delete
                                }
                                val file_path = event.reference.file_path
                                if (Pair(storage_id, file_path) !in fileData) {
                                    val res = connEMD!!.createStatement().executeQuery(
                                        """SELECT file_guid FROM file_ WHERE storage_id = $storage_id AND file_path = '$file_path'"""
                                    )
                                    if (res.next()) {
                                        val file_guid: Int = res.getInt("file_guid")
                                        debug("File GUID ($storage_id, $file_path) = $file_guid")
                                        fileData[Pair(storage_id, file_path)] = file_guid
                                    } else { // no such file
                                        call.respond(
                                            HttpStatusCode.NotFound,
                                            "Error: file_guid not found for event ${event.str()}"
                                        )
                                        return@delete
                                    }
                                }
                            }
                            val stmt = connEMD!!.createStatement()
                            events.forEach { event ->
                                debug("Deleting event: $event")
                                val storage_id = storageMap.str_to_id[event.reference.storage_name]
                                val file_guid = fileData[Pair(storage_id, event.reference.file_path)]
                                val query = """
                                    DELETE FROM ${page.db_table_name} 
                                    WHERE (("file_guid" = $file_guid AND "event_number" = ${event.reference.event_number}));
                                    """.trimIndent()
                                debug(query)
                                stmt.addBatch(query)
                            }
                            val res = stmt.executeBatch()
                            val deletedCount = res.sum()
                            if (deletedCount != events.size) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    "Error: could not find some of the events to delete, aborting transaction.\n" +
                                            "The first missing event was ${events[res.indexOfFirst { it == 0 }].str()}"
                                )
                                return@delete
                            }
                            connEMD!!.commit()
                            call.respond(HttpStatusCode.OK, "Success: $deletedCount event(s) were deleted")
                        } catch (err: PSQLException) {
                            if (err.toString().contains("The connection attempt failed.")) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Database connection failed: $err")
                            } else {
                                call.respond(HttpStatusCode.Conflict, "Database error: $err")
                            }
                        } catch (err: BadRequestException) {
                            call.respond(HttpStatusCode.UnprocessableEntity, "Error processing content: $err")
                        } catch (err: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Error deleting events: $err")
                        } finally {
                            connEMD?.close()
                        }
                    }

                    // Synchronous - build a file with some ROOT macro and return it
                    get("/eventFile") {
                        // TODO Apply all filtering, build ROOT file
                        debug("Serving dummy eventFile...")
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
