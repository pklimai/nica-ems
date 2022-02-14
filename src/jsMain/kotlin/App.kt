import csstype.FlexGrow
import csstype.px
import kotlinext.js.jso
import react.*
import react.dom.*
import kotlinx.html.js.*
import kotlinx.coroutines.*
import mui.material.*
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML


val scope = MainScope()

val app = fc<Props> { props ->

    val (config, setConfig) = useState<ConfigFile>()

    val (currentPage, setCurrentPage) = useState<PageConfig>()
    // setCurrentPage(null) -- valid but causes too many re-renders here!

    val (EMDdata, setEMDdata) = useState<String>()


    useEffectOnce {
        scope.launch {
            setConfig(getConfig())
        }
    }

    Box {
        attrs {
            sx = jso {
                flexGrow = FlexGrow(1.0)
                marginBottom = 25.px
            }

        }

        AppBar {
            attrs {
                position = AppBarPosition.static
            }

            Toolbar {

                Typography {
                    attrs {
                        sx = jso { flexGrow = FlexGrow(1.0) }
                        variant = "h6"
                        component = ReactHTML.div
                    }

                    + (config?.title ?: "EMS")
                }

                Button {
                    attrs {
                        color = ButtonColor.inherit
                    }

                    +"Login"
                }
            }
        }
    }


    div("container-for-three") {
        // kotlin-react-dom-legacy is used here
        div("div-select-catalog") {
            Card {
                attrs {
                    style = jso {
                        paddingLeft = 25.px
                        paddingRight = 25.px
                    }
                }

                ul {
                    config?.pages?.forEach { item ->
                        li {
                            key = item.name
                            attrs.onClickFunction = {
                                setCurrentPage(item)
                                // Clear data for table
                                setEMDdata(null)
                            }
                            h4 {
                                +"[${item.name}] ${item.api_url} "
                            }
                        }
                    }

                    li {
                        key = "Home"
                        attrs.onClickFunction = {
                            setCurrentPage(null)
                        }
                        h4 {
                            +"Home"
                        }
                    }

                }
            }
        }

        if (currentPage == null) {
            child(homePage)
        } else {
            child(emdPage) {
                attrs.pageConfig = currentPage
                attrs.EMDdata = EMDdata
                attrs.setEMDdata = { it: String? ->
                    setEMDdata(it)
                }
                attrs.condition_db = config?.condition_db
            }
        }


    }


}

