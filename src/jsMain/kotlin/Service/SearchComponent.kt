package ru.mipt.npm.nica.emd

import react.Props
import react.createElement
import react.dom.div
import react.dom.svg
import react.fc

external interface SearchComponentProps: Props {
    var highlighted: Boolean
}

val searchComponent = fc<SearchComponentProps> { props ->
    div("search") {
        svg("search__svg") {
            attrs["width"] = 29
            attrs["height"] = 29
            attrs["viewBox"] = "0 0 29 29"
            attrs["xmlns"] = "http://www.w3.org/2000/svg"
            attrs["fill"] = if (props.highlighted) "#5ba6ff" else "#928787d4"
            child(createElement("path", SVGPathAttrs("evenodd", "evenodd", SVGSearchEvents)))
        }
        div("search__name") {
            +"Search Events"
        }
    }
}
