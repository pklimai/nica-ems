package ru.mipt.npm.nica.emd
import kotlinx.html.js.onClickFunction
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Checkbox
import mui.material.Size
import react.Props
import react.dom.*
import react.fc
import react.useState


external interface LoginPageProps : Props {
    var pageConfig: PageConfig
}

val login = fc<LoginPageProps> { props ->
    val (reset, setReset) = useState(false);
    val (add, setAdd) = useState(false);
    val (checked, setChecked) = useState(true);
    div("login"){
        div("login__page"){
            div("login_page__card"){
                div("login_page__card__left"){
                    if(add){
                        div("login_page__card__left__login"){
                            +"User Registration"
                        }
                    } else{
                        div("login_page__card__left__login"){
                            +"Sign In"
                        }
                    }
                    div("login_page__card__left__text"){}
                    if(!add){
                        div("wrap-form1 validate-input") {
                            input() {
                                attrs {
                                    placeholder = "Username"
                                }
                            } //<input type="text" #username  placeholder="Username" required>
                            span("focus-form1") {}
                            span("symbol-form1") {
                                div() {
                                    i("bx bxs-user-account"){}
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
                                    i("bx bxs-lock-alt"){}
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
                            attrs.onClickFunction = {
                                setReset(!reset)
                            }
                        }
                        if(reset){
                            div("reset_block"){
                                div("wrap-form1 validate-input") {
                                    input() {
                                        attrs {
                                            placeholder = "Login / Email"
                                        }
                                    } //<input type="text" #reset  placeholder="Login / Email" required>
                                    span("focus-form1") {}
                                    span("symbol-form1") {
                                        div() {
                                            i("bx bx-fingerprint"){}
                                        }
                                    }
                                }
                                div(){
                                    Button {
                                        attrs {
                                            +"Reset"
                                            variant = ButtonVariant.contained
                                            size = Size.small
                                        }
                                    }
                                    attrs.onClickFunction = {
                                        setReset(false)
                                    }
                                }

                            }
                        }
                    } else {
                        div("wrap-form1 validate-input") {
                            input() {
                                attrs {
                                    placeholder = "First name"
                                }
                            }
                            span("focus-form1") {}
                            span("symbol-form1") {
                                div() {
                                    i("bx bxs-user-account"){}
                                }
                            }
                        }
                        div("wrap-form1 validate-input") {
                            input() {
                                attrs {
                                    placeholder = "Last name"
                                }
                            }
                            span("focus-form1") {}
                            span("symbol-form1") {
                                div() {
                                    i("bx bxs-user-account"){}
                                }
                            }
                        }
                        div("wrap-form1 validate-input") {
                            input() {
                                attrs {
                                    placeholder = "Login"
                                }
                            }
                            span("focus-form1") {}
                            span("symbol-form1") {
                                div() {
                                    i("bx bx-id-card"){}
                                }
                            }
                        }
                        div("wrap-form1 validate-input") {
                            input() {
                                attrs {
                                    placeholder = "Email"
                                }
                            }
                            span("focus-form1") {}
                            span("symbol-form1") {
                                div() {                                    
                                    i("bx bx-envelope"){}
                                }
                            }
                        }
                        div("checkbox"){
                            Checkbox {}
                            div{
                                +"Write permission"
                            }
                        }
                        Button {
                            attrs {
                                +"Create Account"
                                variant = ButtonVariant.contained
                                size = Size.small
                            }
                        }
                    }
                }
                div("login_page__card__right"){
                    div("login_page__card__right__icon"){
                        a(href = "https://bmn.jinr.ru/", target = "_blank") {
                            img(src = "img/favicon.png") {}
                        }
                    }
                    div("login_page__card__right__text cursor"){
                        div("text__head"){
                            +"Registration"
                        }
                        div("text__down"){
                            +"Need account or additional permissions?"
                        }
                        if(add){
                            div("div__width"){
                                Button() {
                                    attrs {
                                        +"Back to Sign In"
                                        variant = ButtonVariant.contained
                                        size = Size.small
                                    }
                                }
                            }
                            attrs.onClickFunction = {
                                setAdd(false)
                            }
                        } else{
                            div("div__width"){
                                Button() {
                                    attrs {
                                        +"Create Account"
                                        variant = ButtonVariant.contained
                                        size = Size.small
                                    }
                                }
                            }
                            attrs.onClickFunction = {
                                setAdd(true)
                            }
                        }
                    }
                }
            }
        }
    }
}