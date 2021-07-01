package com.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import java.io.File
import java.sql.DriverManager

// val DRIVER = "org.postgresql.Driver"

fun Application.main() {

    val mapper = ObjectMapper(YAMLFactory())
    mapper.findAndRegisterModules()
    val config = mapper.readValue(File("src/main/resources/event-config.yaml"), ConfigFile::class.java)

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    install(Compression)

    val urlEventDB =
        "jdbc:postgresql://${config.event_db.host}:${config.event_db.port}/${config.event_db.db_name}"
    val conn = DriverManager.getConnection(urlEventDB, config.event_db.user, config.event_db.password)

    // If null, do not use event preselection from the Condition Database
    val connCondition = config.condition_db?.let {
        val urlConditionDB =
            "jdbc:postgresql://${config.condition_db.host}:${config.condition_db.port}/${config.condition_db.db_name}"
        DriverManager.getConnection(urlConditionDB, config.condition_db.user, config.condition_db.password)
    }

    println("Conn to Condition DB: $connCondition")
    connCondition?.let {
        println(getRunsBy(it, "Ar", "Al", 2.6f))
    }

    // println("Working Directory = ${System.getProperty("user.dir")}")

    routing {
        static("static") {
            // http://127.0.0.1:8080/static/style.css
            files("src/main/resources/static/css")
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +config.title }
                    styleLink("/static/style.css")
                }
                body {
                    // val runtime = Runtime.getRuntime()
                    // h3 { +"Ktor Netty engine: Hello, ${System.getProperty("user.name")}!" }
                    // p { +"CPUs: ${runtime.availableProcessors()}. Memory free/total/max: ${runtime.freeMemory()} / ${runtime.totalMemory()} / ${runtime.maxMemory()}." }
                    h2 { +config.title }

                    config.pages.forEach {
                        h3 { +it.name }
                        h5 { +"REST API" }
                        p { a(href = it.api_url + "/events") { +"API - get all events" } }
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

        route("/dictionaries") {
            get {
                call.respondHtml {
                    +"TODO show small tables here"
                }
            }
        }

        val periodConfig = ParameterConfig("period_number", "int", true, "Period Number")
        val runConfig = ParameterConfig("run_number", "int", true, "Run Number")

        config.pages.forEach { page ->

            route(page.web_url) {
                get {

                    val period_number = Parameter.fromParameterConfig(periodConfig, call.parameters["period_number"])
                    val run_number = Parameter.fromParameterConfig(runConfig, call.parameters["run_number"])

                    val software_version: String? = call.parameters["software_version"]

                    // Mapping of optional parameter name to its value as a string (possibly range, etc.)
                    val parametersSupplied = HashMap<String, Parameter>()
                    page.parameters.forEach { parameterConfig ->
                        if (parameterConfig.name in call.parameters) {
                            val parameterValue = call.parameters[parameterConfig.name].toString()
                            if (parameterValue.isNotBlank()) {
                                parametersSupplied[parameterConfig.name] =
                                    Parameter.fromParameterConfig(parameterConfig, parameterValue)!!
                            }
                        }
                    }

                    val softwareMap = getSoftwareMap(conn)

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
                            form {

                                parameterInput(periodConfig, period_number)
                                parameterInput(runConfig, run_number)

                                label { +"Software Version" }
                                // TODO add null selection
                                select {
                                    id = "software_version"
                                    name = "software_version"
                                    softwareMap.str_to_id.keys.forEach {
                                        option {
                                            value = it
                                            if (it == software_version) {
                                                selected = true
                                            }
                                            +it   // NB: Depends on the order!
                                        }
                                    }
                                }
                                br {}

                                page.parameters.forEach { parameter ->
                                    parameterInput(parameter, parametersSupplied[parameter.name])
                                }

                                br { }
                                submitInput {
                                    value = "Submit"
                                    formMethod = InputFormMethod.get
                                }

                            }

                            h3 { +"Events found:" }

                            val et = page.db_table_name
                            var query =
                                """SELECT * FROM $et 
                                    INNER JOIN software_ ON $et.software_id = software_.software_id
                                    INNER JOIN file_ ON $et.file_guid = file_.file_guid
                                    INNER JOIN storage_ ON file_.storage_id = storage_.storage_id
                                """
                            val filterCriteria = ArrayList<String>()
                            period_number?.let {
                                filterCriteria.add(it.generateSQLWhere())
                            }

                            run_number?.let {
                                filterCriteria.add(it.generateSQLWhere())
                            }
                            software_version?.let {
                                filterCriteria.add("software_version = '$software_version'")
                            }

                            parametersSupplied.forEach {
                                filterCriteria.add(it.value.generateSQLWhere())
                            }

                            if (filterCriteria.isNotEmpty()) {
                                query += "WHERE " + filterCriteria.joinToString(" AND ")
                            }

                            print(query)

                            val res = conn.createStatement().executeQuery(query)

                            br {}
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
                                    tr {
                                        td { +res.getString("storage_name") }
                                        td { +res.getString("file_path") }
                                        td { +res.getInt("event_number").toString() }
                                        td { +res.getString("software_version") }
                                        td { +res.getShort("period_number").toString() }
                                        td { +res.getShort("run_number").toString() }
                                        page.parameters.forEach { parameter ->
                                            // TODO Types
                                            td {
                                                when (parameter.type) {
                                                    "int" -> +res.getInt(parameter.name).toString()
                                                    "float" -> +res.getFloat(parameter.name).toString()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                /*
                post {
                    call.respondHtml {
                        body {
                            h2 { +"POST worked!" }
                        }
                    }
                }
                */

            }


            route(page.api_url) {

                get("/events") {
                    val stmt = conn.createStatement()
                    val et = page.db_table_name

                    // TODO: Check how joins affect the performance. Consider doing DIY joins?
                    val res = stmt.executeQuery(
                        """SELECT * FROM $et INNER JOIN software_  
                            ON $et.software_id = software_.software_id
                            INNER JOIN file_ ON $et.file_guid = file_.file_guid
                            INNER JOIN storage_ ON file_.storage_id = storage_.storage_id
                        """
                    )

                    val lstEvents = ArrayList<EventRepr>()
                    while (res.next()) {
                        val paramMap = HashMap<String, Any>()

                        page.parameters.forEach() {
                            paramMap[it.name] = res.getInt(it.name)
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
                    call.respond(mapOf("events" to lstEvents))

                }
                /*
                [ {
                  "reference": {
                    "storage_name": "data1",
                    "file_path": "/tmp/file1",
                    "event_number": 1
                  },
                  "software_version": "19.1",
                  "period_number": 7,
                  "run_number": 5000,
                  "parameters": {
                    "track_number": 20
                  }
                } ]
                */
                post("/events") {
                    val events = call.receive<Array<EventRepr>>()
                    val swMap = getSoftwareMap(conn)
                    val storageMap = getStorageMap(conn)
                    events.forEach { event ->
                        println("Create event: $event")
                        val swid = swMap.str_to_id[event.software_version]
                        val file_path = event.reference.file_path
                        val storage_name = event.reference.storage_name
                        val storage_id = storageMap.str_to_id[storage_name]

                        // get file_guid

                        val res = conn.createStatement().executeQuery(
                            """SELECT file_guid FROM file_ WHERE 
                             storage_id = $storage_id AND file_path = '$file_path'
                            """.trimMargin()
                        )
                        if (res.next()) {
                            val file_guid = res.getInt("file_guid")
                            println("File GUID = $file_guid")

                            val query = """
                                INSERT INTO ${page.db_table_name} 
                                (file_guid, event_number, software_id, period_number, run_number,
                                 ${page.parameters.joinToString(transform = { it.name }, separator = ", ")} )
                                 VALUES ($file_guid, ${event.reference.event_number}, $swid, ${event.period_number},
                                   ${event.run_number}, 
                                   ${page.parameters.map { event.parameters[it.name].toString() }.joinToString(", ")})
                        """.trimIndent()
                            print(query)
                            conn.createStatement().executeUpdate(query)
                        } else {
                            println("Could not extract file GUID...")
                            call.respond("Not OK")
                        }
                    }
                    call.respond("Events were created")
                }


            }

        }
    }
}


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
