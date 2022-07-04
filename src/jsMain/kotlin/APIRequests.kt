package ru.mipt.npm.nica.emd

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.engine.js.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window

val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

suspend fun getConfig(): ConfigFile {
    val jsonClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    val res = jsonClient.get(endpoint + CONFIG_URL).body<ConfigFile>()
    jsonClient.close()
    return res
}

suspend fun getEMD(api_url: String, config: ConfigFile?, username: String, password: String): String {
    val stringClient = HttpClient(Js) {
        if (config?.authRequired() == true) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    // enable sending credentials in the initial request without waiting for a 401 (Unauthorized) response:
                    sendWithoutRequest { request -> true }
                }
            }
        }
    }
    val res = stringClient.get(endpoint + api_url).bodyAsText()
    stringClient.close()
    return res
}

fun jsonClientWithOptionalAuth(config: ConfigFile?, username: String, password: String): HttpClient  =
    HttpClient {
        install(ContentNegotiation) {
            json()
        }
        if (config?.authRequired() == true) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = username, password = password)
                    }
                    sendWithoutRequest { request -> true }
                }
            }
        }
    }

suspend fun getSoftwareVersions(config: ConfigFile?, username: String, password: String): Array<SoftwareVersion> {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val res = jsonClient.get(endpoint + SOFTWARE_URL).body<Array<SoftwareVersion>>()
    jsonClient.close()
    return res
}

suspend fun getStorages(config: ConfigFile?, username: String, password: String): Array<Storage> {
    val jsonClient = jsonClientWithOptionalAuth(config, username, password)
    val res = jsonClient.get(endpoint + STORAGE_URL).body<Array<Storage>>()
    jsonClient.close()
    return res
}
