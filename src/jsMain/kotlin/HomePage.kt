package ru.mipt.npm.nica.emd

import Highcharts
import HighchartsReact
import kotlinext.js.jso
import kotlinx.html.DIV
import kotlinx.html.js.onClickFunction
import mui.material.FormControlVariant
import mui.material.TextField
import react.*
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.onChange
import kotlin.js.json


val homePage = fc<Props> {
    val (period, setPeriod) = useState(false);
    val (soft, setSoft) = useState(false);
    val (params, setParams) = useState<Map<String, String>>()
    div("home__page") {
        div() {
            div("home__page__dashboard") {
                div("home__page__dashboard__head") {
                    +"Event Metadata System"
                }
                div("home__page__dashboard__text") {
                    +"The Event Catalogue stores summary event metadata to select necessary events by criteria"
                }
            }
            div("home__page__stats") {
                div("home__page__stats__block") {
                    dangerousSVG(SVGHomeRecords)
                    div("home__page__stats__block__column") {
                        div("home__page__stats__block__column__stats") {
                            div() {
                                +"5000"
                            }
                            div {
                                +"Records"
                            }
                        }
                        div("event_metadata") {
                            +"event metadata"
                        }
                    }
                }
                div("home__page__stats__block borders stats_new_block") {
                    attrs.onClickFunction = {
                        setPeriod(!period)
                    }
                    dangerousSVG(SVGHomePeriod)
                    div("stats_new_block__div") {
                        div("per") {
                            +"Period Number —"
                        }
                        div("per_number") {
                            +"8 " //_attention_ из базы брать последнее значение и по умолчанию отстраивать чарты
                        }
                    }
                }
                if (period) {
                    fun RDOMBuilder<DIV>.textSelect(paramName: String, labelString: String = "") {
                        TextField {
                            attrs {
                                name = paramName
                                id = paramName
                                value = params?.get(paramName) ?: ""    /// ? to test
                                variant = FormControlVariant.standard
                                label = ReactNode(labelString)
                                onChange = { }
                            }
                        }
                    }
                    div("home__page__stats__block3") {
                        textSelect("period number", "Period Number")
                    }
                }
                div("home__page__stats__block2 borders right_line") {
                    attrs.onClickFunction = {
                        setSoft(!soft)
                    }
                    dangerousSVG(SVGHomeSoftware)
                    div("stats_new_block__div") {
                        div("per") {
                            +"Software Version — "
                        }
                        div("per_number") {
                            +"20.12.0 " //_attention_ а если активировали только 1 элемент(period), а софт нет то в зарос брать по умолчанию
                        }
                    }
                }
                if (soft) {
                    fun RDOMBuilder<DIV>.textSelect(paramName: String, labelString: String = "") {
                        TextField {
                            attrs {
                                name = paramName
                                id = paramName
                                value = params?.get(paramName) ?: ""    /// ? to test
                                variant = FormControlVariant.standard
                                label = ReactNode(labelString)
                                onChange = { }
                            }
                        }
                    }
                    div("home__page__stats__block3") {
                        textSelect("software_version", "Software Version")
                    }
                }
            }
        }
        div("charts") {
            div("chart_bloks"){ // https://www.postgresql.org/docs/current/postgres-fdw.html
                div("chart_bloks__title_beam_energy"){+"Beam {{i.beam}} ( E = {{i.energy}} GeV/n )"}
                div("chart_bloks_title_total"){+"Total: {{i.total}} MEvents"}
                div("chart_blok_svg"){
                    HighchartsReact {
                        attrs {
                            this.highcharts = Highcharts
                            this.options = json(
                                "title" to "",
                                "chart" to json(
                                    "height" to 220,
                                    "width" to 320, //сейчас широко, но когда заполнятся поля будет ровно
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
                                "tooltip" to js("{style: { color: '#1b1818' }, backgroundColor: '#ffffff', borderRadius: 5, borderWidth: 3, headerFormat: '<small></small>', pointFormat: '{point.name}:  {point.y} MEvents' }"),
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
                                            "format" to "{point.name}: <i>{point.y} MEvents</i>"
                                        )
                                    )
                                ),
                                "series" to arrayOf<dynamic>(
                                    json(
                                        "type" to "pie",
                                        "name" to "",
                                        "data" to arrayOf(1, 2, 3, 4, 5)
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

//_attention_ Проблема с разворачиванием баз в condition_db нет таблиц, а event даже не развернулась в докере
