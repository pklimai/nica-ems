package ru.mipt.npm.nica.emd

import kotlinx.html.DIV
import kotlinx.html.js.onClickFunction
import mui.material.FormControlVariant
import mui.material.TextField
import react.*
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.onChange


external interface Side : Props {
    var experiment: String?
}

val statsPage = fc<Side> { props ->
    val (period, setPeriod) = useState(false);
    val (soft, setSoft) = useState(false);
    val (params, setParams) = useState<Map<String, String>>()

    div("home__page") {
        div {
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
                            if(props.experiment == "BM@N"){
                                div {
                                    +"5000"
                                }
                            } else {
                                div {
                                    +"6000"
                                }
                            }
                            div {
                                +"Total"
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
                            +"8 " 
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
                            +"20.12.0 "
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
            child(chartComponent){
                attrs.exp = props.experiment.toString()
            }
        }
    }
}