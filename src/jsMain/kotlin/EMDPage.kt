import csstype.Height
import csstype.NamedColor
import csstype.pct
import csstype.px
import kotlinext.js.jso
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.style
import mui.material.*
import org.w3c.dom.HTMLInputElement
import react.Props
import react.ReactNode
import react.css.css
import react.dom.*
import react.fc
import react.useState


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
    var EMDdata: String?
    var setEMDdata: (String?) -> Unit
    var condition_db: DBConnectionConfig?
}

// Component to render attribute selection and nested table
val emdPage = fc<EMDPageProps> { props ->
    // Parameters entered in form
    val (params, setParams) = useState<Map<String, String>>()

    // Event metadata from API
    // val (EMDData, setEMDData) = useState<String>()

    div("div-filtering-fields") {
        Card {
            attrs {
                style = jso {
                    paddingLeft = 25.px
                    paddingRight = 25.px
                }
            }

            h3 {
                +props.pageConfig.name
            }

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

            div("input-div") {
                textInput("period_number", "Period Number")
            }

            div("input-div") {
                textInput("run_number", "Run Number")
            }

            // TODO List selection
            div("input-div") {
                textInput("software_version", "Software Version")
            }

            if (props.condition_db != null) {
                div("divider-div") {
                    hr { }
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

            div("divider-div") {
                hr { }
            }

            props.pageConfig.parameters.forEach { param ->
                div("input-div") {
                    textInput(param.name, param.web_name)
                }
            }

            div("divider-div") {
                hr { }
            }

            // TODO set [dflt = 1000]
            div("input-div") {
                textInput("limit", "Limit:")
            }

            div("input-div") {
                textInput("offset", "Offset:")
            }

            div("button-container") {

                Button {

                    attrs {
                        +"Filter"
                        variant = ButtonVariant.contained
                        size = Size.small
                        onClick = {

                            // form API request
                            val paramsForURL = if (params != null) {
                                "?" + params.map { "${it.key}=${it.value}" }.filter { it.isNotBlank() }
                                    .joinToString("&")
                            } else {
                                ""
                            }

                            console.log(params.toString())
                            console.log(paramsForURL)

                            scope.launch {
                                val emd = getEMD(props.pageConfig.api_url + "/emd" + paramsForURL)
                                console.log(emd)
                                // update state with API data
                                props.setEMDdata(emd)
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

    div("div-emd-table") {
        // Table component with props taking API data state

        child(EMDTable) {
            attrs.content = props.EMDdata
            attrs.pageConfig = props.pageConfig
        }

    }

}