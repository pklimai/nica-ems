package ru.mipt.npm.nica.emd

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.unboundid.ldap.sdk.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.ldap.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.postgresql.util.PSQLException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager


// val DRIVER = "org.postgresql.Driver"
val DOCKER_CONFIG_PATH = "/root/event-config.yaml"
val TEST_CONFIG_PATH = "src/jvmMain/resources/event-config-example.yaml"

fun Application.main() {

    val mapper = ObjectMapper(YAMLFactory())
    mapper.findAndRegisterModules()

    var config: ConfigFile
    try {
        // Config provided as Docker volume
        config = mapper.readValue(File(DOCKER_CONFIG_PATH), ConfigFile::class.java)
        println("Read config from $DOCKER_CONFIG_PATH")
    } catch (e: java.lang.Exception) {
        // Local test config
        config = mapper.readValue(File(TEST_CONFIG_PATH), ConfigFile::class.java)
        println("Read config file from $TEST_CONFIG_PATH")
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    install(Compression)

    if (config.ldap_auth != null) {
        if (config.database_auth == true) {
            error("If database_auth is set, no LDAP parameters are expected!")
        }
        install(Authentication) {
            basic("auth-ldap") {
                validate { credentials ->
                    ldapAuthenticate(
                        credentials,
                        "ldap://${config.ldap_auth!!.ldap_server}:${config.ldap_auth!!.ldap_port}",
                        config.ldap_auth!!.user_dn_format
                    )
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
        static("static") {
            // http://127.0.0.1:8080/static/style.css
            files("src/jvmMain/resources/static/css")   // in dev
            files("/app/resources/main/static/css")  // in Docker
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +config.title }
                    styleLink("/static/style.css")
                }
                body {
                    h2 { +config.title }

                    config.pages.forEach {
                        h3 { +it.name }
                        h5 { +"REST API" }
                        p { a(href = it.api_url + "/emd") { +"API - get all events" } }
                        h5 { +"WebUI" }
                        p { a(href = it.web_url) { +"Search Form" } }
                        hr {}
                    }

                    h3 { +"Auxiliary data" }
                    p { a(href = "/dictionaries") { +"Dictionaries" } }

                }
            }
        }

        get("/health") {
            call.respond(HttpStatusCode.OK)
        }

        fun Route.optionallyAuthenticate(build: Route.() -> Unit): Route {
            if (config.ldap_auth != null) {
                return authenticate("auth-ldap", build = build)
            } else if (config.database_auth == true) {
                return authenticate("auth-via-database", build = build)
            } else {
                build(this)
            }
            return this
        }

        optionallyAuthenticate {
            route("/dictionaries") {
                get {
                    call.respondHtml {
                        head {
                            title { +config.title }
                            styleLink("/static/style.css")
                        }
                        body {
                            a {
                                href = "/"
                                +"Home"
                            }

                            val connEMD = newEMDConnection(config, this@get.context)
                            if (connEMD == null) {
                                h4 { +"Event Catalogue unavailable!!!" }
                            } else {
                                h2 { +"Software version table" }
                                connEMD.createStatement().executeQuery("SELECT * FROM software_").let { res ->
                                    table {
                                        tr {
                                            th { +"software_id" }
                                            th { +"software_version" }
                                        }
                                        while (res.next()) {
                                            tr {
                                                td { +res.getInt("software_id").toString() }
                                                td { +res.getString("software_version") }
                                            }
                                        }
                                    }
                                }
                                h2 { +"Storage table" }
                                connEMD.createStatement().executeQuery("SELECT * FROM storage_").let { res ->
                                    table {
                                        tr {
                                            th { +"storage_id" }
                                            th { +"storage_name" }
                                        }
                                        while (res.next()) {
                                            tr {
                                                td { +res.getInt("storage_id").toString() }
                                                td { +res.getString("storage_name") }
                                            }
                                        }
                                    }
                                }
                                h2 { +"Files table" }
                                connEMD.createStatement().executeQuery("SELECT * FROM file_").let { res ->
                                    table {
                                        tr {
                                            th { +"file_guid" }
                                            th { +"storage_id" }
                                            th { +"file_path" }
                                        }
                                        while (res.next()) {
                                            tr {
                                                td { +res.getInt("file_guid").toString() }
                                                td { +res.getShort("storage_id").toString() }
                                                td { +res.getString("file_path") }
                                            }
                                        }
                                    }
                                }
                                connEMD.close()
                            }
                        }
                    }
                }
            }
        }

        config.pages.forEach { page ->

            optionallyAuthenticate {
                route(page.web_url) {
                    get {

                        val parameterBundle = ParameterBundle.buildFromCall(call, page)

                        println(this.context.principal<Principal>().toString())
                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            val softwareMap = getSoftwareMap(connEMD)

                            call.respondHtml {
                                head {
                                    title { +page.name }
                                    styleLink("/static/style.css")
                                }
                                body {
                                    a {
                                        href = "/"
                                        +"Home"
                                    }
                                    h2 { +page.name }
                                    h3 { +"Enter search criteria for events" }
                                    inputParametersForm(parameterBundle, page, softwareMap, connCondition)

                                    h3 { +"Events found:" }

                                    val res = queryEMD(
                                        parameterBundle, page, connCondition, connEMD, this,
                                        DEFAULT_LIMIT_FOR_WEB
                                    )

                                    br {}
                                    var count = 0
                                    table {
                                        tr {
                                            th { +"storage_name" }
                                            th { +"file_path" }
                                            th { +"event_number" }
                                            th { +"software_version" }
                                            th { +"period_number" }
                                            th { +"run_number" }
                                            page.parameters.forEach {
                                                th { +it.name }
                                            }
                                        }
                                        while (res.next()) {
                                            count++
                                            tr {
                                                td { +res.getString("storage_name") }
                                                td { +res.getString("file_path") }
                                                td { +res.getInt("event_number").toString() }
                                                td { +res.getString("software_version") }
                                                td { +res.getShort("period_number").toString() }
                                                td { +res.getShort("run_number").toString() }
                                                page.parameters.forEach { parameter ->
                                                    td {
                                                        when (parameter.type) {
                                                            "int" -> +res.getInt(parameter.name).toString()
                                                            "float" -> +res.getFloat(parameter.name).toString()
                                                            "bool" -> +res.getBoolean(parameter.name).toString()
                                                            "string" -> +res.getString(parameter.name)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (count == 0) {
                                        p { +"No results matching specified criteria found in the EMD database" }
                                    }

                                }
                            }
                            connEMD.close()
                        }
                    }
                }

                route(page.api_url) {

                    get("/emd") {

                        val parameterBundle = ParameterBundle.buildFromCall(call, page)
                        val connEMD = newEMDConnection(config, this.context)
                        if (connEMD == null) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            val softwareMap = getSoftwareMap(connEMD)
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

                    post("/emd") {

                        val roles = getUserRoles(config, call)
                        if (!(roles.isWriter or roles.isAdmin)) {
                            call.respond(HttpStatusCode.Unauthorized)
                        }

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

                    delete("/emd") {
                        val roles = getUserRoles(config, call)
                        if (!roles.isAdmin) {
                            call.respond(HttpStatusCode.Unauthorized)
                        } else {
                            call.respond(HttpStatusCode.NotImplemented)
                            TODO("To be implemented")
                        }
                    }

                    get("/eventFile") {
                        // Synchronous
                        // TODO Apply all filtering, build ROOT file, return it...
                        val f = File("src/main/resources/downloadFile.bin")
                        call.respondFile(f)
                    }

                    get("/eventFileRef") {
                        // Asynchronous
                        // Maybe rename to /eventFileSync, /eventFileAsync ?
                        TODO()
                    }

                }
            }
        }
    }
}

fun newEMDConnection(config: ConfigFile, context: ApplicationCall): Connection? {
    val urlEventDB =
        "jdbc:postgresql://${config.event_db.host}:${config.event_db.port}/${config.event_db.db_name}"
    // val connEMD = DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    if (config.database_auth == true) {
        val user = context.principal<UserIdPwPrincipal>()!!.name
        val pass = context.principal<UserIdPwPrincipal>()!!.pw
        try {
            val connection = DriverManager.getConnection(urlEventDB, user, pass)
            return connection
        } catch (_: PSQLException) {
            return null
        }
    } else {
        // LDAP or no auth at all
        return DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)
    }
}

private fun getUserRoles(config: ConfigFile, call: ApplicationCall): UserRoles {
    if (config.database_auth == true) {
        // Allow all on our end -- database will permit/deny based on user
        return UserRoles(isReader = true, isWriter = true, isAdmin = true)
    } else if (config.ldap_auth != null) {
        val username = call.principal<UserIdPrincipal>()?.name!!
        println("AUTHENTICATED USER NAME IS: $username")
        val ldapConn = LDAPConnection()
        ldapConn.connect(config.ldap_auth.ldap_server, config.ldap_auth.ldap_port)
        // Perform actual authentication
        ldapConn.bind(
            "uid=${config.ldap_auth.ldap_username},cn=users,cn=accounts,dc=jinr,dc=ru",
            config.ldap_auth.ldap_password
        )

        fun belongsToGroup(group: String) = (ldapConn.search(
            SearchRequest(
                //"uid=$username,cn=users,cn=accounts,dc=jinr,dc=ru"
                config.ldap_auth.user_dn_format.replace("%s", username),
                SearchScope.SUB,
                "(&(memberOf=$group))"
            )
        ).entryCount == 1)

        val isWriter = belongsToGroup(config.ldap_auth.writer_group_dn)
        val isAdmin = belongsToGroup(config.ldap_auth.admin_group_dn)
        println("Writer: $isWriter, Admin: $isAdmin")
        ldapConn.close()

        return UserRoles(isReader = true, isWriter = isWriter, isAdmin = isAdmin)
    } else {
        return UserRoles(isReader = true, isWriter = false, isAdmin = false)
    }
}


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
