import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import mui.material.*
import org.w3c.dom.HTMLInputElement
import react.Props
import react.ReactNode
import react.dom.*
import react.fc
import react.useState


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
    var EMDdata: String?
    var setEMDdata: (String?) -> Unit
}

// Component to render attribute selection and nested table
val emdPage = fc<EMDPageProps> { props ->
    // Parameters entered in form
    val (params, setParams) = useState<Map<String, String>>()

    // Event metadata from API
    // val (EMDData, setEMDData) = useState<String>()

    div("yellow") {

        h3 {
            +"EMDPage component"
        }
        h4 {
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
                }

            }
        }

        div {
            textInput("period_number", "Period Number")
        }

        div {
            textInput("run_number", "Run Number")
        }

        // TODO List selection
        div {
            textInput("software_version", "Software Version")
        }

        props.pageConfig.parameters.forEach { param ->
            div {
//                +"${param.web_name} - ${param.name}:"
//                br { }
                textInput(param.name, param.web_name)
            }
        }

        // TODO [dflt = 1000]
        div {
            textInput("limit", "Limit:")
        }

        div {
            textInput("offset", "Offset:")
        }

        Button {

            attrs {
                +"Filter"
                variant = ButtonVariant.contained
                onClick = {

                    // form API request
                    val paramsForURL = if (params != null) {
                        "?" + params.map { "${it.key}=${it.value}" }.filter { it.isNotBlank() }.joinToString("&")
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
                // TODO onClick
                onClick = {
                    setParams(emptyMap<String, String>())
                    props.setEMDdata(null)
                }
            }
        }
    }

    div("red") {
        // Table component with props taking API data state
        child(EMDTable) {
            attrs.content = props.EMDdata
            attrs.pageConfig = props.pageConfig
        }

    }

}