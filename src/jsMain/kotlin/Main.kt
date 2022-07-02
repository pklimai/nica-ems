package ru.mipt.npm.nica.emd

import kotlinx.browser.document
import react.create
import react.dom.client.createRoot

fun main() {
    createRoot(document.getElementById("root")!!).render(
        app.create()
    )
}
