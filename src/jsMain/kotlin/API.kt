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

val jsonClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun getConfig(): ConfigFile {
    return jsonClient.get(endpoint + CONFIG_URL).body()
}

suspend fun getEMD(api_url: String, username: String, password: String): String {
    val stringClient = HttpClient(Js) {
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
    val res = stringClient.get(endpoint + api_url).bodyAsText()
    stringClient.close()
    return res
}

suspend fun getSoftwareVersions(): Array<SoftwareVersion> {
    return jsonClient.get(endpoint + SOFTWARE_URL).body()
}

suspend fun getStorages(): Array<Storage> {
    return jsonClient.get(endpoint + STORAGE_URL).body()
}
