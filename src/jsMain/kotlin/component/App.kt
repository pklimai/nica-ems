package ru.mipt.npm.nica.emd

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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
    val (menu, setMenu) = useState(true)
    val (currentPage, setCurrentPage) = useState<PageConfig>()
    val (showStats, setShowStats) = useState(true) // show stats if true, else search and data
    val (EMDdata, setEMDdata) = useState<String>()
    val (SWdata, setSWdata) = useState<Array<SoftwareVersion>>()
    val (Storagedata, setStoragedata) = useState<Array<Storage>>()
    val (authenticated, setAuthenticated) = useState(false)
    val (username, setUsername) = useState<String>("")
    val (password, setPassword) = useState<String>("")

    useEffectOnce {
        scope.launch {
            val newConfig = getConfig()
            setConfig(newConfig)
            setCurrentPage(newConfig.pages.first())
        }
    }

    div("wrapper") {
        header {
            nav {
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
                                setCurrentPage(config?.pages?.first()) // Is it what we want to show as home?
                                setShowStats(true)
                            }
                            + (config?.title ?: "Event Metadata System")
                        }
                        dangerousSVG(SVGHeaderBubbles)
                    }
                }
                span("example-spacer") {}
                if (!authenticated) {
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
                                    +username
                                }
                            }
                            div("header_line") {}
                        }
                        div {
                            div("header_svg_pad svg_anim_exit") {
                                dangerousSVG(SVGLogout)
                                attrs {
                                    onClickFunction = {
                                        setAuthenticated(false)
                                        setUsername("")
                                        setPassword("")
                                        setCurrentPage(config?.pages?.first())
                                        setEMDdata("""{"events": []}""")
                                        setShowStats(true)
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
            if (currentPage == LOGIN_PAGE) {
                child(login) {
                    attrs.setValues = { username, password ->
                        setUsername(username)
                        setPassword(password)
                        setAuthenticated(true)
                        setCurrentPage(config?.pages?.first())
                    }
                    attrs.config = config
                }
            } else if (currentPage == DICTIONARY_PAGE) {
                child(dictionary) {// color: #e13a3a;
                    attrs.SWdata = SWdata
                    attrs.setSWdata = {
                        setSWdata(it)
                    }
                    attrs.Storagedata = Storagedata
                    attrs.setStoragedata = {
                        setStoragedata(it)
                    }
                    attrs.config = config
                    attrs.username = username
                    attrs.password = password
                }
            } else if (showStats) {
                child(statsPage) {
                    attrs.experiment = currentPage?.name?.split(" ")?.first() ?: "BM@N"  // TODO
                }
            } else {
                child(emdPage) {
                    attrs.pageConfig = currentPage!!
                    attrs.config = config
                    attrs.EMDdata = EMDdata
                    attrs.setEMDdata = { it: String? ->
                        setEMDdata(it)
                    }
                    attrs.authenticated = authenticated
                    attrs.username = username
                    attrs.password = password
                    attrs.redirectToAuth = { setCurrentPage(LOGIN_PAGE) }
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
