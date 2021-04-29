package com.example

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {}
    }
    val dao = EventDAO(
        Database.connect(
            "jdbc:postgresql://192.168.65.52:5000/postgres",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "example"
        )
    )

    println(dao.getAllEvents())

    routing {
        get("/") {
            call.respondHtml {
                head {
                    title { +"Ktor: Docker" }
                }
                body {
                    val runtime = Runtime.getRuntime()
                    h3 { +"Ktor Netty engine: Hello, ${System.getProperty("user.name")}!" }
                    p { + "CPUs: ${runtime.availableProcessors()}. Memory free/total/max: ${runtime.freeMemory()} / ${runtime.totalMemory()} / ${runtime.maxMemory()}."}
                    hr {}
                    h3 { + "REST API"}
                    p { a(href = "/events") { +"All events"} }
                }
            }
        }
        route("/events") {
            get() {
                call.respond(mapOf("events" to dao.getAllEvents()))
            }
        }
    }
}

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
