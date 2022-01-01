import kotlinx.browser.document

import io.ktor.http.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*

import kotlinx.browser.window
import kotlinx.coroutines.*


val endpoint = window.location.origin // only needed until https://github.com/ktorio/ktor/issues/1695 is resolved

suspend fun main() {

    val client = HttpClient{
    }

    val s = client.get<String>(endpoint + "/event_api/v1/test/emd")

    document.getElementById("root")?.innerHTML = "Hello, Kotlin/JS! <br> $s"
}
