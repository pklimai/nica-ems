package ru.mipt.npm.nica.emd

import kotlinx.html.*
import java.sql.Connection

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

fun BODY.inputParametersForm(parameterBundle: ParameterBundle, page: PageConfig, softwareMap: SoftwareMap, connCondition: Connection?) {
    form {
        with (parameterBundle) {
            parameterInput(periodConfig, period_number)
            parameterInput(runConfig, run_number)

            label { +"Software Version" }
            select {
                id = "software_version"
                name = "software_version"
                option {
                    value = ""  // sent as a value in URL
                    if (software_version.isNullOrEmpty()) {
                        selected = true
                    }
                    +"No selection"   // Displayed
                }
                softwareMap.str_to_id.keys.forEach {
                    option {
                        value = it
                        if (it == software_version) {
                            selected = true
                        }
                        +it   // NB: Order matters!
                    }
                }
            }

            connCondition?.let {
                hr {}
                parameterInput(beamParticleConfig, beam_particle)
                parameterInput(targetParticleConfig, target_particle)
                parameterInput(energyConfig, energy)
                hr {}
            }

            page.parameters.forEach { parameter ->
                parameterInput(parameter, parametersSupplied[parameter.name])
            }

            br { }
            submitInput {
                value = "Submit"
                formMethod = InputFormMethod.get
            }
        }
    }
}