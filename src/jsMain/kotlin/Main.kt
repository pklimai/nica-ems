package ru.mipt.npm.nica.ems

import kotlinx.browser.document
import react.create
import react.dom.client.createRoot

fun main() {
    createRoot(document.getElementById("root")!! as web.dom.Element).render(
        app.create()
    )
}
