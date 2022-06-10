package ru.mipt.npm.nica.emd
import csstype.*
import react.Props
import react.createElement
import react.dom.div
import react.dom.svg
import react.fc
import react.*
import react.dom.*
import kotlinext.js.jso
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.coroutines.MainScope
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.dom.RDOMBuilder
import mui.material.TextField
import kotlinx.html.style
import mui.material.*
import mui.material.Size
import org.w3c.dom.HTMLInputElement
import react.ReactNode
import react.css.css
import react.useState
import mui.material.Button
import mui.material.ButtonVariant
import kotlinx.html.HTMLTag


external interface LoginPageProps : Props {
    var pageConfig: PageConfig
}

val login = fc<LoginPageProps> { props ->
    div("login"){
        div("login__page"){
            div("login_page__card"){
                div("login_page__card__left"){
                    div("login_page__card__left__login"){
                        +"Sign In"
                    }
                    div("login_page__card__left__text"){}
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
                    Button {
                        attrs {
                            +"Sign In"
                            variant = ButtonVariant.contained
                            size = Size.small
                        }
                    }
                    div("login_page__card__left__password"){
                        +"Forgot your password?"
                    }
                }
                div("login_page__card__right"){
                    div("login_page__card__right__icon"){
                        a(href = "https://bmn.jinr.ru/", target = "_blank") {
                            img(src = "favicon.png") {}
                        }
                    }
                    div("login_page__card__right__text cursor"){
                        div("text__head"){
                            +"Registration"
                        }
                        div("text__down"){
                            +"Need account or additional permissions?"
                        }
                        Button() {
                            attrs {
                                +"Create Account"
                                variant = ButtonVariant.contained
                                size = Size.small
                            }
                        }
                    }
                }
            }
            
        }
    }
}

                            /* */