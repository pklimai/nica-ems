import csstype.HtmlAttributes
import kotlinx.coroutines.launch
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

        props.pageConfig.parameters.forEach { param ->
            p {
                + "${param.web_name} - ${param.name}"
                br { }
                input(name = param.name) {
                    attrs {
                        id = param.name
                        onChangeFunction = { it ->
                            val newValue = ( it.target as HTMLInputElement).value
                            console.log(newValue)
                            val copyParams = HashMap(params ?: emptyMap())
                            copyParams[param.name] = newValue
                            setParams(copyParams)
                            // print params.toString() here shows outdated result!
                        }
                    }

                }
            }
        }

        button {
            + "Click me"
            attrs {
                onClickFunction = {
                    // form API request
                    // update state with API data ?
                    console.log(params.toString())

                    scope.launch {
                        val emd = getEMD(props.pageConfig.api_url)
                        console.log(emd)
                        setEMDData(emd)
                    }

                }
            }
        }

        child(EMDTable) {
            attrs.content = EMDData ?: "Not set"
        }

        // Table component with props taking API data state

    }

}