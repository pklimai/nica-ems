package ru.mipt.npm.nica.ems

import io.ktor.server.application.*
import io.ktor.util.*

class ParameterBundle(
    val period_number: Parameter?,
    val run_number: Parameter?,
    val software_version: Parameter?,
    val beam_particle: Parameter?,
    val target_particle: Parameter?,
    val energy: Parameter?,
    val limit: Parameter?,
    val offset: Parameter?,
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
                software_version = Parameter.fromParameterConfig(softwareConfig, call.parameters[softwareConfig.name]),

                // Parameters for pre-selection
                beam_particle =
                    Parameter.fromParameterConfig(beamParticleConfig, call.parameters[beamParticleConfig.name]),
                target_particle =
                    Parameter.fromParameterConfig(targetParticleConfig, call.parameters[targetParticleConfig.name]),
                energy = Parameter.fromParameterConfig(energyConfig, call.parameters[energyConfig.name]),
                limit = Parameter.fromParameterConfig(limitConfig, call.parameters[limitConfig.name]),
                offset = Parameter.fromParameterConfig(offsetConfig, call.parameters[offsetConfig.name]),
                parametersSupplied = parametersSupplied
            )
        }
    }

    fun hasInvalidParameters(call: ApplicationCall, page: PageConfig): Boolean {
        // Note that parameter can be invalid either if its value is invalid (e.g. incorrect range),
        // or if parameter name is just unknown

        if (period_number?.validParameter == false ||
            run_number?.validParameter == false ||
            software_version?.validParameter == false ||
            beam_particle?.validParameter == false ||
            target_particle?.validParameter == false ||
            energy?.validParameter == false ||
            limit?.validParameter == false ||
            offset?.validParameter == false
        ) return true

        parametersSupplied.forEach { (_ /* name */, value) ->
            if (!value.validParameter) return true
        }

        val allowedPageParameters =
            page.parameters.map { it.name } +
                    listOf(
                        periodConfig.name,
                        runConfig.name,
                        softwareConfig.name,
                        beamParticleConfig.name,
                        targetParticleConfig.name,
                        energyConfig.name,
                        limitConfig.name,
                        offsetConfig.name
                    )

        for (param in call.parameters.toMap().keys) {
            if (param !in allowedPageParameters) {
                return true
            }
        }
        return false
    }

}
