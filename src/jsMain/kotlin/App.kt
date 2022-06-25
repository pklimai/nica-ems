package ru.mipt.npm.nica.emd

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.HTMLTag
import kotlinx.html.colorInput
import kotlinx.html.js.onClickFunction
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Size
import react.*
import react.dom.*
import kotlin.js.json

val DICTIONARY_PAGE = PageConfig("__dictionary", "", "", "", emptyList())
val LOGIN_PAGE = PageConfig("__login", "", "", "", emptyList())

val scope = MainScope()

val app = fc<Props> {
    val (config, setConfig) = useState<ConfigFile>()
    val (menu, setMenu) = useState(true);
    val (currentPage, setCurrentPage) = useState<PageConfig>()
    val (showStats, setShowStats) = useState(true) // show stats if true, else search and data
    //val (experiment, setExperiment) = useState("BM@N")

    // setCurrentPage(null) -- valid but causes too many re-renders here!

    val (EMDdata, setEMDdata) = useState<String>()
    val (auth, setAuth) = useState(true);
    useEffectOnce {
        scope.launch {
            setConfig(getConfig())
        }
    }
    div("wrapper") {
        header() {
            nav() {
                div("menu_icon") {
                    div("mat-button") {
                        attrs.onClickFunction = {
                            setMenu(!menu)
                        }
                        span("app-icon") {}
                    }
                    div("menu_name animbut2") {
                        div("menu_name__text") {
                            key = "Home"
                            attrs.onClickFunction = {
                                setCurrentPage(null)
                            }
                            +"BM@N Event Metadata System"
                        }
                        dangerousSVG(SVGHeaderBubbles)
                    }
                }
                span("example-spacer") {}
                if (auth) {
                    div("menu_name2") {
                        div("events_icon2") {}
                        div("login_block") {
                            div("but_login") {
                                Button {
                                    attrs {
                                        +"Sign In"
                                        variant = ButtonVariant.contained
                                        size = Size.small
                                    }
                                }
                                attrs {
                                    onClickFunction = {
                                        setCurrentPage(LOGIN_PAGE)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    div("menu_name3") {
                        div("events_icon3") {
                            div("header_svg_pad svg_anim_di") {
                                dangerousSVG(SVGDictionaryIcon)
                                attrs {
                                    onClickFunction = {
                                        setCurrentPage(DICTIONARY_PAGE)
                                    }
                                }
                                span("svg_anim_di_tooltip") {
                                    +"Dictionary"
                                }
                            }
                            div("header_line") {}
                            div("header_user_info") {
                                dangerousSVG(SVGUserPic)
                                div("header_svg_title") {
                                    +"Username"
                                }
                            }
                            div("header_line") {}
                        }
                        div {
                            div("header_svg_pad svg_anim_exit") {
                                dangerousSVG(SVGLogout)
                                attrs {
                                    onClickFunction = {
                                        setAuth(true)
                                        setCurrentPage(null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        div("main-content") {
            if (menu) {
                div("sidenav") {
                    config?.pages?.forEach { item ->
                        div {
                            key = item.name
                            div("top__search") {
                                attrs.onClickFunction = {
                                    setCurrentPage(item)
                                    setShowStats(true)
                                }
                                div {
                                    +item.name
                                    if (currentPage?.name == item.name && showStats) {
                                        attrs["style"] = json("color" to "#2862ff")
                                    }
                                }
                            }
                            div {
                                child(searchComponent) {
                                    attrs.highlighted = (currentPage == item && !showStats)
                                }
                                attrs.onClickFunction = {
                                    setCurrentPage(item)
                                    setShowStats(false)
                                    // Clear data for table
                                    setEMDdata(null)
                                }
                            }
                        }
                    }
                }
            }
            //if (currentPage == null) {
            if (showStats) {
                child(homePage) {
                    attrs.experiment = currentPage?.name?.split(" ")?.first() ?: "BM@N"  // TODO
                }
            } else if (currentPage == LOGIN_PAGE) {
                child(login)
            } else if (currentPage == DICTIONARY_PAGE) {
                child(dictionary) // color: #e13a3a;
            } else {
                child(emdPage) {
                    attrs.pageConfig = currentPage!!
                    attrs.EMDdata = EMDdata
                    attrs.setEMDdata = { it: String? ->
                        setEMDdata(it)
                    }
                    attrs.condition_db = config?.condition_db
                }
            }
        }
        footer {
            div("footer__home__icon") {
                a(href = "/") {
                    i("bx bx-home") {}
                }
            }
            span("example-spacer") {}
            div {
                a(href = "https://bmn.jinr.ru/", target = "_blank") {
                    img(src = "img/favicon.png") {}
                }
            }
        }
    }
}