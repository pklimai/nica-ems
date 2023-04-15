package ru.mipt.npm.nica.emd

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

// Legacy pure static HTML-based pages
fun Route.legacyHomePage(config: ConfigFile) = route("/legacy") {
    get {
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
                    p { a(href = it.api_url + "/${EVENT_ENTITY_API_NAME}") { +"API - get all events" } }
                    h5 { +"WebUI" }
                    p { a(href = it.api_url + "/legacy_web") { +"Search Form" } }
                    hr {}
                }

                h3 { +"Auxiliary data" }
                p { a(href = "/legacy/dictionaries") { +"Dictionaries" } }

            }
        }
    }
}

fun Route.legacyDictionaries(config: ConfigFile) = route("/legacy/dictionaries") {
    get {
        call.respondHtml {
            head {
                title { +config.title }
                styleLink("/static/style.css")
            }
            body {
                a {
                    href = "/legacy"
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

fun Route.legacyPage(page: PageConfig, config: ConfigFile, connCondition: java.sql.Connection?) =
    route(page.api_url + "/legacy_web") {
    // config parameter for URL was dropped, so we use api_url plus "/legacy_web" to make it unique
        get {

            val parameterBundle = ParameterBundle.buildFromCall(call, page)

            // println(this.context.principal<Principal>().toString())
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
                            href = "/legacy"
                            +"Home"
                        }
                        h2 { +page.name }
                        h3 { +"Enter search criteria for events" }
                        inputParametersForm(parameterBundle, page, softwareMap, connCondition)

                        h3 { +"Events found:" }

                        val res = queryEMD(
                            parameterBundle, page, connCondition, connEMD, this, DEFAULT_LIMIT_FOR_WEB
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

