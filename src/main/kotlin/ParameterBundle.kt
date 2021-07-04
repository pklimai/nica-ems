package ru.mipt.npm.nica.emd

import io.ktor.application.*

class ParameterBundle(
    val period_number: Parameter?,
    val run_number: Parameter?,
    val software_version: String?,
    val beam_particle: Parameter?,
    val target_particle: Parameter?,
    val energy: Parameter?,
    val parametersSupplied: HashMap<String, Parameter>
) {
    companion object {
        fun buildFromCall(call: ApplicationCall, page: PageConfig): ParameterBundle {
            // Mapping of optional parameter name to its config and string value (possibly range, etc.)
            val parametersSupplied = HashMap<String, Parameter>()
            page.parameters.forEach { parameterConfig ->
                if (parameterConfig.name in call.parameters) {
                    val parameterValue = call.parameters[parameterConfig.name].toString()
                    if (parameterValue.isNotBlank()) {
                        parametersSupplied[parameterConfig.name] =
                            Parameter.fromParameterConfig(parameterConfig, parameterValue)!!
                    }
                }
            }
            return ParameterBundle(
                period_number = Parameter.fromParameterConfig(periodConfig, call.parameters[periodConfig.name]),
                run_number = Parameter.fromParameterConfig(runConfig, call.parameters[runConfig.name]),
                software_version =
                    if (call.parameters["software_version"].isNullOrEmpty()) null
                    else call.parameters["software_version"],
                // Parameters for pre-selection
                beam_particle =
                    Parameter.fromParameterConfig(beamParticleConfig, call.parameters[beamParticleConfig.name]),
                target_particle =
                    Parameter.fromParameterConfig(targetParticleConfig, call.parameters[targetParticleConfig.name]),
                energy = Parameter.fromParameterConfig(energyConfig, call.parameters[energyConfig.name]),
                parametersSupplied = parametersSupplied
            )
        }
    }
}
