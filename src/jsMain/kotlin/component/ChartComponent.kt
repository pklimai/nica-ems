package ru.mipt.npm.nica.ems

import react.Props
import react.dom.div
import react.fc
import Highcharts
import HighchartsReact
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlin.js.json

external interface ChartComponentProps : Props {
    var statGraph: StatGraph?
}

val chartComponent = fc<ChartComponentProps> { props ->
    div("chart_bloks") {
        div("chart_bloks__title_beam_energy") {
            +(props.statGraph?.title1 ?: "")    // "Beam {{i.beam}} ( E = {{i.energy}} GeV/n )"
        }
        div("chart_bloks_title_total") {
            +(props.statGraph?.title2 ?: "")    // "Total: {{i.total}} MEvents"
        }

        // Could not build it better...
        val lst = buildList<dynamic> {
            props.statGraph?.slices?.forEach { slice ->
                add(
                    json(
                        "name" to slice.name,
                        "y" to slice.value
                    )
                )
            }
        }
        val arr = arrayOf<dynamic>(lst.size)
        lst.forEachIndexed { index, dyn ->
            arr[index] = dyn
        }


        div("chart_blok_svg") {
            HighchartsReact {
                attrs {
                    this.highcharts = Highcharts
                    this.options = json(
                        "title" to "",
                        "chart" to json(
                            "height" to 220,
                            "width" to 320,
                            "borderWidth" to 0,
                            "plotBackgroundColor" to null,
                            "plotBorderWidth" to null,
                            "margin" to 0,
                            "borderRadius" to 0,
                            "plotShadow" to false,
                            "spacingTop" to 0,
                            "spacingBottom" to 0,
                            "spacingLeft" to 0,
                            "spacingRight" to 0,
                        ),
                        "credits" to json(
                            "enabled" to false
                        ),
                        "tooltip" to js("{style: { color: '#1b1818' }, backgroundColor: '#ffffff', borderRadius: 5, borderWidth: 3, headerFormat: '<small></small>', pointFormat: '{point.name}: {point.y} events' }"), // MEvents
                        "plotOptions" to json(
                            "pie" to json(
                                "allowPointSelect" to true,
                                "accessibility" to false,
                                "cursor" to "pointer",
                                "size" to 100,
                                //"colors" to arrayOf("#ff0000", "#00ff00", "#0000ff"),   this.bname[i].color,
                                "dataLabels" to json(
                                    "enabled" to true,
                                    "distance" to 15,
                                    "style" to js("{ fontSize: 'inherit', fontWeight: 'normal', fontFamily: 'Lato, sans-serif', lineHeight: '18px' }"),
                                    "format" to "{point.name}: <i>{point.y} events</i>" // MEvents
                                )
                            )
                        ),
                        "series" to arrayOf<dynamic>(
                            json(
                                "type" to "pie",
                                "name" to "AAA",

                                "data" to arr   // TODO make not so ugly

//                                "data" to arrayOf<dynamic>(
//                                    json(
//                                        "name" to "Chrome",
//                                        "y" to 15
//                                    ),
//                                    json(
//                                        "name" to "Firefox",
//                                        "y" to 25
//                                    ),
//                                    json(
//                                        "name" to "IE",
//                                        "y" to 1
//                                    )
//                                )
                            )
                        )
                    )
                }
            }
        }
    }
}

// https://www.postgresql.org/docs/current/postgres-fdw.html
