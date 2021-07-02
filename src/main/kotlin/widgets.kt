package ru.mipt.npm.nica.emd

import kotlinx.html.*

fun FORM.parameterInput(parameterConfig: ParameterConfig, parameter: Parameter?) {
    // TODO input based on type - bool, str, etc.
    label { +parameterConfig.web_name }
    textInput {
        id = parameterConfig.name
        name = parameterConfig.name  // required for parameter to be sent in URL
        parameter?.let {
            value = parameter.stringValue
        }
    }
    br { }
}
