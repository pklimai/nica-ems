package ru.mipt.npm.nica.ems

import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import mui.material.*
import react.*
import react.dom.div
import react.dom.onChange
import ru.mipt.npm.nica.ems.component.chartSet

external interface StatsPageProps : Props {
    var experiment: String?
}

val statsPage = fc<StatsPageProps> { props ->
    val (periodOpened, setPeriodOpened) = useState(false)
    val (softOpened, setSoftOpened) = useState(false)
    val (stats, setStats) = useState<EMSStatistics>()
    val (currentPeriod, setCurrentPeriod) = useState<String>()   // TODO why String?
    val (currentSW, setCurrentSW) = useState<String>()
    val (errorStr, setErrorStr) = useState<String>("")
    val (errorVisible, setErrorVisible) = useState<Boolean>(false)

    useEffectOnce {
        scope.launch {
            val newStats = getStats()
            if (newStats.result != null) {
                setStats(newStats.result)
            } else {
                setErrorStr("Server error ${newStats.status} \n ${newStats.message}")
                setErrorVisible(true)
            }
        }
    }

    useEffect(props.experiment, stats) {  // this is dependencies list - when they change, effect is applied
        stats?.let {
            console.log("In useEffect...")
            setPeriodOpened(false)
            setSoftOpened(false)
            val newPeriod = stats.periodStats.lastOrNull()?.periodNumber.toString()
            console.log("New period: $newPeriod")
            setCurrentPeriod(newPeriod)
            val newSW = stats.periodStats.lastOrNull()?.softwareStats?.last()?.swVer.toString()
            console.log("New sw: $newSW")
            setCurrentSW(newSW)
        }
    }

    div("home__page") {
        if (errorVisible) {
            div("error") {
                div("error__login") {
                    +"Statistics obtaining failed"
                }
                errorStr.split("\n").forEach {
                    div("error__text") {
                        + it
                    }
                }
            }
        }
        div("home__page__stats") {
            div("home__page__dashboard") {
                div("home__page__dashboard__head") {
                    +"Event Metadata System"
                }
                div("home__page__dashboard__text") {
                    +"The Event Catalogue stores summary event metadata to select necessary events by criteria"
                }
            }
            div("home__page__stats__block") {
                dangerousSVG(SVGHomeRecords)
                div("home__page__stats__block__column") {
                    div("home__page__stats__block__column__stats") {
                        div {
                            +"Total Events —"
                        }
                        div {
                            +(stats?.totalRecords?.toString() ?: "0")
                        }
                    }
                }
            }
            div("home__page__stats__block borders stats_new_block") {
                attrs.onClickFunction = {
                    setPeriodOpened(!periodOpened)
                    setSoftOpened(false)
                }
                dangerousSVG(SVGHomePeriod)
                div("stats_new_block__div") {
                    div("per") {
                        +"Period Number —"
                    }
                    div("per_number") {
                        +(currentPeriod ?: "0")
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
                                    val newPeriod = it.target.value as String
                                    setCurrentPeriod(newPeriod)
                                    setPeriodOpened(false)
                                    // change software to last in list
                                    val newSW =
                                        stats?.periodStats?.filter { it.periodNumber.toString() == newPeriod }
                                            ?.firstOrNull()?.softwareStats?.last()?.swVer.toString()
                                    setCurrentSW(newSW)
                                }
                            }
                            stats?.periodStats?.map { it.periodNumber }?.sortedByDescending { it }?.forEach { perNum ->
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
                    setPeriodOpened(false)
                }
                dangerousSVG(SVGHomeSoftware)
                div("stats_new_block__div") {
                    div("per") {
                        +"Software Version — "
                    }
                    div("per_number") {
                        +(currentSW ?: "0")
                    }
                }
            }
            if (softOpened) {
                div("home__page__stats__block3") {
                    FormControl {
                        attrs {
                            fullWidth = true
                        }
                        InputLabel {
                            +"Software Version"
                        }
                        Select {
                            attrs {
                                size = Size.small
                                label = ReactNode("Software Version")
                                value = currentSW.unsafeCast<Nothing?>()
                                onChange = { it: dynamic, _ ->
                                    setCurrentSW(it.target.value as String)
                                    setSoftOpened(false)
                                }
                            }
                            stats?.periodStats
                                ?.filter { it.periodNumber.toString() == currentPeriod }
                                ?.first()?.softwareStats?.map { it.swVer }?.forEach { sw ->
                                    MenuItem {
                                        attrs {
                                            value = sw
                                        }
                                        +sw
                                    }
                                }
                        }
                    }
                }
            }
        }
        div("charts") {
            child(chartSet) {
                attrs {
                    experimentStats =
                        stats
                    period = currentPeriod?.toInt()
                    sw = currentSW
                }
            }
        }
    }
}
