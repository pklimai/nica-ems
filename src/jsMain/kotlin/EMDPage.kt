import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*


external interface EMDPageProps : Props {
    var pageConfig: PageConfig
}

interface EMDPageState : State {
    var params: Map<String, String>?
    var EMDData: String?
}


// Component to render attribute selection and nested table
class EMDPage(prps: EMDPageProps) : RComponent<EMDPageProps, EMDPageState>(prps) {

    override fun EMDPageState.init(prps: EMDPageProps) {
        params = null;
        EMDData = null;
    }

    override fun componentWillReceiveProps(nextProps: EMDPageProps) {
        super.componentWillReceiveProps(nextProps)
        if (nextProps != props) {
            this@EMDPage.setState {
                params = null
                EMDData = null
            }
        }
    }
    // fc<EMDPageProps> { props ->

    // Parameters entered in form
    // val (params, setParams) = useState<Map<String, String>>()

    // Event metadata from API
    // val (EMDData, setEMDData) = useState<String>()

    override fun RBuilder.render() {
        return div("yellow") {

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
                            val copyParams = HashMap(this@EMDPage.state.params ?: emptyMap())
                            copyParams[paramName] = newValue
                            this@EMDPage.setState {
                                params = copyParams
                            }
                            //setParams(copyParams)
                        }
                    }

                }
            }

            div {
                +"Period Number"
                br { }
                textInput("period_number")
            }

            div {
                +"Run Number"
                br { }
                textInput("run_number")
            }

            // TODO List selection
            div {
                +"Software Version"
                br { }
                textInput("software_version")
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
                +"Limit:"
                br { }
                textInput("limit")
            }

            div {
                +"Offset:"
                br { }
                textInput("offset")
            }

            button {
                +"Filter"
                attrs {
                    onClickFunction = {
                        // form API request

                        val paramsForURL = if (this@EMDPage.state.params != null) {
                            "?" + this@EMDPage.state.params!!.map { "${it.key}=${it.value}" }
                                .filter { it.isNotBlank() }.joinToString("&")
                        } else {
                            ""
                        }

                        console.log(this@EMDPage.state.params.toString())
                        console.log(paramsForURL)

                        scope.launch {
                            val emd = getEMD(props.pageConfig.api_url + "/emd" + paramsForURL)
                            console.log(emd)
                            // update state with API data
                            this@EMDPage.setState {
                                EMDData = emd
                            }
                            // setEMDData(emd)
                        }

                    }
                }
            }

            button {
                +"Reset"
                // TODO onClick
            }

            // Table component with props taking API data state
            child(EMDTable) {
                attrs.content = this@EMDPage.state.EMDData
                attrs.pageConfig = props.pageConfig

                console.log("***************")
                console.log(this@EMDPage.state.EMDData)
                console.log(props.pageConfig)
            }

        }
    }

}