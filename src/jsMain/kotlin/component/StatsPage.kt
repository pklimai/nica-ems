package ru.mipt.npm.nica.emd

import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import mui.material.*
import react.*
import react.dom.div
import react.dom.onChange

external interface StatsPageProps : Props {
    var experiment: String?
}

val statsPage = fc<StatsPageProps> { props ->
    val (periodOpened, setPeriodOpened) = useState(false)
    val (softOpened, setSoftOpened) = useState(false)
    val (stats, setStats) = useState<EMSStatistics>()
    val (currentPeriod, setCurrentPeriod) = useState<String>()
    val (currentSW, setCurrentSW) = useState<String>()

    useEffectOnce {
        scope.launch {
            val newStats = getStats()
            setStats(newStats)
        }
    }

    useEffect(props.experiment, stats) {  // this is dependencies list - when they change, effect is applied
        stats?.let {
            setPeriodOpened(false)
            setSoftOpened(false)
            setCurrentPeriod(
                stats.experimentStatistics[props.experiment]?.periodStats?.keys?.toList()?.maxOf { it }.toString()
            )
        }
    }

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
                            div {
                                +(stats?.experimentStatistics?.get(props.experiment)?.totalRecords?.toString() ?: "HZ")
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
                        setPeriodOpened(!periodOpened)
                    }
                    dangerousSVG(SVGHomePeriod)
                    div("stats_new_block__div") {
                        div("per") {
                            +"Period Number —"
                        }
                        div("per_number") {
                            +currentPeriod.toString()
                        }
                    }
                }
                if (periodOpened) {
                    div("home__page__stats__block3") {
                        FormControl {
                            attrs {
                                fullWidth = true
                            }
                            InputLabel {
                                +"Period Number"
                            }
                            Select {
                                attrs {
                                    size = Size.small
                                    label = ReactNode("Period Number")
                                    value = currentPeriod.unsafeCast<Nothing?>()
                                    onChange = { it: dynamic, _ ->
                                        setCurrentPeriod(it.target.value as String)
                                        setPeriodOpened(false)
                                    }
                                }
                                stats?.experimentStatistics?.get(props.experiment)?.periodStats?.keys?.forEach { perNum ->
                                    MenuItem {
                                        attrs {
                                            value = perNum.toString()
                                        }
                                        +perNum.toString()
                                    }
                                }

                            }
                        }
                    }
                }
                div("home__page__stats__block2 borders right_line") {
                    attrs.onClickFunction = {
                        setSoftOpened(!softOpened)
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
                if (softOpened) {
                    div("home__page__stats__block3") {
                        TextField {
                            attrs {
                                name = "software_version"
                                id = "software_version"
                                // value = ""
                                variant = FormControlVariant.standard
                                label = ReactNode("Software Version")
                                onChange = {
                                }
                            }
                        }
                    }
                }
            }
        }
        div("charts") {
            child(chartComponent) {
                attrs {
                    experimentStats = stats?.experimentStatistics?.get(props.experiment.toString()) // specify period, sw
                }
            }
        }
    }
}
