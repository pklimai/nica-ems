package com.example

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database
import java.sql.DriverManager

val URL = "jdbc:postgresql://192.168.65.52:5000/event_db"
val DRIVER = "org.postgresql.Driver"
val USER = "postgres"
val PASS = "example"

class SoftwareMap (val id_to_str: Map<Short, String>, val str_to_id: Map<String, Short>)

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
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    val dao = EventDAO(
        Database.connect(URL, driver = DRIVER, user = USER, password = PASS)
    )
    val conn = DriverManager.getConnection(URL, USER, PASS)

    // println(dao.getAllEvents())

    // println("Working Directory = ${System.getProperty("user.dir")}")

    routing {
        static("static") {
            // http://127.0.0.1:8080/static/style.css
            files("src/main/resources/static/css")
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +"Ktor: Docker" }
                    styleLink("static/style.css")
                }
                body {
                    val runtime = Runtime.getRuntime()
                    h3 { +"Ktor Netty engine: Hello, ${System.getProperty("user.name")}!" }
                    p { +"CPUs: ${runtime.availableProcessors()}. Memory free/total/max: ${runtime.freeMemory()} / ${runtime.totalMemory()} / ${runtime.maxMemory()}." }
                    hr {}
                    h3 { +"REST API" }
                    p { a(href = "/event_api/v1/exposed/all_events") { +"Exposed - All events" } }
                    p { a(href = "/event_api/v1/exposed/all_joined") { +"Exposed - All events joined" } }
                    p { a(href = "/event_api/v1/raw/joined") { +"Raw - All events joined" } }
                    hr {}
                    h3 { +"WebUI" }
                    p { a(href = "/dictionaries") { +"Dictionaries" } }
                    p { a(href = "/search_form") { +"Search Form" } }
                }
            }
        }

        route("/dictionaries") {
            get() {
                call.respondHtml {
                    +"TODO show small tables here"
                }
            }
        }

        route("/search_form") {
            get() {
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

                val tracks: Int? = try {
                    call.parameters["tracks"]?.toInt()
                } catch (e: java.lang.NumberFormatException) {
                    null
                }

                val softwareMap = getSoftwareMap(conn)

                call.respondHtml {
                    head {
                        title { +"Ktor event index" }
                        styleLink("static/style.css")
                    }
                    body {
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
                            select {
                                id = "software_version"
                                name = "software_version"
                                softwareMap.str_to_id.keys.forEach {
                                    option {
                                        value = it
                                        if (it == software_version) {
                                            selected = true
                                        }
                                        + it   // NB: Depends on the order!
                                    }
                                }
                            }

                            br { }
                            label { +"Tracks" }
                            textInput {
                                id = "tracks"
                                name = "tracks"  // required for parameter to be sent in URL
                                tracks?.let {
                                    value = tracks.toString()
                                }
                            }

                            br { }
                            submitInput {
                                value = "Submit"
                                formMethod = InputFormMethod.get
                            }

                        }


                        h3 { +"Events found:" }

                        var query =
                            """SELECT software_version, event_number, file_path, storage_name, period_number, run_number, track_number
                        FROM bmn_event 
                        INNER JOIN software_ ON bmn_event.software_id = software_.software_id
                        INNER JOIN file_ ON bmn_event.file_guid = file_.file_guid
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
                        tracks?.let {
                            filterCriteria.add("track_number = $tracks")
                        }
                        if (filterCriteria.isNotEmpty()) {
                            query += "WHERE " + filterCriteria.joinToString(" AND ")
                        }

                        // p { +query }
                        // p { +software_version!! }

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
                                th { +"track_number" }
                            }

                            while (res.next()) {
                                tr {
                                    td { +res.getString("storage_name") }
                                    td { +res.getString("file_path") }
                                    td { +res.getInt("event_number").toString() }
                                    td { +res.getString("software_version") }
                                    td { +res.getShort("period_number").toString() }
                                    td { +res.getShort("run_number").toString() }
                                    td { +res.getInt("track_number").toString() }
                                }
                            }
                        }
                    }
                }
            }
            post() {
                call.respondHtml {
                    body {
                        h2 { +"POST worked!" }
                    }
                }
            }

        }


        route("/event_api/v1/raw") {
            get() {
                val stmt = conn.createStatement()
                val res = stmt.executeQuery("SELECT * FROM bmn_event")
                val lstEvents = ArrayList<Event>()
                while (res.next()) {
                    lstEvents.add(
                        Event(
                            res.getInt("file_guid"),
                            res.getInt("event_number"),
                            res.getShort("software_id"),
                            res.getShort("period_number"),
                            res.getShort("run_number"),
                            res.getInt("track_number")
                        )
                    )
                }
                call.respond(mapOf("events_raw" to lstEvents))
            }

            get("/joined") {
                val stmt = conn.createStatement()
                val res = stmt.executeQuery(
                    """SELECT software_version, event_number, file_path, storage_name, period_number, run_number, track_number
                        FROM bmn_event INNER JOIN software_  
                        ON bmn_event.software_id = software_.software_id
                        INNER JOIN file_ ON bmn_event.file_guid = file_.file_guid
                        INNER JOIN storage_ ON file_.storage_id = storage_.storage_id
                        """
                )

                val lstEvents = ArrayList<EventRaw>()
                while (res.next()) {
                    lstEvents.add(
                        EventRaw(
                            res.getString("storage_name"),
                            res.getString("file_path"),
                            res.getInt("event_number"),
                            res.getString("software_version"),
                            res.getShort("period_number"),
                            res.getShort("run_number"),
                            res.getInt("track_number")
                        )
                    )
                }
                call.respond(mapOf("events_raw_joined" to lstEvents))

            }
        }


        route("/event_api/v1/exposed") {

            route("/all_events") {
                get() {
                    call.respond(mapOf("events" to dao.getAllEvents()))
                }
            }

            route("/all_joined") {
                get() {
                    call.respond(mapOf("events_joined" to dao.getAllEventsJoined()))
                }
            }

            // Example URL -- http://127.0.0.1:8080/events/4/10
            get("/{file_ptr}/{event_num}") {
                val file_ptr = call.parameters["file_ptr"]?.toInt()
                val event_num = call.parameters["event_num"]?.toInt()
                if (file_ptr != null && event_num != null) {
                    val resp = dao.getEvent(file_guid = file_ptr, event_num = event_num)
                    if (resp != null) {
                        call.respond(resp)
                    } else {
                        call.respond("No such event found!")
                    }
                }
            }

            // Example URL -- http://127.0.0.1:8080/events/getevent?file_ptr=4&event_num=10
            get("/getevent") {
                val file_ptr = call.parameters["file_ptr"]?.toInt()
                val event_num = call.parameters["event_num"]?.toInt()
                if (file_ptr != null && event_num != null) {
                    val resp = dao.getEvent(file_guid = file_ptr, event_num = event_num)
                    if (resp != null) {
                        call.respond(resp)
                    } else {
                        call.respond("No such event found!")
                    }
                } else {
                    call.respond("file_ptr and event_num are required here")
                }
            }

            // Example URL -- http://127.0.0.1:8080/events/search?period=7&run=5000
            get("/search") {
                val period = call.parameters["period"]?.toInt()
                val run = call.parameters["run"]?.toInt()
                if (period != null && run != null) {
                    call.respond(mapOf("events" to dao.searchEvents(period, run)))
                }
            }

            // **NB**: Content-Type is mandatory
            // POST http://127.0.0.1:8080/events/create
            // Content-Type: application/json
            //
            // { "file_ptr": 10, "event_num": 22, "period": 7, "run": 5000, "software_id": 1, "all_tracks": 55 }
            post("/create") {
                val event = call.receive<Event>()
                dao.createEvent(
                    event.file_guid,
                    event.event_number,
                    event.period_number,
                    event.run_number.toInt(),
                    event.software_id.toInt(),
                    event.track_number
                )
                call.respond("Event created!")
            }

            /*
             { "events": [
               { "file_ptr": 10, "event_num": 31, "period": 7, "run": 5000, "software_idr": 1, "all_tracks": 55 },
               { "file_ptr": 10, "event_num": 32, "period": 7, "run": 5000, "software_id": 1, "all_tracks": 55 }
             ] }
            */
            post("/create-multiple") {
                val events = call.receive<EventList>()
                events.events.forEach { event ->
                    dao.createEvent(
                        event.file_guid,
                        event.event_number,
                        event.period_number,
                        event.run_number.toInt(),
                        event.software_id.toInt(),
                        event.track_number
                    )
                }
                call.respond("${events.events.size} events created!")
            }

        }


    }
}


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
