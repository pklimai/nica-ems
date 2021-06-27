package com.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import java.io.File
import java.sql.DriverManager

// val DRIVER = "org.postgresql.Driver"

class SoftwareMap(val id_to_str: Map<Short, String>, val str_to_id: Map<String, Short>)

fun getSoftwareMap(conn: java.sql.Connection): SoftwareMap {
    val query = "SELECT * FROM software_"
    val res = conn.createStatement().executeQuery(query)
    val idToStr = HashMap<Short, String>()
    val strToId = HashMap<String, Short>()
    while (res.next()) {
        val id = res.getShort("software_id")
        val ver = res.getString("software_version")
        idToStr[id] = ver
        strToId[ver] = id
    }
    return SoftwareMap(idToStr, strToId)
}


fun Application.main() {

    val mapper = ObjectMapper(YAMLFactory())
    mapper.findAndRegisterModules()
    val config = mapper.readValue(File("src/main/resources/event-config.yaml"), ConfigFile::class.java)

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    val url = "jdbc:postgresql://${config.db_connection.host}:${config.db_connection.port}/${config.db_connection.db_name}"
    val conn = DriverManager.getConnection(url, config.db_connection.user, config.db_connection.password)

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

        route("/dictionaries") {
            get {
                call.respondHtml {
                    +"TODO show small tables here"
                }
            }
        }

        config.pages.forEach { page ->

            route(page.web_url) {
                get {
                    val period = try {
                        call.parameters["period"]?.toInt()
                    } catch (e: java.lang.NumberFormatException) {
                        null
                    }
                    val run: Int? = try {
                        call.parameters["run"]?.toInt()
                    } catch (e: java.lang.NumberFormatException) {
                        null
                    }
                    val software_version: String? = try {
                        call.parameters["software_version"]
                    } catch (e: java.lang.NumberFormatException) {
                        null
                    }

                    // Mapping of parameter name to its value as a string (possibly range, etc.)
                    val parameterStrs = HashMap<String, String>()
                    page.parameters.forEach { parameter ->
                        if (parameter.name in call.parameters) {
                            parameterStrs[parameter.name] = call.parameters[parameter.name].toString()
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
                                + "Home"
                            }
                            h2 { +page.name }
                            h3 { +"Enter search criteria for events" }
                            form {
                                label { +"Period" }
                                textInput {
                                    id = "period"
                                    name = "period"  // required for parameter to be sent in URL
                                    period?.let {
                                        value = period.toString()
                                    }
                                }
                                br { }
                                label { +"Run" }
                                textInput {
                                    id = "run"
                                    name = "run"  // required for parameter to be sent in URL
                                    run?.let {
                                        value = run.toString()
                                    }
                                }
                                br { }
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

                                page.parameters.forEach { parameter ->
                                    br {}
                                    label { + parameter.web_name }
                                    textInput {
                                        id = parameter.name
                                        name = parameter.name
                                        parameterStrs[parameter.name]?.let {
                                            value = parameterStrs[parameter.name]!!
                                        }
                                    }
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
                            period?.let {
                                filterCriteria.add("period_number = $period")
                            }
                            run?.let {
                                filterCriteria.add("run_number = $run")
                            }
                            software_version?.let {
                                filterCriteria.add("software_version = '$software_version'")
                            }
                            /* TODO filtering based on parameters
                            tracks?.let {
                                filterCriteria.add("track_number = $tracks")
                            }
                            */
                            parameterStrs.forEach {
                                if (it.value.isNotEmpty())
                                    filterCriteria.add("${it.key} = ${it.value}")
                            }

                            if (filterCriteria.isNotEmpty()) {
                                query += "WHERE " + filterCriteria.joinToString(" AND ")
                            }

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
                                        th { + it.name }
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
                                        page.parameters.forEach {
                                            // TODO Types
                                            td { +res.getInt(it.name).toString() }
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
            }

        }
    }
}


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
