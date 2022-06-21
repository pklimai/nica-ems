package ru.mipt.npm.nica.emd

import csstype.*
import react.Props
import react.createElement
import react.dom.div
import react.dom.svg
import react.fc
import kotlinext.js.jso
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import kotlinx.html.id
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

external interface DictionaryPageProps : Props {
    var pageConfig: PageConfig
    var EMDdata: String?
    var setEMDdata: (String?) -> Unit
}

val dictionary = fc<DictionaryPageProps> { props ->
    val (params, setParams) = useState<Map<String, String>>()
    scope.launch {
        val getSoft = getEMD(props.pageConfig.api_url + "/emd")
        val getStorage = getEMD(props.pageConfig.api_url + "/emd")
    }
    div("dictionary") {
        div("dictionary__back"){
            div("flex"){
                fun RDOMBuilder<DIV>.textInput(paramName: String, labelString: String = "") {
                    TextField {
                        attrs {
                            name = paramName
                            id = paramName
                            value = params?.get(paramName) ?: ""    /// ? to test
                            variant = FormControlVariant.outlined
                            label = ReactNode(labelString)
                        }
                    }
                }
                div("dictionary__back__card"){
                    dangerousSVG(SVGCloudForDict)
                    div("dictionary__back__input") {
                        textInput("storage_name", "Storage Name")
                    }
                    Button {
                        attrs {
                            +"Add"
                            variant = ButtonVariant.contained
                            size = Size.small
                        }
                    }
                }
                div("dictionary__back__card"){
                    dangerousSVG(SVGSWforDict)
                    div("dictionary__back__input") {
                        textInput("software_version", "Software Version")
                    }
                    Button {
                        attrs {
                            +"Add"
                            variant = ButtonVariant.contained
                            size = Size.small
                        }
                    }
                }
           }
           div("dictionary__tables"){
                child(SSTable){
                    attrs.content = props.EMDdata
                    attrs.pageConfig = props.pageConfig
                    attrs.table = "Storage"
                }
                child(SSTable){
                    attrs.content = props.EMDdata
                    attrs.pageConfig = props.pageConfig
                    attrs.table = "Software"
                }
            }
       } 
    }
}