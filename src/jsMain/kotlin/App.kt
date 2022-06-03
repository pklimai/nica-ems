package ru.mipt.npm.nica.emd

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.HTMLTag
import kotlinx.html.js.onClickFunction
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Size
import react.*
import react.dom.*

val DICTIONARY_PAGE = PageConfig("__dictionary", "", "", "", emptyList())

val scope = MainScope()

val app = fc<Props> { props ->

    val (config, setConfig) = useState<ConfigFile>()
    val (menu, setMenu) = useState(true);

    val (currentPage, setCurrentPage) = useState<PageConfig>()
    // setCurrentPage(null) -- valid but causes too many re-renders here!

    val (EMDdata, setEMDdata) = useState<String>()
    val (disp, setDisp) = useState(true);
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
                if(disp){
                    div("menu_name2") {
                        div("events_icon2") {}
                        div("login_block") {
                            div("wrap-form1 validate-input") {
                                input() {
                                    attrs {
                                        placeholder = "Username"
                                    }
                                } //<input type="text" #username  placeholder="Username" required>
                                span("focus-form1") {}
                                span("symbol-form1") {
                                    div() {
                                        img(classes = "login-password-icon", src = "username.png") {  }
                                    }
                                }
                            }
                            div("wrap-form1 validate-input") {
                                input() {
                                    attrs {
                                        placeholder = "Password"
                                    }
                                } //<input type="text" #password type="password"  placeholder="Password" required>
                                span("focus-form1") {}
                                span("symbol-form1") {
                                    div() {
                                        img(classes = "login-password2-icon", src = "password.png") {  }
                                    }
                                }
                            }
                            div("but_login") {
                                Button {
                                    attrs {
                                        +"Sign In"
                                        variant = ButtonVariant.contained
                                        size = Size.small
                                        onClick = {
                                            setDisp(false)
                                        }
                                    }
                                }
                            }
                        }
                    } 
                } else{
                 div("menu_name3") {
                        div("events_icon3") {
                            div("header_svg_pad svg_anim_di"){
                                dangerousSVG(SVGDictionaryIcon)
                                attrs {
                                    onClickFunction = {
                                        setCurrentPage(DICTIONARY_PAGE)
                                    }
                                }

                                span("svg_anim_di_tooltip"){
                                    +"Dictionary"
                                }
                            }
                            div("header_line"){}
                            div("header_user_info"){
                                dangerousSVG(SVGUserPic)
                                div("header_svg_title"){
                                    +"Username"
                                }
                            }
                            div("header_line"){}
                        }
                        div() {
                            div("header_svg_pad svg_anim_exit"){
                                dangerousSVG(SVGLogout)
                                attrs {
                                    onClickFunction = {
                                        setDisp(true)
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
                            attrs.onClickFunction = {
                                setCurrentPage(item)
                                // Clear data for table
                                setEMDdata(null)
                            }
                            div("top__search") {
                                +item.name
                            }
                            child(searchComponent) {
                                attrs.highlighted = (currentPage == item)
                            }
                        }
                    }
                }
            }
            if (currentPage == null) {
                child(homePage)
            } else if (currentPage == DICTIONARY_PAGE) {
                child(dictionary)
                    // _attention_ при клике по иконке словарика надо чтобы перекидывала в другой компонент,
                    // но необходимо проверять по 2 условиям по роли и авторизации
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
        footer {
            div {
                a(href = "/") {
                    img(src = "home.png", classes = "home-icon") { }
                }
            }
            span("example-spacer") {}
            div {
                a(href = "https://bmn.jinr.ru/", target = "_blank") {
                    img(src = "favicon.png") {}
                }
            }
        }
    }
}