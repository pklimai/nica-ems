import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.dom.*
import react.fc
import react.useState

// Component to render attribute selection and nested table


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
}

val emdPage = fc<EMDPageProps> { props ->
    // Parameters entered in form
    var (params, setParams) = useState<Map<String, String>>()

    // Event metadata from API
    var (EMDData, setEMDData) = useState<String>()

    div("yellow") {

        h3 {
            +"EMDPage component"
        }
        h4 {
            +props.pageConfig.name
        }

        // Without receiver, it works but tags go to wrong level in document
        fun RDOMBuilder<DIV>.textInput(paramName: String) {
            input(name = paramName) {
                attrs {
                    id = paramName
                    onChangeFunction = { it ->
                        val newValue = (it.target as HTMLInputElement).value
                        // console.log(newValue)
                        val copyParams = HashMap(params ?: emptyMap())
                        copyParams[paramName] = newValue
                        setParams(copyParams)
                    }
                }

            }
        }

        props.pageConfig.parameters.forEach { param ->
            div {
                +"${param.web_name} - ${param.name}:"
                br { }
                textInput(param.name)
            }
        }

        // TODO enter limit [dflt = 1000], offset
        div {
            + "Limit:"
            br { }
            textInput("limit")
        }

        div {
            + "Offset:"
            br { }
            textInput("offset")
        }

        button {
            + "Filter"
            attrs {
                onClickFunction = {
                    // form API request

                    val paramsForURL = if (params != null) {
                        "?" + params.map{"${it.key}=${it.value}"}.filter{it.isNotBlank()}.joinToString("&")
                    } else {
                        ""
                    }

                    console.log(params.toString())
                    console.log(paramsForURL)

                    scope.launch {
                        val emd = getEMD(props.pageConfig.api_url + "/emd" + paramsForURL)
                        console.log(emd)
                        // update state with API data
                        setEMDData(emd)
                    }

                }
            }
        }

        button {
            + "Reset"
        }

        child(EMDTable) {
            attrs.content = EMDData ?: "Not set"
        }

        // Table component with props taking API data state

    }

}