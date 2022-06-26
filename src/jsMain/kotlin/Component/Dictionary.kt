package ru.mipt.npm.nica.emd

import react.dom.div
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import react.dom.RDOMBuilder
import mui.material.TextField
import mui.material.*
import mui.material.Size
import react.*

external interface DictionaryPageProps : Props {
    var SWdata: Array<SoftwareVersion>?
    var setSWdata: (Array<SoftwareVersion>?) -> Unit
    var Storagedata: Array<Storage>?
    var setStoragedata: (Array<Storage>?) -> Unit
}

val dictionary = fc<DictionaryPageProps> { props ->
    val (params, setParams) = useState<Map<String, String>>()

    useEffectOnce {
        scope.launch {
            props.setSWdata(getSoftwareVersions() as Array<SoftwareVersion>?)
            props.setStoragedata(getStorages() as Array<Storage>?)
        }
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
                child(StorageTable){
                    attrs.content = props.Storagedata
                }
//                child(SoftwareTable){
//                    attrs.content = props.SWdata
//                }
            }
       } 
    }
}