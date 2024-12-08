package ru.mipt.npm.nica.ems

import react.dom.div
import kotlinx.coroutines.launch
import kotlinx.html.DIV
import react.dom.RDOMBuilder
import mui.material.TextField
import mui.material.*
import mui.material.Size
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.onChange
import react.dom.p
import ru.mipt.npm.nica.ems.utility.EMSConflictException
import ru.mipt.npm.nica.ems.utility.EMSServerError
import ru.mipt.npm.nica.ems.utility.EMSUnauthException

external interface DictionaryPageProps : Props {
    var SWdata: Array<SoftwareVersion>?
    var setSWdata: (Array<SoftwareVersion>?) -> Unit
    var Storagedata: Array<Storage>?
    var setStoragedata: (Array<Storage>?) -> Unit
    var config: ConfigFile?
    var username: String
    var password: String
}

val dictionary = fc<DictionaryPageProps> { props ->
    val (storage, setStorage) = useState<String>()
    val (SWver, setSWver) = useState<String>()
    val (errorMessage, setErrorMessage) = useState<String?>(null)

    useEffectOnce {
        scope.launch {
            val swVer = getSoftwareVersions(props.config, props.username, props.password)
            if (swVer.result != null) {
                props.setSWdata(swVer.result)
            } else {
                setErrorMessage("Error obtaining software information\n ${swVer.status}\n ${swVer.message}")
            }
            val storages = getStorages(props.config, props.username, props.password)
            if (storages.result != null) {
                props.setStoragedata(storages.result)
            } else {
                setErrorMessage("Error obtaining storage information\n ${storages.status}\n ${storages.message}")
            }
        }
    }

    div("dictionary") {
        errorMessage?.let {
            it.split("\n").forEach {
                p("dictionary__p__error") {
                    +it
                }
            }
        }
        div("dictionary__top") {
            div("dictionary__top__card") {
                dangerousSVG(SVGCloudForDict)
                div("dictionary__back__input") {
                    TextField {
                        attrs {
                            id = "storage_name"
                            value = storage ?: ""
                            variant = FormControlVariant.outlined
                            label = ReactNode("Storage Name")
                            onChange = {
                                val newStorage = (it.target as HTMLInputElement).value
                                setStorage(newStorage)
                            }
                        }
                    }
                }
                Button {
                    attrs {
                        variant = ButtonVariant.contained
                        size = Size.small
                        onClick = {
                            scope.launch {
                                if (!storage.isNullOrEmpty()) {
                                    try {
                                        postStorage(storage, props.config, props.username, props.password)
                                        props.setStoragedata(getStorages(props.config, props.username, props.password).result)
                                    } catch (_: EMSUnauthException) {
                                        setErrorMessage("Error - unauthorized")
                                    } catch (_: EMSConflictException) {
                                        setErrorMessage("Error - record already exists")
                                    } catch (_: EMSServerError) {
                                        setErrorMessage("Server error")
                                    }
                                }
                            }
                            setStorage("")  // clear in the input
                            setErrorMessage(null)
                        }
                    }
                    +"Add"
                }
            }
            div("dictionary__top__card") {
                dangerousSVG(SVGSWforDict)
                div("dictionary__back__input") {
                    TextField {
                        attrs {
                            id = "software_version"
                            value = SWver ?: ""
                            variant = FormControlVariant.outlined
                            label = ReactNode("Software Version")
                            onChange = {
                                val newSW = (it.target as HTMLInputElement).value
                                setSWver(newSW)
                            }
                        }
                    }
                }
                Button {
                    attrs {
                        variant = ButtonVariant.contained
                        size = Size.small
                        onClick = {
                            scope.launch {
                                if (!SWver.isNullOrEmpty()) {
                                    try {
                                        postSoftwareVersion(SWver, props.config, props.username, props.password)
                                        props.setSWdata(
                                            getSoftwareVersions(
                                                props.config,
                                                props.username,
                                                props.password
                                            ).result
                                        )
                                    } catch (_: EMSUnauthException) {
                                        setErrorMessage("Error - unauthorized")
                                    } catch (_: EMSConflictException) {
                                        setErrorMessage("Error - record already exists")
                                    } catch (_: EMSServerError) {
                                        setErrorMessage("Server error")
                                    }
                                }
                            }
                            setSWver("")  // clear in the input
                            setErrorMessage(null)
                        }
                    }
                    +"Add"
                }
            }
        }
        div("dictionary__bottom") {
            child(storageTable) {
                attrs.content = props.Storagedata
            }
            child(softwareTable) {
                attrs.content = props.SWdata
            }
        }
    }
}
