package ru.mipt.npm.nica.emd

import csstype.px
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.js.jso   // Note package change!
import mui.material.*
import org.w3c.dom.HTMLInputElement
import react.Props
import react.ReactNode
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.onChange
import react.fc
import react.useState


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
    var config: ConfigFile?
    var EMDdata: String?
    var setEMDdata: (String?) -> Unit
    var authenticated: Boolean
    var username: String
    var password: String
    var redirectToAuth: () -> Unit
}

// Component to render attribute selection and nested table
val emdPage = fc<EMDPageProps> { props ->
    // Parameters entered in form
    val (params, setParams) = useState<Map<String, String>>()

    // Event metadata from API
    // val (EMDData, setEMDData) = useState<String>()
    div("new_table_page") {
        div("new_table_page__card") {
            div("mat-table-title") {
                +props.pageConfig.name
            }
            div("form") {
                // Without receiver, it works but tags go to wrong level in document
                fun RDOMBuilder<DIV>.textInput(paramName: String, labelString: String = "") {
                    TextField {
                        attrs {
                            name = paramName
                            id = paramName
                            value = params?.get(paramName) ?: ""    /// ? to test
                            variant = FormControlVariant.outlined
                            label = ReactNode(labelString)
                            onChange = { it ->
                                val newValue = (it.target as HTMLInputElement).value
                                // console.log(newValue)
                                val copyParams = HashMap(params ?: emptyMap())
                                copyParams[paramName] = newValue
                                setParams(copyParams)
                            }
                            size = Size.small
                        }
                    }
                }

                fun RDOMBuilder<DIV>.boolInput(paramName: String, labelString: String = "") {
                    FormControl{
                        attrs {
                            fullWidth = true
                        }
                        InputLabel {
                            attrs {
                                id = paramName
                                sx = jso {
                                    // Fixes default location - is there a better way?
                                    marginTop = (-5).px
                                }
                            }
                            +labelString
                        }
                        Select {
                            attrs {
                                size = Size.small
                                label = ReactNode(labelString)
                                // labelId = paramName
                                value =  (params?.get(paramName) ?: "").unsafeCast<Nothing?>()
                                onChange = { it: dynamic, _ ->
                                    val newValue = it.target.value    // Note: it.asDynamic() won't work
                                    // console.log("onChange called in Select with value $newValue")
                                    val copyParams = HashMap(params ?: emptyMap())
                                    copyParams[paramName] = newValue
                                    setParams(copyParams)
                                }
                            }
                            MenuItem {
                                attrs {
                                    value = ""
                                }
                                +"No selection"
                            }
                            MenuItem {
                                attrs {
                                    value = "true"
                                }
                                +"true"
                            }
                            MenuItem {
                                attrs {
                                    value = "false"
                                }
                                +"false"
                            }
                        }
                    }
                }

                // TODO List selection
                div("input-div") {
                    textInput("software_version", "Software Version")
                }

                div("input-div") {
                    textInput("period_number", "Period Number")
                }

                div("input-div") {
                    textInput("run_number", "Run Number")
                }

                if (props.config?.condition_db != null) {
                    div("divider-div") {
                    }

                    div("input-div") {
                        textInput("beam_particle", "Beam Particle")
                    }

                    div("input-div") {
                        textInput("target_particle", "Target Particle")
                    }

                    div("input-div") {
                        textInput("energy", "Energy, GeV")
                    }
                }

                div("divider-div2") {
                }

                props.pageConfig.parameters.forEach { param ->
                    div("input-div top-input") {
                        when (param.type) {
                            "bool" -> boolInput(param.name, param.web_name)
                            else -> textInput(param.name, param.web_name)
                        }
                    }
                }

                div("divider-div2") {
                }

                div("input-div") {
                    textInput("limit", "Limit [dflt=${props.pageConfig.default_limit_web}]")
                }

                div("input-div") {
                    textInput("offset", "Offset")
                }

                div("button-container") {
                    Button {
                        attrs {
                            +"Filter"
                            variant = ButtonVariant.contained
                            size = Size.small
                            onClick = {
                                // form API request
                                if (props.config?.authRequired() == true && !props.authenticated) {
                                    // redirect to auth window
                                    scope.launch {
                                        props.redirectToAuth()
                                    }
                                } else {
                                    val paramsWithLimit = HashMap(params ?: emptyMap())
                                    if (paramsWithLimit["limit"].isNullOrEmpty())
                                        paramsWithLimit["limit"] = props.pageConfig.default_limit_web.toString()
                                    val paramsForURL = if (paramsWithLimit.isNotEmpty()) {
                                        "?" + paramsWithLimit.map { "${it.key}=${it.value}" }
                                            .filter { it.isNotBlank() }
                                            .joinToString("&")
                                    } else {
                                        ""
                                    }

                                    console.log(paramsWithLimit.toString())
                                    console.log(paramsForURL)
                                    scope.launch {
                                        val emd = getEMD(
                                            props.pageConfig.api_url + "/emd" + paramsForURL,
                                            props.config,
                                            props.username,
                                            props.password
                                        )
                                        console.log(emd)
                                        // update state with API data
                                        props.setEMDdata(emd)
                                    }
                                }
                            }
                        }
                    }

                    Button {
                        attrs {
                            +"Reset"
                            variant = ButtonVariant.contained
                            size = Size.small
                            onClick = {
                                setParams(emptyMap<String, String>())
                                props.setEMDdata(null)
                            }
                        }
                    }
                }
            }
        }

        div("new_table_page__table") {
            // Table component with props taking API data state
            child(EMDTable) {
                attrs.content = props.EMDdata
                attrs.pageConfig = props.pageConfig
            }
        }
    }
}