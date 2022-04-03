package ru.mipt.npm.nica.emd

import kotlinx.browser.document
import react.dom.render

fun main() {

    render(document.getElementById("root")!!) {
        child(app)
    }

}
